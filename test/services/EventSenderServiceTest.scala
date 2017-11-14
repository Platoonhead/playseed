package services

import org.mockito.Mockito._
import org.specs2.mock.Mockito
import play.api.libs.json.JsValue
import play.api.libs.ws.{BodyWritable, WSClient, WSRequest, WSResponse}
import play.api.test.PlaySpecification
import utilities.{ConfigLoader, PlatformConfig}

import scala.concurrent.Future

class EventSenderServiceTest extends PlaySpecification with Mockito {
  private val mockedWSClient = mock[WSClient]
  private val mockedResponse = mock[WSResponse]
  private val mockedWSRequest = mock[WSRequest]
  private val mockedConfig = mock[ConfigLoader]

  def getMockedObject = new EventSenderService(mockedWSClient, mockedConfig)

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

  "Event Sender Service" should {

    "ingest sign up event" in {
      val eventService = getMockedObject

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.status) thenReturn 200
      when(mockedWSClient.url("http://P3_API_DOMAIN_EVENT")) thenReturn mockedWSRequest
      when(mockedWSRequest.post(any[JsValue])(any[BodyWritable[JsValue]])) thenReturn Future.successful(mockedResponse)

      val result = eventService.insertSignUpEvent("p3UserId1234", "test")

      result must be equalTo true
    }

    "ingest login event" in {
      val eventService = getMockedObject

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.status) thenReturn 200
      when(mockedWSClient.url("http://P3_API_DOMAIN_EVENT")) thenReturn mockedWSRequest
      when(mockedWSRequest.post(any[JsValue])(any[BodyWritable[JsValue]])) thenReturn Future.successful(mockedResponse)

      val result = eventService.insertLoginEvent("p3UserId1234", "test")

      result must be equalTo true
    }

    "ingest upload receipt event" in {
      val eventService = getMockedObject

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.status) thenReturn 200
      when(mockedWSClient.url("http://P3_API_DOMAIN_EVENT")) thenReturn mockedWSRequest
      when(mockedWSRequest.post(any[JsValue])(any[BodyWritable[JsValue]])) thenReturn Future.successful(mockedResponse)

      val result = eventService.insertUploadEvent("p3UserId1234", "test")

      result must be equalTo true
    }

  }
}
