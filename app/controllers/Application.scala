package controllers

import javax.inject.Inject

import forms.UserForm
import models._
import play.api.i18n.{I18nSupport, Messages, MessagesImpl}
import play.api.libs.json._
import play.api.mvc._

class Application @Inject()(controllerComponent: ControllerComponents,
                            userForm: UserForm)
  extends AbstractController(controllerComponent) with I18nSupport {

  implicit val jodaDateReads = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss'Z'")
  implicit val jodaDateWrites = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss'Z'")

  implicit val formatProfile = Json.format[Profile]

  implicit val messages: Messages = MessagesImpl(controllerComponent.langs.availables.head, controllerComponent.messagesApi)

  def register: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.content.register(userForm.signUpForm)).withNewSession
  }

  def login: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.content.unsubscribefromemail()).withNewSession
  }

  def upload: Action[AnyContent] = Action { implicit request =>
    val sessionStateString = request.session.get("userInfo").getOrElse("")

    if (sessionStateString.equals("")) {
      Redirect(routes.Application.register())
    } else {
      Ok(views.html.content.upload())
    }
  }

  def support: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.content.support(userForm.userSupportForm)).withNewSession
  }

}
