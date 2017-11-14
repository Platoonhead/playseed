package controllers

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.i18n._
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject()(implicit val messagesApi: MessagesApi) extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Logger.error(s"Got an error while handling client request $request, status code $statusCode message $message")

    Future.successful(NotFound(views.html.error.error404()))
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Logger.error(s"Got an exception while handling server request $request, exception $exception")

    Future.successful(InternalServerError(views.html.error.error500()))
  }
}
