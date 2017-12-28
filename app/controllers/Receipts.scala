package controllers

import java.io.File
import javax.inject.Inject

import models._
import org.h2.engine.Constants
import play.api.Logger
import play.api.i18n.{Lang, Messages, MessagesImpl}
import play.api.libs.json._
import play.api.libs.ws.ahc.{AhcWSResponse, StandaloneAhcWSResponse}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc._
import play.shaded.ahc.org.asynchttpclient.request.body.multipart.{FilePart, StringPart}
import play.shaded.ahc.org.asynchttpclient.{AsyncCompletionHandler, RequestBuilder, Request => AHCRequest}
import services.{EventSenderService, SendGridService}
import utilities.{ConfigLoader, Util}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

case class AccessRequest(access_token: String)

class Receipts @Inject()(controllerComponent: ControllerComponents,
                         p3UserInfoRepository: P3UsersInfoRepository,
                         eventSender: EventSenderService,
                         eventRepository: EventRepository,
                         receiptRepository: ReceiptRepository,
                         securedImplicit: SecuredImplicits,
                         config: ConfigLoader,
                         wsClient: WSClient,
                         sendGridService: SendGridService,
                         util: Util) extends AbstractController(controllerComponent) {
  implicit val jodaDateReads = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss'Z'")
  implicit val jodaDateWrites = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss'Z'")

  implicit val formatProfile = Json.format[Profile]
  implicit val formatAccessRequest = Json.format[AccessRequest]

  val lang: Lang = controllerComponent.langs.availables.head
  implicit val message: Messages = MessagesImpl(lang, messagesApi)

  def receiveReceipt: Action[AnyContent] = Action.async { implicit request =>
    val domainUrl = "http://" + request.host
    val userProfile = securedImplicit.userProfile

    val userInfo = securedImplicit.p3UserInfo

    receiptRepository.shouldSubmit(userProfile.id).flatMap {
      case true  =>
        try {
          val encodedResponse = request.body.asMultipartFormData.get.file("enc")
          val file = encodedResponse.map(_.ref.path)
          val contentType = request.body.asMultipartFormData.get.files.map(_.contentType).toString()
          getAccessToken.fold {
            Logger.error(s"Access token not found for receipt uploaded by email address ${userProfile.email}")
            Future.successful(InternalServerError(message("error.access.token")))
          } { accessToken =>
            val (hashedFileName, mimeType): (String, String) = fileGenerate(contentType)
            val ahcRequest = buildUploadRequest(userProfile, accessToken, domainUrl, userInfo.p3UserId, hashedFileName, mimeType, file.get.toFile)

            executeRequest(ahcRequest) map { wsResponse =>
              val responseStatus = ocrResponseHandler(wsResponse, userProfile, hashedFileName, userInfo.p3UserId, request.remoteAddress)(request)
              responseStatus match {
                case 200 =>
                  Logger.info(s"Receipt uploaded successfully for user ${userInfo.p3UserId} response ${wsResponse.body}")
                  Ok("s").addingToSession("receipt" -> "uploaded")
                case 401 => Logger.info(s"Duplicate receipt uploaded by user ${userInfo.p3UserId} response ${wsResponse.body}")
                  Ok("d")
                case _   => Logger.info(s"Got some error for receipt uploaded by user ${userInfo.p3UserId} response ${wsResponse.body}")
                  Ok("f")
              }
            }
          }
        } catch {
          case ex: Exception =>
            Logger.error(s"Got an exception while processing receipt for user ${userProfile.email}, ex: $ex")
            Future.successful(InternalServerError(message("common.error.message")))
        }
      case false =>
        Logger.info(s"User has already submitted receipt for today with email ${userProfile.email}")
        Future.successful(Ok("u")
          .addingToSession("receipt" -> "uploaded"))
    }
  }

  protected def executeRequest(request: AHCRequest): Future[WSResponse] = {
    import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient

    import scala.concurrent.Promise

    val responsePromise = Promise[WSResponse]()
    val httpClient: AsyncHttpClient = wsClient.underlying

    httpClient.executeRequest(request, new AsyncCompletionHandler[Unit] {
      override def onCompleted(response: play.shaded.ahc.org.asynchttpclient.Response) = {
        responsePromise.success(AhcWSResponse(StandaloneAhcWSResponse(response)))
      }

      override def onThrowable(ex: Throwable): Unit = {
        responsePromise.failure(ex)
      }
    })
    responsePromise.future
  }

  private def getAccessToken: Option[String] = {
    try {
      val url = "%s/auth?consumer_key=%s&consumer_secret=%s" format(config.load.ocrApiUrl, config.load.snap3ClientKey,
        config.load.snap3ClientSecret)
      val futureResponse = wsClient.url(url).get()
      val response = Await.result(futureResponse, 20.seconds)
      response.status match {
        case 200 =>
          val accessRequest = Json.parse(response.body).validate[AccessRequest]
          Some(accessRequest.get.access_token)
        case _   => None
      }
    } catch {
      case ex: Exception =>
        None
    }
  }

  private def ocrResponseHandler(wsResponse: WSResponse, userProfile: Profile, fileName: String, p3UserId: String,
                                 remoteIp: String)(implicit request: Request[AnyContent]): Int = {
    val time = util.getPacificTime
    wsResponse.status match {
      case 200 =>
        val jsResponse = Json.parse(wsResponse.body)
        val receiptId = (jsResponse \ "imageUUID").validate[String].get
        val receipt = Receipt(0, receiptId, userProfile.id, time.getDayOfMonth,
          time.getMonthOfYear, time.getYear, userProfile.email, None, p3UserId, config.load.p3ClientId,
          fileName, None, None, None, time, 0, "", "[]", pointSent = false)

        val hasReceiptStore = receiptRepository.store(receipt)

        hasReceiptStore map {
          case true  =>
            eventRepository.store(Event(0, userProfile.id, "upload", Some(remoteIp), time)).flatMap {
              case true  =>
                p3UserInfoRepository.fetchByEmail(userProfile.email) map {
                  case Some(userInfo) =>
                    eventSender.insertUploadEvent(userInfo.p3UserId, userProfile.firstName)
                    val unsubscribeLink = s"http://${request.host}/user/${userInfo.p3UserId}/unsubscribe/email"
                    sendGridService.sendEmailForUpload(userProfile.email, userProfile.firstName, unsubscribeLink)
                  case None           =>
                    Logger.info(s"Could not get p3 user info by email ${userProfile.email} while processing receipt")
                    Future.successful(InternalServerError(message("common.error.message")))
                }
              case false =>
                Logger.error(s"Could not store upload event in events table for user profile id ${userProfile.id} while processing receipt")
                Future.successful(InternalServerError(message("common.error.message")))
            }
          case false =>
            Logger.error(s"Could not store receipt details in receipts table for user profile id ${userProfile.id} while processing receipt")
            Future.successful(InternalServerError(message("common.error.message")))
        }
      case _   =>
    }
    wsResponse.status
  }


  private def fileGenerate(imageTypeInfo: String): (String, String) = {
    val currentTimeMillis = System.currentTimeMillis()

    val (mimeType, extension) =
      if (imageTypeInfo.contains("jpg") || imageTypeInfo.contains("jpeg")) {
        ("image/jpeg", ".jpg")
      } else if (imageTypeInfo.contains("png")) {
        ("image/png", ".png")
      } else if (imageTypeInfo.contains("gif")) {
        ("image/gif", ".gif")
      }
    val hashedFileName = util.md5Hash("Image Title" + "-" + currentTimeMillis) + extension

    (hashedFileName, mimeType.toString)
  }

  protected def buildUploadRequest(userProfile: Profile,
                                   accessToken: String,
                                   domainUrl: String,
                                   p3UserId: String,
                                   fileName: String,
                                   contentType: String,
                                   file: File): AHCRequest = {
    new RequestBuilder("POST")
      .setUrl(config.load.ocrApiUrl + "/ocr/image/upload")
      .setHeader("Content-Type", "multipart/form-data")
      .addBodyPart(new StringPart("access_token", accessToken))
      .addBodyPart(new FilePart("image", file, contentType, Constants.UTF8, fileName))
      .addBodyPart(new StringPart("callback_url", domainUrl + "/callback/receive"))
      .addBodyPart(new StringPart("first_name", userProfile.firstName))
      .addBodyPart(new StringPart("last_name", userProfile.lastName))
      .addBodyPart(new StringPart("email", userProfile.email))
      .addBodyPart(new StringPart("receipt_user_id", p3UserId))
      .addBodyPart(new StringPart("campaign_id", config.load.p3CampaignId))
      .build()

  }
}
