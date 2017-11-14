package controllers

import javax.inject.{Inject, Named}

import actors.RewardActor._
import akka.actor.ActorRef
import akka.pattern.ask
import models._
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.SendGridService
import utilities.ConfigLoader

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

case class Products(upcCode: String,
                    quantity: Int,
                    description: Option[String],
                    productAmount: Double)

case class Data(store: String,
                date: String,
                amount: Double,
                qualifyingAmount: Double,
                products: List[Products])

case class ReceiptCallback(status: Option[String],
                           error: Option[String],
                           submissionDate: Option[String],
                           UUID: String,
                           rawData: Option[String],
                           data: Data)

class Callbacks @Inject()(controllerComponent: ControllerComponents,
                          receiptRepository: ReceiptRepository,
                          userProfileRepository: UserProfileRepository,
                          config: ConfigLoader,
                          wsClient: WSClient,
                          sendGridService: SendGridService,
                          @Named("reward-codes-actor") rewardActor: ActorRef) extends AbstractController(controllerComponent) {
  implicit val productFormat = Json.format[Products]
  implicit val dataFormat = Json.format[Data]
  implicit val callbackResponseFormat = Json.format[ReceiptCallback]

  implicit val serializedProductListFormat = new Writes[Products] {
    def writes(product: Products): JsValue =
      Json.obj(
        "upcCode" -> product.upcCode,
        "quantity" -> product.quantity,
        "description" -> product.description,
        "productAmount" -> product.productAmount)
  }

  def receive: Action[JsValue] = Action.async(controllerComponent.parsers.json) {
    implicit request =>
      val response = request.body.validate[Map[String, ReceiptCallback]]
      response.fold(
        _ =>
          request.body.validate[Map[String, Map[String, String]]].fold(
            fail => {
              Logger.error(s"Malformed data submitted: $fail")
              Future.successful(BadRequest(Json.obj("message" -> "Malformed data submitted")) as "application/json")
            },
            error => {
              Logger.error(s"Got an internal server error: $error")
              Future.successful(InternalServerError(Json.obj("message" -> "Got an error from ocr engine")) as "application/json")
            }),
        data =>
          data.headOption map {
            case (_, receiptCallback) =>
              receiptRepository.fetchByReceiptId(receiptCallback.UUID).flatMap {
                case Some(receipt) =>
                  userProfileRepository.findByEmail(receipt.email).flatMap {
                    case Some(profile) =>
                      rewardUser(receiptCallback, profile, receipt.pointSent)
                    case None          =>
                      Logger.error(s"User profile not found for email: ${receipt.email}")
                      Future.successful(NotFound(Json.obj("message" -> "User profile not found")) as "application/json")
                  }
                case None          =>
                  Logger.error(s"Receipt with the given UUID doesn't exist: ${receiptCallback.UUID}")
                  Future.successful(NotFound(Json.obj("message" -> "Receipt with the given UUID doesn't exist")) as "application/json")
              }
          } getOrElse {
            Logger.error(s"Malformed data submitted: $data")
            Future.successful(BadRequest(Json.obj("message" -> "Malformed data submitted")) as "application/json")
          })
  }

  private def rewardUser(receiptCallback: ReceiptCallback, profile: Profile, pointSent: Boolean): Future[Result] = {
    val serializedProductList = Json.toJson(receiptCallback.data.products).toString()
    (receiptCallback.status.getOrElse(""), pointSent) match {
      case ("ambiguous", _)     =>
        Logger.error(s"Ambiguous receipt could not processed for user profile id ${profile.id} and snap3 receipt id ${receiptCallback.UUID}")
        receiptRepository.updateRejectedEmail(receiptCallback, serializedProductList)
        Future.successful(NotAcceptable("Ambiguous receipt could not processed"))
      case ("rejected", false)  =>
        Logger.info(s"Rejected receipt could not be processed for user profile id ${profile.id} and snap3 receipt id ${receiptCallback.UUID}")
        receiptRepository.updateRejectedEmail(receiptCallback, serializedProductList)
        handleRejectedEmail(profile, "rejected", receiptCallback.UUID)
      case ("invalid", false)   =>
        Logger.info(s"Invalid receipt could not be processed for user profile id ${profile.id} and snap3 receipt id ${receiptCallback.UUID}")
        receiptRepository.updateRejectedEmail(receiptCallback, serializedProductList)
        handleRejectedEmail(profile, "invalid", receiptCallback.UUID)
      case ("duplicate", false) =>
        Logger.info(s"Duplicate receipt could not be processed for user profile id ${profile.id} and snap3 receipt id ${receiptCallback.UUID}")
        receiptRepository.updateRejectedEmail(receiptCallback, serializedProductList)
        handleRejectedEmail(profile, "duplicate", receiptCallback.UUID)
      case ("approved", false)  =>
        processReceipt(receiptCallback, profile, serializedProductList)
      case (status, point)      =>
        Logger.error(s"No valid data found for receipt status $status and redeem point $point for user ${profile.email}" +
          s" and snap3 receipt id ${receiptCallback.UUID}")
        Future.successful(NotAcceptable("No valid data found from the receipt"))
    }
  }

  private def processReceipt(receiptCallback: ReceiptCallback, profile: Profile, serializedProductList: String): Future[Result] = {
    val futureCodesSize = (rewardActor ? GetSizeOfUnusedCodes) (30.seconds).mapTo[Int]
    futureCodesSize flatMap { size =>
      if (size == 0) {
        Logger.info(s"Callback Controller: No more reward codes available for the promotion for user profile id ${profile.id} " +
          s"and ocr receipt id ${receiptCallback.UUID}")
        Future.successful(InternalServerError("No more reward codes available for the promotion"))
      } else {
        receiptRepository.shouldSubmit(profile.id).flatMap {
          case true  =>
            receiptRepository.update(receiptCallback, serializedProductList) flatMap {
              case Some(receipt) =>
                val getFutureCode = (rewardActor ? GetUnusedCode(profile.id, Some(receipt.id))) (30.seconds).mapTo[String]

                getFutureCode flatMap { code =>
                  Logger.info(s"Reward code $code for user profile id ${profile.id} and receipt id ${receipt.id}")
                  handleApprovedEmail(profile, receipt.id, code)
                }
              case None          =>
                Logger.error(s"Could not able to update receipt data in receipt table for user profile id ${profile.id} and receipt id ${receiptCallback.UUID}")
                Future.successful(InternalServerError("Receipt should be updated"))
            }
          case false =>
            Logger.info(s"User has already qualified for the reward for user profile id ${profile.id} and snap3 receipt id ${receiptCallback.UUID}")
            Future.successful(Ok("already qualified"))
        }
      }
    }
  }

  private def handleRejectedEmail(profile: Profile, status: String, snap3ReceiptId: String): Future[Result] =
    sendGridService.sendEmailForRejection(profile.email, profile.firstName).fold {
      Logger.error(s"Internal server error while sending email for email address ${profile.email}, status $status, " +
        s"user profile id ${profile.id} and snap3 receipt id $snap3ReceiptId")
      Future.successful(InternalServerError("Email not sent"))
    } {
      _ =>
        Logger.info(s"Rejection email successfully sent for email ${profile.email}, status $status, " +
          s"user profile id ${profile.id} and snap3 receipt id $snap3ReceiptId")
        Future.successful(Ok("Email sent"))
    }

  private def handleApprovedEmail(profile: Profile, receiptId: Long, rewardCode: String): Future[Result] = {
    sendGridService.sendEmailForApproval(profile.email, profile.firstName, rewardCode).fold {
      Logger.error(s"Internal server error while sending approval email for email address ${profile.email}, " +
        s"user profile id ${profile.id} and receipt id $receiptId")
      Future.successful(InternalServerError("Email not sent"))
    } {
      _ =>
        Logger.info(s"Approval email successfully sent to email ${profile.email} with reward code $rewardCode for user " +
          s"profile id ${profile.id} and receipt id $receiptId")
        Future.successful(Ok("Email sent"))
    }
  }

}
