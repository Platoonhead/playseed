package controllers

import com.google.inject.Inject
import forms.{SupportForm, UserForm}
import models.{SupportRepository, Support => UserSupport}
import play.api.Logger
import play.api.i18n.{Messages, MessagesImpl}
import play.api.mvc._
import services.SendGridService
import utilities.Util._

import scala.concurrent.Future

class Support @Inject()(controllerComponents: ControllerComponents,
                        userForm: UserForm,
                        supportRepository: SupportRepository,
                        sendGridService: SendGridService) extends AbstractController(controllerComponents) {
  implicit val messages: Messages = MessagesImpl(controllerComponents.langs.availables.head, controllerComponents.messagesApi)

  def userSupport: Action[AnyContent] = Action.async { implicit request =>

    val time = getPacificTime

    userForm.userSupportForm.bindFromRequest.fold(
      formWithError => {
        Logger.info(s"Bad request in user support form ${formWithError.errors}")
        Future.successful(BadRequest(views.html.content.support(formWithError)))
      },
      data => {
        val userName = data.name.trim
        val userEmail = data.email.trim.toLowerCase

        val supportDetail = UserSupport(0, userName, userEmail, data.message, time)
        supportRepository.store(supportDetail)
        sendGridService.sendEmailForSupport(userName, userEmail, data.message).fold {

          Logger.error(s"Internal server error while sending support email for email address $userEmail")
          Future.successful(Ok(views.html.content.support(userForm.userSupportForm.fill(SupportForm(data.name, data.email, data.message))
            .withGlobalError("Unable to send Message!"))))
        } {
          _ =>
            Logger.info(s"Support email successfully sent for email $userEmail")
            Future.successful(Redirect(routes.Application.support()).flashing("success" -> "Message successfully sent!"))
        }
      }
    )
  }

}
