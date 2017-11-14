package controllers

import com.google.inject.Inject
import forms.UserForm
import models.{Support => UserSupport, SupportRepository}
import org.joda.time.DateTime
import play.api.Logger
import play.api.i18n.{MessagesImpl, Messages, MessagesApi}
import play.api.mvc._
import services.SendGridService
import utilities.Util
import utilities.Util._

import scala.concurrent.Future

class Support @Inject()(controllerComponents: ControllerComponents,
                        userForm: UserForm,
                        supportRepository: SupportRepository,
                        sendGridService: SendGridService) extends AbstractController(controllerComponents) {
  implicit val messages: Messages = MessagesImpl(controllerComponents.langs.availables.head, controllerComponents.messagesApi)

  def userSupport: Action[AnyContent] = Action.async { implicit request =>
    val time = getPacificTime
    val message: Messages = controllerComponents.messagesApi.preferred(request)

    userForm.userSupportForm.bindFromRequest.fold(
      formWithError => {

        Logger.info(s"Bad request in user support form ${formWithError.errors}")
        Future.successful(BadRequest(views.html.index(message("home.page.title"), formWithError)))
      },
      data => {
        val userName = data.name.trim
        val userEmail = data.email.trim

        val supportDetail = UserSupport(0, userName, userEmail, data.message, time)
        supportRepository.store(supportDetail)
        sendGridService.sendEmailForSupport(userName, userEmail, data.message).fold {

          Logger.error(s"Internal server error while sending support email for email address $userEmail")
          Future.successful(InternalServerError("Email not sent"))
        } {
          response =>
            Logger.info(s"Support email successfully sent for email $userEmail")
            Future.successful(Redirect(routes.Application.index()))
        }
      }
    )
  }

}
