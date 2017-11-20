package utilities

import models.Profile
import play.api.libs.json._
import play.api.mvc.RequestHeader

class SessionHelper {

  implicit val jodaDateReads = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss'Z'")
  implicit val jodaDateWrites = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss'Z'")

  implicit val formatProfile = Json.format[Profile]

  def getUserFromSession(implicit request: RequestHeader): Option[Profile] = {
    val sessionStateString = request.session.get("userInfo").getOrElse("")

    if (sessionStateString.equals("")) {
      None
    } else {
      Some(Json.parse(sessionStateString).validate[Profile].get)
    }
  }

  def isReceiptUploaded(implicit request: RequestHeader): Option[String] = {
    val sessionStateString = request.session.get("receipt").getOrElse("")

    if (sessionStateString.equals("")) {
      None
    } else {
      Some(sessionStateString)
    }
  }

}

object SessionHelper extends SessionHelper
