package controllers

import org.specs2.mock.Mockito
import play.api.i18n.MessagesApi
import play.api.mvc.{Result, Results}
import play.api.test.{FakeRequest, PlaySpecification}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

case object CustomException extends Exception

class ErrorHandlerTest extends PlaySpecification with Results with Mockito {
  implicit val mockedMessageApi = mock[MessagesApi]
  val controller = new ErrorHandler()

  "ErrorHandler" should {
    "test client side error" in {

      val res: Future[Result] = controller.onClientError(FakeRequest(), 404, "message")
      val result: Result = Await.result(res, 10.seconds)
      status(res) must equalTo(404)
    }

    "test server side error" in {
      val res: Future[Result] = controller.onServerError(FakeRequest(), CustomException)
      val result: Result = Await.result(res, 10.seconds)
      status(res) must equalTo(500)
    }
  }

}
