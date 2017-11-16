package controllers

import javax.inject.{Inject, Named}

import actors.RewardActor._
import akka.actor.ActorRef
import akka.pattern.ask
import forms.UserForm
import models._
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages, MessagesImpl}
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


class Application @Inject()(controllerComponent: ControllerComponents,
                            userForm: UserForm,
                            @Named("reward-codes-actor") rewardActor: ActorRef)
  extends AbstractController(controllerComponent) with I18nSupport {

  implicit val jodaDateReads = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss'Z'")
  implicit val jodaDateWrites = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss'Z'")

  implicit val formatProfile = Json.format[Profile]

  implicit val messages: Messages = MessagesImpl(controllerComponent.langs.availables.head, controllerComponent.messagesApi)

  def index: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.index(messages("Zacbrown"), userForm.userSupportForm)).withNewSession
  }

  def faq1: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.content.faq1()).withNewSession
  }

  def faq2: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.content.faq2()).withNewSession
  }

  def faq3: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.content.faq3()).withNewSession
  }

  def faq4: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.content.faq4()).withNewSession
  }

  def termsModal: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.termsModal()).withNewSession
  }

  def register: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.content.register(userForm.signUpForm)).withNewSession
  }

  def login: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.content.login(userForm.loginForm)).withNewSession
  }

  def upload: Action[AnyContent] = Action { implicit request =>
    val sessionStateString = request.session.get("userInfo").getOrElse("")

    if (sessionStateString.equals("")) {
      Redirect(routes.Application.register())
    } else {
      val user = Json.parse(sessionStateString).validate[Profile].get
      Ok(views.html.content.upload(Some(user)))
    }
  }
}