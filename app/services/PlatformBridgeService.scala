package services

import java.sql.Timestamp
import javax.inject.Inject

import models._
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import utilities.ConfigLoader

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

case class SocialId(userId: String, providerId: String)

case class AuthenticationMethod(method: String)

case class OAuth1Info(token: String, secret: String)

case class OAuth2Info(accessToken: String,
                      tokenType: Option[String] = None,
                      expiresIn: Option[Int] = None,
                      refreshToken: Option[String] = None)

case class AssociateId(compositeKey: String,
                       socialId: Option[SocialId],
                       userId: String,
                       email: String,
                       authMethod: AuthenticationMethod,
                       oAuth1Info: Option[OAuth1Info] = None,
                       oAuth2Info: Option[OAuth2Info] = None,
                       avatarUrl: Option[String] = None,
                       clientid: Option[String] = None)

class PlatformBridgeService @Inject()(p3userInfoRepo: P3UsersInfoRepository, wsClient: WSClient, config: ConfigLoader) {

  implicit val p3UserFormat = Json.format[P3User]
  implicit val p3UserPhone = Json.format[Phone]
  implicit val p3UserProfileFormat = Json.format[P3UserProfile]

  implicit val formatOAuth2Info = Json.format[OAuth2Info]
  implicit val formatOAuth1Info = Json.format[OAuth1Info]
  implicit val formatSocialId = Json.format[SocialId]
  implicit val formatAuthenticationMethod = Json.format[AuthenticationMethod]
  implicit val formatAssociateId = Json.format[AssociateId]

  def checkUserExistsByEmail(email: String): Option[String] = {
    try {
      val url = "http://%s/user/%s/associateUser/%s" format(config.load.p3DomainAccount, email, config.load.p3ClientId)
      val responseFuture = wsClient.url(url)
      val response = Await.result(responseFuture.get, 20 seconds)

      response.status match {
        case 200 =>
          Some(getP3UserId(response.body))
        case 404 =>
          Logger.info(s"[Bridge - Check user does not exist by email] , User doesn't exists email: $email")
          None
        case _   =>
          Logger.info(s"[Bridge - Check user exist by email] Issue reported, email: $email")
          throw new Exception("internal server error")
      }
    } catch {
      case ex: Exception =>
        Logger.error(s"[Bridge - Check user exist by email] Internal Error, email: $email, exception $ex")
        throw new Exception("internal server error")
    }
  }

  def checkProfileExistsByGuid(guid: String): Option[Boolean] = {
    try {
      val url = "http://%s/user/%s/profile/%s" format(config.load.p3DomainAccount, guid, config.load.p3ClientId)

      val responseFuture = wsClient.url(url).get
      val response = Await.result(responseFuture, 20 seconds)

      response.status match {
        case 200 =>
          Logger.info(s"[Bridge - Check profile exist by guid] Profile exists, guid: $guid")
          Some(true)
        case 404 =>
          Logger.info(s"[Bridge - Check profile exist by guid] Profile doesn't exists, guid: $guid")
          Some(false)
        case _   =>
          Logger.info(s"[Bridge - Check profile exist by guid] Server Issue, guid: $guid")
          None
      }
    } catch {
      case ex: Exception =>
        Logger.error(s"[Bridge - Check profile exist by guid] Internal Error, guid: $guid, exception $ex")
        None
    }
  }

  def createUser(profile: Profile): Future[Option[String]] = {
    try {
      val email = profile.email.toLowerCase
      val url = "http://%s/user/client/%s" format(config.load.p3DomainAccount, config.load.p3ClientId)
      val password = "12345678"
      val user = Json.toJson(P3User(profile.firstName, profile.lastName, profile.email, password))

      val responseFuture = wsClient.url(url)

      val response = Await.result(responseFuture.post(user), 20 seconds)

      response.status match {
        case 200 =>
          Logger.info("[Bridge - Create User] User created, email:" + profile.email)
          processReturnOneGuidFromCreateResponse(response.body, email)
        case 400 =>
          Logger.info("[Bridge - Create User] User not created malformed data, email:" + profile.email)
          Future(None)
        case 409 =>
          Logger.info("[Bridge - Create User] User exists, email:" + profile.email)
          Future(Some(response.status.toString))
        case _   =>
          Logger.info("[Bridge - Create User] Server Issue, email:" + profile.email)
          Future(None)
      }
    } catch {
      case ex: Exception =>
        Logger.error("[Bridge - Create User] Internal Error, email:" + profile.email)
        Future(None)
    }
  }

  def createProfile(guid: String, profile: Profile): Option[String] = {
    try {
      val url = "http://%s/user/%s/profile/%s" format(config.load.p3DomainAccount, guid, config.load.p3ClientId)
      val dob = new Timestamp(profile.dateOfBirth.getMillis)

      val userProfileJson: String = Json.toJson(P3UserProfile(
        profile.email,
        profile.firstName,
        profile.lastName,
        dob.getTime,
        "na",
        Phone("", ""),
        "na",
        "na",
        "na",
        "na",
        receiveEmail = false,
        receiveSms = false)).toString

      val responseFuture = wsClient.url(url).post(userProfileJson)
      val response = Await.result(responseFuture, 20 seconds)

      response.status match {
        case 200 =>
          Logger.info(s"[Bridge - Create Profile] Profile created, guid: $guid")
          Some(guid)
        case 400 =>
          Logger.info(s"[Bridge - Create Profile] Profile not created malformed data, guid: $guid")
          None
        case _   =>
          Logger.info(s"[Bridge - Create Profile] Profile not created, guid: $guid")
          None
      }
    } catch {
      case ex: Exception =>
        Logger.error(s"[Bridge - Create Profile] Internal Error, guid: $guid")
        None
    }
  }

  private def processReturnOneGuidFromCreateResponse(jsonUser: String, email: String): Future[Option[String]] = {
    val p3UserId = getP3UserIdFromJson(jsonUser)

    p3userInfoRepo.fetchByEmail(email) flatMap {
      case Some(user) => Future.successful(Some(user.p3UserId))
      case None       =>
        p3userInfoRepo.store(P3UserInfo(0, p3UserId, email, DateTime.now)) map {
          case true  => Some(p3UserId)
          case false => None
        }
    }
  }

  private def getP3UserIdFromJson(jsonString: String): String = {
    val firstPart = jsonString.substring(jsonString.indexOf("guid") + 7)
    val secondPart = firstPart.substring(0, firstPart.indexOf(",") - 1)

    secondPart
  }

  private def getP3UserId(jsonString: String): String = {
    val associatedId = Json.parse(jsonString).validate[AssociateId].get

    associatedId.userId
  }

}
