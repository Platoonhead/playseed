package services

import org.mockito.Mockito._
import org.specs2.mock.Mockito
import play.api.libs.json.Json
import play.api.libs.ws.{BodyWritable, WSClient, WSRequest, WSResponse}
import play.api.test.PlaySpecification
import utilities.{ConfigLoader, PlatformConfig}

import scala.concurrent.Future

class GoogleReCaptchaServiceTest extends PlaySpecification with Mockito {

  private val mockedWSClient = mock[WSClient]
  private val mockedResponse = mock[WSResponse]
  private val mockedWSRequest = mock[WSRequest]
  private val mockedConfig = mock[ConfigLoader]

  def getMockedObject = new GoogleReCaptchaService(mockedWSClient, mockedConfig)

  private val platformConfig = PlatformConfig("clientId123",
    "P3_API_DOMAIN_ACCOUNT",
    "P3_API_DOMAIN_EVENT",
    "P3_API_DOMAIN_INGESTOR",
    "GOOGLE_CAPTCHA_SECRET",
    "P3_API_CAMPAIGN_ID",
    "SIGNUP_BEHAVIOUR_ID",
    "LOGIN_BEHAVIOUR_ID",
    "UPLOAD_BEHAVIOUR_ID",
    "OCR_API_URL",
    "SNAP3_CLIENT_KEY",
    "SNAP3_CLIENT_SECRET")

  "Google ReCaptcha Service" should {

    "check ReCaptcha validity" in {
      val googleRecaptchaService = getMockedObject

      val json = Json.parse(
        """{"success": true}""")

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.json) thenReturn json
      when(mockedResponse.status) thenReturn 200
      when(mockedWSClient.url(any[String])) thenReturn mockedWSRequest
      when(mockedWSRequest.post(any[Map[String, String]])(any[BodyWritable[Map[String, String]]])) thenReturn Future.successful(mockedResponse)

      val result = googleRecaptchaService.checkReCaptchaValidity("captcha", "remoteip")

      result must be equalTo true
    }

    "could not check ReCaptcha validity because of some exception" in {
      val googleRecaptchaService = getMockedObject

      val json = Json.parse(
        """{"error": true}""")

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.json) thenReturn json
      when(mockedResponse.status) thenReturn 200
      when(mockedWSClient.url(any[String])) thenReturn mockedWSRequest
      when(mockedWSRequest.post(any[Map[String, String]])(any[BodyWritable[Map[String, String]]])) thenReturn Future.successful(mockedResponse)

      val result = googleRecaptchaService.checkReCaptchaValidity("captcha", "remoteip")

      result must be equalTo false
    }
  }

}
