package controllers

import javax.inject.Inject

import play.api.mvc._
import play.api.routing._

class JavascriptRoutes @Inject()(controllerComponent: ControllerComponents) extends AbstractController(controllerComponent) {

  def jsRoutes: Action[AnyContent] = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        controllers.routes.javascript.Receipts.receiveReceipt,
        controllers.routes.javascript.Application.register
      ))
  }
}
