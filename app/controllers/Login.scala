package controllers

import com.google.inject.Inject
import forms.UserForm
import models._
import play.api.Logger
import play.api.i18n.{Lang, Messages, MessagesImpl}
import play.api.libs.json._
import play.api.mvc._
import services.EventSenderService
import utilities.Util._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Login @Inject()(controllerComponent: ControllerComponents,
                      userForm: UserForm,
                      userProfileRepository: UserProfileRepository,
                      blockListRepository: BlockListRepository,
                      eventRepository: EventRepository,
                      p3UserInfoRepository: P3UsersInfoRepository,
                      eventSender: EventSenderService,
                      receiptRepository: ReceiptRepository) extends AbstractController(controllerComponent) {

  implicit val jodaDateReads = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss'Z'")
  implicit val jodaDateWrites = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss'Z'")
  implicit val formatProfile = Json.format[Profile]

  val lang: Lang = controllerComponent.langs.availables.head
  implicit val message: Messages = MessagesImpl(lang, messagesApi)

  def login: Action[AnyContent] = Action.async { implicit request =>
    val time = getPacificTime
    val messages: Messages = controllerComponent.messagesApi.preferred(request)

    userForm.loginForm.bindFromRequest.fold(
      formWithError => {
        Logger.info(s"Bad request while verifying the user ${formWithError.errors}")
        Future.successful(BadRequest(views.html.content.login(formWithError)))
      },
      data => {
        val email = data.trim.toLowerCase
        userProfileRepository.findByEmail(email).flatMap {
          case Some(userProfile: Profile) =>
            blockListRepository.shouldEnter(email).flatMap {
              case true  =>
                val jsonStringProfile = Json.toJson(userProfile).toString()
                Logger.info(s"User already exists with email ${userProfile.email}")

                eventRepository.store(Event(0, userProfile.id, "signin", Some(request.remoteAddress), time))
                p3UserInfoRepository.fetchByEmail(userProfile.email).flatMap {
                  case Some(userInfo) =>
                    eventSender.insertLoginEvent(userInfo.p3UserId, userProfile.firstName + " " + userProfile.lastName)

                    receiptRepository.shouldSubmit(userProfile.id).flatMap {
                      case true  =>
                        Future.successful(Redirect(routes.Application.upload()).withSession("userInfo" -> jsonStringProfile))
                      case false =>
                        Logger.info(s"User has already submitted receipt for today with email ${userProfile.email}")
                        Future.successful(Redirect(routes.Application.upload())
                          .withSession("userInfo" -> jsonStringProfile, "receipt" -> "uploaded"))
                    }
                  case None           =>
                    Logger.info(s"P3UserInfo is not found by email ${userProfile.email}")
                    Future.successful(Ok(views.html.content.notregisteredpopup(userForm.loginForm.fill(data))))
                }
              case false =>
                Logger.info(s"User tried to login with blocked email $email")
                Future.successful(Ok(views.html.content.login(userForm.loginForm.fill(data).withGlobalError(messages("validation.login.blocked")))))
            }
          case None                       =>
            Logger.info(s"user not found by email $email")
            Future.successful(Ok(views.html.content.notregisteredpopup(userForm.loginForm.fill(data))))
        }
      })
  }
}
