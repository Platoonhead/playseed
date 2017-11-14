package services

import javax.inject.Inject

import play.api.libs.ws.WSClient
import utilities.ConfigLoader

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try

class GoogleReCaptchaService @Inject()(wsClient: WSClient, config: ConfigLoader) {

  def checkReCaptchaValidity(captcha: String, ip: String): Boolean = {
    TryToOption("Google Recaptcha request:") {
      Try {
        val CAPTCHA_SECRET = config.load.googleRECaptchaCode
        val url = "https://www.google.com/recaptcha/api/siteverify"

        val response = wsClient.url(url).post(Map("secret" -> Seq(CAPTCHA_SECRET), "response" -> Seq(captcha), "remoteip" -> Seq(ip))).map { response =>
          (response.json \ "success").as[Boolean]
        }

        Await.result(response, 20.seconds)
      }
    } getOrElse false
  }
}
