package controllers

import javax.inject.Inject

import models.{P3UserInfo, P3UsersInfoRepository, Profile}
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request}
import utilities.ConfigLoader

import scala.concurrent.Await
import scala.concurrent.duration._

// .get is used everywhere because all this information should always be available for secured pages,
// if it fails InternalServerError should occur which would be handled by play

class SecuredImplicits @Inject()(p3UserInfoRepository: P3UsersInfoRepository,
                                 configLoader: ConfigLoader) {
  implicit val jodaDateReads = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss'Z'")
  implicit val jodaDateWrites = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss'Z'")

  implicit val formatProfile = Json.format[Profile]

  implicit def userProfile()(implicit request: Request[AnyContent]): Profile = {
    val sessionUserProfile = request.session.get("userInfo").get

    Json.parse(sessionUserProfile).validate[Profile].get
  }

  implicit def p3UserInfo()(implicit request: Request[AnyContent]): P3UserInfo = {
    val sessionUserProfile = request.session.get("userInfo").get
    val profile = Json.parse(sessionUserProfile).validate[Profile].get

    val futureResponse = p3UserInfoRepository.fetchByEmail(profile.email)

    Await.result(futureResponse, 20.seconds).get
  }
}
