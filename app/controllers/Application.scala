package controllers

import javax.inject.Inject

import play.api.mvc._

class Application @Inject()(controllerComponent: ControllerComponents) extends AbstractController(controllerComponent) {

  def index: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.main())
  }
}
