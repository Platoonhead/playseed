package utilities

import forms.{DateOfBirth, Email, User}
import models.Profile
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import play.api.test.FakeRequest

class SessionHelperTest extends PlaySpec {
  implicit val jodaDateReads = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss'Z'")
  implicit val jodaDateWrites = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss'Z'")
  val sessionHelperObj = new SessionHelper

  val date = Util.getPacificTime

  implicit val formatProfile = Json.format[Profile]
  val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
    "address1", Some("address2"), "V1V 1V1", "1234567891", "province",  "recaptcha", isAgree = true)
  val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
  val dob = DateTime.parse(dateInString)

  val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
    "1245678903", new DateTime(System.currentTimeMillis()), suspended = false)

  val jsonStringProfile = Json.toJson(profile).toString()

  "SessionHelper" should {

    "get user from session" in {

      val result = sessionHelperObj.getUserFromSession(FakeRequest().withSession("userInfo" -> jsonStringProfile, "receipt" -> "uploaded"))

      result.get.email must equal(profile.email)
    }

    "not get user from session" in {

      val result = sessionHelperObj.getUserFromSession(FakeRequest())

      result must equal(None)
    }
  }

}
