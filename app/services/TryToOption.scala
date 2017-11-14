package services

import play.api.Logger

import scala.util.{Failure, Success, Try}

object TryToOption {
  def apply[T](requestType: String = "")(result: Try[T]): Option[T] =
    result match {
      case Success(response) => Some(response)
      case Failure(ex) =>
        Logger.error(s"$requestType Got an exception in rest request, exception $ex")
        ex.printStackTrace()
        None
    }
}
