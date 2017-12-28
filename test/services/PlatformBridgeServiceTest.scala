package services

import models.{P3UserInfo, P3UsersInfoRepository, Profile}
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import play.api.libs.json.JsValue
import play.api.libs.ws.{BodyWritable, WSClient, WSRequest, WSResponse}
import play.api.test.PlaySpecification
import utilities.{ConfigLoader, PlatformConfig, Util}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PlatformBridgeServiceTest extends PlaySpecification with Mockito {
  private val mockedP3userInfoRepo = mock[P3UsersInfoRepository]
  private val mockedWSClient = mock[WSClient]
  private val mockedResponse = mock[WSResponse]
  private val mockedWSRequest = mock[WSRequest]
  private val mockedConfig = mock[ConfigLoader]

  def getMockedObject = new PlatformBridgeService(mockedP3userInfoRepo, mockedWSClient, mockedConfig)

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

  private val dob = "%s-%s-%s" format(1991, 9, 8)
  private val date = Util.getPacificTime

  private val userProfile = Profile(1, "firstname_test", "lastname_test", "test@example.com", DateTime.parse(dob), date, suspended = false)
  private val p3UserInfo = P3UserInfo(0, "6424c63afe85e23875bf2a749cc6e6cd", "test@examaple.com", new DateTime(1470633591))

  "Platform Bridge Service" should {
    "check user exist by email" in {
      val platformService = getMockedObject

      val json =
        """{"compositeKey":"d98abf03-ee2a-4b46-b15e-0d4e29a359bb","socialId":{"userId":"teena@knoldus.com",
          |"providerId":"userpass"},"userId":"user12345","email":"teena@knoldus.com",
          |"authMethod":{"method":"userpass"},"clientid":"clientId123"}""".stripMargin.replaceAll("\n", "")

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.body) thenReturn json
      when(mockedResponse.status) thenReturn 200
      when(mockedWSClient.url(any[String])) thenReturn mockedWSRequest
      when(mockedWSRequest.get) thenReturn Future.successful(mockedResponse)

      val result = platformService.checkUserExistsByEmail("test@example.com")

      result must be equalTo Some("user12345")
    }

    "could not find user by email" in {
      val platformService = getMockedObject

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.body) thenReturn "Object not found."
      when(mockedResponse.status) thenReturn 404
      when(mockedWSClient.url(any[String])) thenReturn mockedWSRequest
      when(mockedWSRequest.get) thenReturn Future.successful(mockedResponse)

      val result = platformService.checkUserExistsByEmail("test@example.com")

      result must be equalTo None
    }

    "check user profile exists by guuid" in {
      val platformService = getMockedObject

      val json =
        """{"compositeKey":"7adedceb3e291a9c3f8c44d28d52e637-b21165ed2b5752831c4a5406c90c6ca","firstName":"Teena",
          |"lastName":"sharma","email":"teena@knoldus.com","emailHash":"36f0a8e64bd8cb9f2d30ea2ea5c184b3","birth":-19800000,
          |"gender":"na","phone": null,"city":"gfgg","postal":"15269","receiveEmail":false, "receiveSms":false,
          |"created":1474618832657,"updated":1486385415785,"suspended":false,  "address":"abcdefg",
          |"suite":"","state":"de","country":"","shippingInstructions": "" }""".stripMargin.replaceAll("\n", "")

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.status) thenReturn 200
      when(mockedWSClient.url(any[String])) thenReturn mockedWSRequest
      when(mockedWSRequest.get) thenReturn Future.successful(mockedResponse)

      val result = platformService.checkProfileExistsByGuid("user12345")

      result must be equalTo Some(true)
    }

    "could not find user profile exists by guuid" in {
      val platformService = getMockedObject

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.status) thenReturn 404
      when(mockedWSClient.url(any[String])) thenReturn mockedWSRequest
      when(mockedWSRequest.get) thenReturn Future.successful(mockedResponse)

      val result = platformService.checkProfileExistsByGuid("user12345")

      result must be equalTo Some(false)
    }

    "got an exception while checking user profile exists by guuid" in {
      val platformService = getMockedObject

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.status) thenReturn 500
      when(mockedWSClient.url(any[String])) thenReturn mockedWSRequest
      when(mockedWSRequest.get) thenReturn Future.successful(mockedResponse)

      val result = platformService.checkProfileExistsByGuid("user12345")

      result must be equalTo None
    }

    "create user" in {
      val platformService = getMockedObject

      val user =
        """{"guid":"6424c63afe85e23875bf2a749cc6e6cd","email":"test@example.com","passwordInfo":
          |{"hasher":"bcrypt","password":"$2a$10$BdS5MpOwGR/V8QiVwL/FROblE2hnpkckHUUJ.tD9K1OM9.JmWYj2y"},
          |"firstName":"test123","lastName":"test345","suspended":false,"confirmPass":false,
          |"created":1486732293551}""".stripMargin.replaceAll("\n", "")

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.status) thenReturn 200
      when(mockedResponse.body) thenReturn user
      when(mockedWSClient.url(any[String])) thenReturn mockedWSRequest
      when(mockedWSRequest.post(any[JsValue])(any[BodyWritable[JsValue]])) thenReturn Future.successful(mockedResponse)
      when(mockedP3userInfoRepo.fetchByEmail("test@example.com")) thenReturn Future.successful(Some(p3UserInfo))

      val futureResult = platformService.createUser(userProfile)

      val result = Await.result(futureResult, 10.seconds)

      result must be equalTo Some("6424c63afe85e23875bf2a749cc6e6cd")
    }

    "could not create user because of malformed data" in {
      val platformService = getMockedObject

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.status) thenReturn 400
      when(mockedWSClient.url(any[String])) thenReturn mockedWSRequest
      when(mockedWSRequest.post(any[JsValue])(any[BodyWritable[JsValue]])) thenReturn Future.successful(mockedResponse)

      val futureResult = platformService.createUser(userProfile)

      val result = Await.result(futureResult, 10.seconds)

      result must be equalTo None
    }

    "could not create user because user already exists" in {
      val platformService = getMockedObject

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.status) thenReturn 409
      when(mockedWSClient.url(any[String])) thenReturn mockedWSRequest
      when(mockedWSRequest.post(any[JsValue])(any[BodyWritable[JsValue]])) thenReturn Future.successful(mockedResponse)

      val futureResult = platformService.createUser(userProfile)

      val result = Await.result(futureResult, 10.seconds)

      result must be equalTo Some("409")
    }

    "create user profile" in {
      val platformService = getMockedObject

      val user =
        """{"guid":"6424c63afe85e23875bf2a749cc6e6cd","email":"test@example.com","passwordInfo":
          |{"hasher":"bcrypt","password":"$2a$10$BdS5MpOwGR/V8QiVwL/FROblE2hnpkckHUUJ.tD9K1OM9.JmWYj2y"},
          |"firstName":"test123","lastName":"test345","suspended":false,"confirmPass":false,
          |"created":1486732293551}""".stripMargin.replaceAll("\n", "")

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.status) thenReturn 200
      when(mockedResponse.body) thenReturn user
      when(mockedWSClient.url(any[String])) thenReturn mockedWSRequest
      when(mockedWSRequest.post(any[JsValue])(any[BodyWritable[JsValue]])) thenReturn Future.successful(mockedResponse)

      val result = platformService.createProfile("6424c63afe85e23875bf2a749cc6e6cd", userProfile)

      result must be equalTo Some("6424c63afe85e23875bf2a749cc6e6cd")
    }

    "could not create user profile because of mallformed data" in {
      val platformService = getMockedObject

      val user =
        """{"guid":"6424c63afe85e23875bf2a749cc6e6cd","email":"test@example.com","passwordInfo":
          |{"hasher":"bcrypt","password":"$2a$10$BdS5MpOwGR/V8QiVwL/FROblE2hnpkckHUUJ.tD9K1OM9.JmWYj2y"},
          |"firstName":"test123","lastName":"test345","suspended":false,"confirmPass":false,
          |"created":1486732293551}""".stripMargin.replaceAll("\n", "")

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.status) thenReturn 404
      when(mockedResponse.body) thenReturn user
      when(mockedWSClient.url(any[String])) thenReturn mockedWSRequest
      when(mockedWSRequest.post(any[JsValue])(any[BodyWritable[JsValue]])) thenReturn Future.successful(mockedResponse)

      val result = platformService.createProfile("6424c63afe85e23875bf2a749cc6e6cd", userProfile)

      result must be equalTo None
    }

    "could not create user profile because of some exception" in {
      val platformService = getMockedObject

      val user =
        """{"guid":"6424c63afe85e23875bf2a749cc6e6cd","email":"test@example.com","passwordInfo":
          |{"hasher":"bcrypt","password":"$2a$10$BdS5MpOwGR/V8QiVwL/FROblE2hnpkckHUUJ.tD9K1OM9.JmWYj2y"},
          |"firstName":"test123","lastName":"test345","suspended":false,"confirmPass":false,
          |"created":1486732293551}""".stripMargin.replaceAll("\n", "")

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.status) thenReturn 500
      when(mockedResponse.body) thenReturn user
      when(mockedWSClient.url(any[String])) thenReturn mockedWSRequest
      when(mockedWSRequest.post(any[JsValue])(any[BodyWritable[JsValue]])) thenReturn Future.successful(mockedResponse)

      val result = platformService.createProfile("6424c63afe85e23875bf2a749cc6e6cd", userProfile)

      result must be equalTo None
    }

    "un subscribe from email by p3 user id" in {
      val platformService = getMockedObject

      val json =
        """{"unsubscrbed":"true"}""".stripMargin.replaceAll("\n", "")

      when(mockedConfig.load) thenReturn platformConfig
      when(mockedResponse.body) thenReturn json
      when(mockedResponse.status) thenReturn 200
      when(mockedWSClient.url(any[String])) thenReturn mockedWSRequest
      when(mockedWSRequest.get) thenReturn Future.successful(mockedResponse)

      val result = platformService.unsubscribeFromEmails("user12345")

      result must be equalTo Some("user12345")
    }
  }
}
