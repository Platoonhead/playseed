package controllers

import java.io.File

import forms.{DateOfBirth, Email, User}
import models._
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import org.specs2.mock.Mockito
import play.api.libs.Files
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc.MultipartFormData.{BadPart, FilePart}
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import play.api.test.Helpers._
import play.api.test._
import play.shaded.ahc.org.asynchttpclient.{AsyncHttpClient, Request => AHCRequest}
import services.{EventSenderService, SendGridService}
import utilities.{ConfigLoader, PlatformConfig, Util}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

class ReceiptsTest extends PlaySpec with Mockito {
  implicit val jodaDateReads = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss'Z'")
  implicit val jodaDateWrites = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss'Z'")

  implicit val formatAccessRequest = Json.format[AccessRequest]
  implicit val formatProfile = Json.format[Profile]

  import play.api.i18n._

  implicit val lang = Lang("en-US")

  val date = Util.getPacificTime

  private val p3UserInfo = P3UserInfo(1, "p3UserId", "test@example.com", new DateTime(0L))
  val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
    "address1", Some("address2"), "V1V 1V1", "1234567891", "province",  "recaptcha", isAgree = true)
  val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
  val dob = DateTime.parse(dateInString)

  private val profile = Profile(1, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
    "1245678903", date, suspended = false)
  val jsonStringProfile = Json.toJson(profile).toString()

  private val platformConfig =
    PlatformConfig(
      "clientId123",
      "P3_API_DOMAIN_ACCOUNT",
      "P3_API_DOMAIN_EVENT",
      "P3_API_DOMAIN_INGESTOR",
      "GOOGLE_CAPTCHA_SECRET",
      "P3_API_CAMPAIGN_ID",
      "SIGNUP_BEHAVIOUR_ID",
      "LOGIN_BEHAVIOUR_ID",
      "UPLOAD_BEHAVIOUR_ID",
      "localhost:9000",
      "SNAP3_CLIENT_KEY",
      "SNAP3_CLIENT_SECRET")

  "should not able to upload a new receipt when access token not found" in {
    val receiptObjects = testObjects

    val parameters = Map[String, Seq[String]]("" -> Seq(""), "" -> Seq(""))
    val tempFile = Files.SingletonTemporaryFileCreator.create("multipartBody", "TemporaryFile")
    val files = Seq[FilePart[TemporaryFile]](FilePart("enc", "enc", Some("multipart/form-data"), tempFile))
    val multipartBody = MultipartFormData(parameters, files, Seq[BadPart]())

    val mockedResponse = mock[WSResponse]
    val mockedWSRequest = mock[WSRequest]

    val request = FakeRequest().withMultipartFormDataBody(multipartBody)

    when(receiptObjects.securedImplicit.p3UserInfo()(any[Request[AnyContent]])) thenReturn p3UserInfo
    when(receiptObjects.securedImplicit.userProfile()(any[Request[AnyContent]])) thenReturn profile
    when(receiptObjects.config.load) thenReturn platformConfig
    when(receiptObjects.wsClient.url(any[String])) thenReturn mockedWSRequest
    when(mockedWSRequest.get) thenReturn Future.successful(mockedResponse)
    when(receiptObjects.messagesApi("error.access.token")) thenReturn "Access token not found"

    val res = receiptObjects.receiptController.receiveReceipt()(request)

    status(res) must equal(INTERNAL_SERVER_ERROR)
  }

  "should be able to upload a new receipt" in {
    val receiptObjects = testObjects

    val mockedWSRequest = mock[WSRequest]
    val mockedAsyncHttpClient = mock[AsyncHttpClient]

    val json = """{"access_token":"access_token123"}"""

    val parameters = Map[String, Seq[String]]("access_token" -> Seq("bdqbdj"), "callback_url" -> Seq("http://google.com"))
    val tempFile = Files.SingletonTemporaryFileCreator.create("multipartBody", "TemporaryFile")
    val files = Seq[FilePart[TemporaryFile]](FilePart("enc", "enc", Some("image/jpeg"), tempFile))
    val multipartBody = MultipartFormData(parameters, files, Seq[BadPart]())

    val receipt = Receipt(0, "64c4cfd0-b9ab-4afd-a7b1-ceec6ac52f6b", profile.id, date.getDayOfMonth,
      date.getMonthOfYear, date.getYear, profile.email, None, "p3UserId", "clientId123", "imageName.jpg", None, None, None, date, 0, "", "", pointSent = false)

    val request = FakeRequest().withMultipartFormDataBody(multipartBody)
    val remoteIp = FakeRequest().remoteAddress

    when(receiptObjects.securedImplicit.p3UserInfo()(any[Request[AnyContent]])) thenReturn p3UserInfo
    when(receiptObjects.securedImplicit.userProfile()(any[Request[AnyContent]])) thenReturn profile
    when(receiptObjects.config.load) thenReturn platformConfig
    when(receiptObjects.wsResponse.body) thenReturn json
    when(receiptObjects.wsResponse.status) thenReturn 200
    when(receiptObjects.wsClient.url(any[String])) thenReturn mockedWSRequest
    when(mockedWSRequest.get) thenReturn Future.successful(receiptObjects.wsResponse)
    when(receiptObjects.util.getPacificTime) thenReturn date
    when(receiptObjects.util.md5Hash(any[String])) thenReturn "imageName"
    when(receiptObjects.receiptRepository.store(any[Receipt])) thenReturn Future(true)
    when(receiptObjects.eventRepository.store(any[Event])) thenReturn Future(true)
    when(receiptObjects.p3UserInfoRepository.fetchByEmail(profile.email)) thenReturn Future(Some(P3UserInfo(1L, "p3_userId", profile.email, date)))
    when(receiptObjects.eventSender.insertUploadEvent("p3_userId", "test")) thenReturn true
    when(receiptObjects.sendGridService.sendEmailForUpload("text@example.com", "test")) thenReturn Some("200")
    val response = receiptObjects.receiptController.receiveReceipt()(request)

    status(response) must equal(OK)
    contentAsString(response) must equal("s")
  }

  private def testObjects: TestObjects = {
    val mockedEventRepository = mock[EventRepository]
    val mockedP3UserInfoRepository = mock[P3UsersInfoRepository]
    val mockedReceiptRepository = mock[ReceiptRepository]
    val mockedWsClient = mock[WSClient]
    val mockedSecuredImplicit = mock[SecuredImplicits]
    val mockedSendGridService = mock[SendGridService]
    val mockedEventSender = mock[EventSenderService]
    val mockedConfig = mock[ConfigLoader]
    val mockedMessagesApi = mock[MessagesApi]
    val mockedAhcRequest = mock[AHCRequest]
    val mockedResponse = mock[WSResponse]
    val mockedUtil = mock[Util]

    val json ="""{"imageUUID":"64c4cfd0-b9ab-4afd-a7b1-ceec6ac52f6b"}"""

    val controller = new Receipts(stubControllerComponents(), mockedP3UserInfoRepository, mockedEventSender, mockedEventRepository, mockedReceiptRepository,
      mockedSecuredImplicit, mockedConfig, mockedWsClient, mockedSendGridService, mockedUtil) {

      override protected def buildUploadRequest(userProfile: Profile,
                                                accessToken: String,
                                                domainUrl: String,
                                                p3UserId: String,
                                                fileName: String,
                                                contentType: String,
                                                file: File) = mockedAhcRequest

      override protected def executeRequest(request: AHCRequest) = {
        when(mockedResponse.body) thenReturn json
        when(mockedResponse.status) thenReturn 200
        Future(mockedResponse)
      }
    }

    TestObjects(mockedP3UserInfoRepository, mockedEventSender, mockedEventRepository, mockedReceiptRepository, mockedSecuredImplicit,
      mockedConfig, mockedWsClient, mockedSendGridService, mockedMessagesApi, mockedAhcRequest, mockedResponse, mockedUtil, controller)
  }

  case class TestObjects(p3UserInfoRepository: P3UsersInfoRepository,
                         eventSender: EventSenderService,
                         eventRepository: EventRepository,
                         receiptRepository: ReceiptRepository,
                         securedImplicit: SecuredImplicits,
                         config: ConfigLoader,
                         wsClient: WSClient,
                         sendGridService: SendGridService,
                         messagesApi: MessagesApi,
                         ahcRequest: AHCRequest,
                         wsResponse: WSResponse,
                         util: Util,
                         receiptController: Receipts)

}
