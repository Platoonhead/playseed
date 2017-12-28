package controllers

import models.{Profile, Receipt, ReceiptRepository, UserProfileRepository}
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.ControllerComponents
import play.api.test.Helpers.stubControllerComponents
import play.api.test.{FakeRequest, PlaySpecification}
import services.SendGridService
import utilities.ConfigLoader

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CallbacksTest extends PlaySpecification with Mockito {

  "return an error message if invalid json is provided" in {
    val callbackObjects = testObjects

    val expectedResponse = """{"message":"Malformed data submitted"}"""

    val payload =
      """{
        |	"6p5c3x2v4x532p62401d6l4f1h3b27.jpg": {
        |		"submissionDate": "1469444737194",
        |		"rawData": "SOME RANDOM TEXT",
        |		"data": {
        |			"store": "target",
        |			"date": "07/23/2016",
        |			"amount": 12.329999923706055,
        |			"qualifyingAmount": 6.0,
        |			"products": [{
        |				"upcCode": "14800002737",
        |				"quantity": 3,
        |				"description": "Mott's Applesauce Granny Smith Pouch 4 pk",
        |				"productAmount": 2.0
        |			}]
        |		}
        |	}
        |}""".stripMargin

    val request = FakeRequest().withBody(Json.parse(payload).as[JsObject])

    val response = callbackObjects.callbackController.receive()(request)

    status(response) must be equalTo BAD_REQUEST
    contentAsString(response) mustEqual expectedResponse
  }

  "return success message if valid json is provided" in {
    val callbackObjects = testObjects

    val dateTime = new DateTime(System.currentTimeMillis())

    val receipt = Receipt(1, "receiptId", 2, 15, 2, 2017, "test@example.com", None, "userId", "clientId", "image",
      None, Some(2000), Some(1000), dateTime, 123456789, "dateInReceipt", "product")
    val receiptCallBack = ReceiptCallback(Some("approved"), None, Some("1469444737194"), "4298-90a8-97ee920abaaf",
      Some("SOME RANDOM TEXT"), Data("target", "07/23/2016", 12.329999923706055, 6.0, List(Products("14800002737", 3, Some("test"), 2.0))))

    val userProfile = Profile(1, "firstname_test", "lastname_test", "test@example.com",
      new DateTime(System.currentTimeMillis()), new DateTime(System.currentTimeMillis()), false)

    val serializedProductList = """[{"upcCode":"14800002737","quantity":3,"description":"test","productAmount":2}]"""
    val callback = "https://w1.buysub.com/servlet/OrdersGateway?cds_mag_code=PMM&cds_page_id=215226"

    val payload =
      """{
        |	"behaviorId.jpg": {
        |		"status": "approved",
        |		"submissionDate": "1469444737194",
        |		"UUID": "4298-90a8-97ee920abaaf",
        |		"rawData": "SOME RANDOM TEXT",
        |		"data": {
        |			"store": "target",
        |			"date": "07/23/2016",
        |			"amount": 12.329999923706055,
        |			"qualifyingAmount": 6.0,
        |			"products": [{
        |				"upcCode": "14800002737",
        |				"quantity": 3,
        |				"description": "test",
        |				"productAmount": 2.0
        |			}]
        |		}
        |	}
        |}""".stripMargin

    val request = FakeRequest().withBody(Json.parse(payload).as[JsObject])

    when(callbackObjects.receiptRepository.fetchByReceiptId("4298-90a8-97ee920abaaf")) thenReturn Future.successful(Some(receipt))
    when(callbackObjects.userProfileRepository.findByEmail("test@example.com")) thenReturn Future.successful(Some(userProfile))
    when(callbackObjects.receiptRepository.shouldSubmit(1)) thenReturn Future(true)
    when(callbackObjects.receiptRepository.update(receiptCallBack, serializedProductList)) thenReturn Future.successful(Some(receipt))
    when(callbackObjects.sendGridService.sendEmailForApproval("test@example.com", "firstname_test", "http://localhost/user/userId/unsubscribe/email")) thenReturn Some("success")

    val response = callbackObjects.callbackController.receive()(request)

    status(response) must be equalTo OK
    contentAsString(response) mustEqual "Email sent"
  }

  "return error message if email could not send for approved receipt" in {
    val callbackObjects = testObjects

    val dateTime = new DateTime(System.currentTimeMillis())
    val dob = "%s-%s-%s" format(1991, 9, 8)

    val receipt = Receipt(1, "receiptId", 2, 15, 2, 2017, "test@example.com", None, "userId", "clientId", "image",
      None, Some(2000), Some(1000), dateTime, 123456789, "dateInReceipt", "product")
    val receiptCallBack = ReceiptCallback(Some("approved"), None, Some("1469444737194"), "4298-90a8-97ee920abaaf",
      Some("SOME RANDOM TEXT"), Data("target", "07/23/2016", 12.329999923706055, 6.0, List(Products("14800002737", 3, Some("test"), 2.0))))

    val userProfile = Profile(1, "firstname_test", "lastname_test", "test@example.com",
      new DateTime(System.currentTimeMillis()), new DateTime(System.currentTimeMillis()), false)
    val serializedProductList = """[{"upcCode":"14800002737","quantity":3,"description":"test","productAmount":2}]"""

    val payload =
      """{
        |	"behaviorId.jpg": {
        |		"status": "approved",
        |		"submissionDate": "1469444737194",
        |		"UUID": "4298-90a8-97ee920abaaf",
        |		"rawData": "SOME RANDOM TEXT",
        |		"data": {
        |			"store": "target",
        |			"date": "07/23/2016",
        |			"amount": 12.329999923706055,
        |			"qualifyingAmount": 6.0,
        |			"products": [{
        |				"upcCode": "14800002737",
        |				"quantity": 3,
        |				"description": "test",
        |				"productAmount": 2.0
        |			}]
        |		}
        |	}
        |}""".stripMargin

    val request = FakeRequest().withBody(Json.parse(payload).as[JsObject])

    when(callbackObjects.receiptRepository.fetchByReceiptId("4298-90a8-97ee920abaaf")) thenReturn Future.successful(Some(receipt))
    when(callbackObjects.userProfileRepository.findByEmail("test@example.com")) thenReturn Future.successful(Some(userProfile))
    when(callbackObjects.receiptRepository.shouldSubmit(1)) thenReturn Future(true)
    when(callbackObjects.receiptRepository.update(receiptCallBack, serializedProductList)) thenReturn Future.successful(Some(receipt))

    when(callbackObjects.sendGridService.sendEmailForApproval("test@example.com", "firstname_test", "http://localhost/user/userId/unsubscribe/email")) thenReturn None

    val response = callbackObjects.callbackController.receive()(request)

    status(response) must be equalTo INTERNAL_SERVER_ERROR
  }

  "return error message if receipt could process for valid json" in {
    val callbackObjects = testObjects

    val dateTime = new DateTime(System.currentTimeMillis())
    val dob = "%s-%s-%s" format(1991, 9, 8)

    val receipt = Receipt(1, "receiptId", 2, 15, 2, 2017, "test@example.com", None, "userId", "clientId", "image",
      None, Some(2000), Some(1000), dateTime, 123456789, "dateInReceipt", "product")
    val receiptCallBack = ReceiptCallback(Some("approved"), None, Some("1469444737194"), "4298-90a8-97ee920abaaf",
      Some("SOME RANDOM TEXT"), Data("target", "07/23/2016", 12.329999923706055, 6.0, List(Products("14800002737", 3, Some("test"), 2.0))))

    val userProfile = Profile(1, "firstname_test", "lastname_test", "test@example.com",
      new DateTime(System.currentTimeMillis()), new DateTime(System.currentTimeMillis()), false)
    val serializedProductList = """[{"upcCode":"14800002737","quantity":3,"description":"test","productAmount":2}]"""

    val payload =
      """{
        |	"behaviorId.jpg": {
        |		"status": "approved",
        |		"submissionDate": "1469444737194",
        |		"UUID": "4298-90a8-97ee920abaaf",
        |		"rawData": "SOME RANDOM TEXT",
        |		"data": {
        |			"store": "target",
        |			"date": "07/23/2016",
        |			"amount": 12.329999923706055,
        |			"qualifyingAmount": 6.0,
        |			"products": [{
        |				"upcCode": "14800002737",
        |				"quantity": 3,
        |				"description": "test",
        |				"productAmount": 2.0
        |			}]
        |		}
        |	}
        |}""".stripMargin

    val request = FakeRequest().withBody(Json.parse(payload).as[JsObject])

    when(callbackObjects.receiptRepository.fetchByReceiptId("4298-90a8-97ee920abaaf")) thenReturn Future.successful(Some(receipt))
    when(callbackObjects.userProfileRepository.findByEmail("test@example.com")) thenReturn Future.successful(Some(userProfile))
    when(callbackObjects.receiptRepository.shouldSubmit(1)) thenReturn Future(true)
    when(callbackObjects.receiptRepository.update(receiptCallBack, serializedProductList)) thenReturn Future.successful(None)

    val response = callbackObjects.callbackController.receive()(request)

    status(response) must be equalTo INTERNAL_SERVER_ERROR
  }

  "return error message when receipt approved more than once" in {
    val callbackObjects = testObjects

    val dateTime = new DateTime(System.currentTimeMillis())
    val dob = "%s-%s-%s" format(1991, 9, 8)

    val receipt = Receipt(1, "receiptId", 2, 15, 2, 2017, "test@example.com", None, "userId", "clientId", "image",
      None, Some(2000), Some(1000), dateTime, 123456789, "dateInReceipt", "product", true)

    val userProfile = Profile(1, "firstname_test", "lastname_test", "test@example.com",
      new DateTime(System.currentTimeMillis()), new DateTime(System.currentTimeMillis()), false)

    val payload =
      """{
        |	"behaviorId.jpg": {
        |		"status": "approved",
        |		"submissionDate": "1469444737194",
        |		"UUID": "4298-90a8-97ee920abaaf",
        |		"rawData": "SOME RANDOM TEXT",
        |		"data": {
        |			"store": "target",
        |			"date": "07/23/2016",
        |			"amount": 12.329999923706055,
        |			"qualifyingAmount": 6.0,
        |			"products": [{
        |				"upcCode": "14800002737",
        |				"quantity": 3,
        |				"description": "test",
        |				"productAmount": 2.0
        |			}]
        |		}
        |	}
        |}""".stripMargin

    val request = FakeRequest().withBody(Json.parse(payload).as[JsObject])

    when(callbackObjects.receiptRepository.fetchByReceiptId("4298-90a8-97ee920abaaf")) thenReturn Future.successful(Some(receipt))
    when(callbackObjects.userProfileRepository.findByEmail("test@example.com")) thenReturn Future.successful(Some(userProfile))

    val response = callbackObjects.callbackController.receive()(request)

    status(response) must be equalTo NOT_ACCEPTABLE
  }

  "return error message when receipt could not find by receipt id" in {
    val callbackObjects = testObjects
    val payload =
      """{
        |	"behaviorId.jpg": {
        |		"status": "approved",
        |		"submissionDate": "1469444737194",
        |		"UUID": "4298-90a8-97ee920abaaf",
        |		"rawData": "SOME RANDOM TEXT",
        |		"data": {
        |			"store": "target",
        |			"date": "07/23/2016",
        |			"amount": 12.329999923706055,
        |			"qualifyingAmount": 6.0,
        |			"products": [{
        |				"upcCode": "14800002737",
        |				"quantity": 3,
        |				"description": "test",
        |				"productAmount": 2.0
        |			}]
        |		}
        |	}
        |}""".stripMargin

    val request = FakeRequest().withBody(Json.parse(payload).as[JsObject])
    val expectedResponse = """{"message":"Receipt with the given UUID doesn't exist"}"""
    when(callbackObjects.receiptRepository.fetchByReceiptId("4298-90a8-97ee920abaaf")) thenReturn Future.successful(None)

    val response = callbackObjects.callbackController.receive()(request)

    status(response) must be equalTo NOT_FOUND
    contentAsString(response) mustEqual expectedResponse
  }

  "return error message when user profile could not find by email" in {
    val callbackObjects = testObjects

    val dateTime = new DateTime(System.currentTimeMillis())
    val receipt = Receipt(1, "receiptId", 2, 15, 2, 2017, "test@example.com", None, "userId", "clientId", "image",
      None, Some(2000), Some(1000), dateTime, 123456789, "dateInReceipt", "product")

    val payload =
      """{
        |	"behaviorId.jpg": {
        |		"status": "approved",
        |		"submissionDate": "1469444737194",
        |		"UUID": "4298-90a8-97ee920abaaf",
        |		"rawData": "SOME RANDOM TEXT",
        |		"data": {
        |			"store": "target",
        |			"date": "07/23/2016",
        |			"amount": 12.329999923706055,
        |			"qualifyingAmount": 6.0,
        |			"products": [{
        |				"upcCode": "14800002737",
        |				"quantity": 3,
        |				"description": "test",
        |				"productAmount": 2.0
        |			}]
        |		}
        |	}
        |}""".stripMargin

    val request = FakeRequest().withBody(Json.parse(payload).as[JsObject])
    val expectedResponse = """{"message":"User profile not found"}"""

    when(callbackObjects.receiptRepository.fetchByReceiptId("4298-90a8-97ee920abaaf")) thenReturn Future.successful(Some(receipt))
    when(callbackObjects.userProfileRepository.findByEmail("test@example.com")) thenReturn Future.successful(None)

    val response = callbackObjects.callbackController.receive()(request)

    status(response) must be equalTo NOT_FOUND
    contentAsString(response) mustEqual expectedResponse
  }

  "return message when receipt is ambiguous" in {
    val callbackObjects = testObjects

    val dateTime = new DateTime(System.currentTimeMillis())
    val dob = "%s-%s-%s" format(1991, 9, 8)

    val receipt = Receipt(1, "receiptId", 2, 15, 2, 2017, "test@example.com", None, "userId", "clientId", "image",
      None, Some(2000), Some(1000), dateTime, 123456789, "dateInReceipt", "product")
    val receiptCallBack = ReceiptCallback(Some("ambiguous"), None, Some("1469444737194"), "4298-90a8-97ee920abaaf",
      Some("SOME RANDOM TEXT"), Data("target", "07/23/2016", 12.329999923706055, 6.0, List(Products("14800002737", 3, Some("test"), 2.0))))

    val userProfile = Profile(1, "firstname_test", "lastname_test", "test@example.com",
      new DateTime(System.currentTimeMillis()), new DateTime(System.currentTimeMillis()), false)

    val payload =
      """{
        |	"behaviorId.jpg": {
        |		"status": "ambiguous",
        |		"submissionDate": "1469444737194",
        |		"UUID": "4298-90a8-97ee920abaaf",
        |		"rawData": "SOME RANDOM TEXT",
        |		"data": {
        |			"store": "target",
        |			"date": "07/23/2016",
        |			"amount": 12.329999923706055,
        |			"qualifyingAmount": 6.0,
        |			"products": [{
        |				"upcCode": "14800002737",
        |				"quantity": 3,
        |				"description": "test",
        |				"productAmount": 2.0
        |			}]
        |		}
        |	}
        |}""".stripMargin

    val request = FakeRequest().withBody(Json.parse(payload).as[JsObject])
    val expectedResponse = """Ambiguous receipt could not processed"""

    when(callbackObjects.receiptRepository.fetchByReceiptId("4298-90a8-97ee920abaaf")) thenReturn Future.successful(Some(receipt))
    when(callbackObjects.userProfileRepository.findByEmail("test@example.com")) thenReturn Future.successful(Some(userProfile))
    when(callbackObjects.receiptRepository.updateRejectedEmail(receiptCallBack, "serializedProductList")) thenReturn Future.successful(true)

    val response = callbackObjects.callbackController.receive()(request)

    status(response) must be equalTo NOT_ACCEPTABLE
    contentAsString(response) mustEqual expectedResponse
  }

  "return message when receipt is rejected" in {
    val callbackObjects = testObjects

    val dateTime = new DateTime(System.currentTimeMillis())
    val dob = "%s-%s-%s" format(1991, 9, 8)

    val receipt = Receipt(1, "receiptId", 2, 15, 2, 2017, "test@example.com", None, "userId", "clientId", "image",
      None, Some(2000), Some(1000), dateTime, 123456789, "dateInReceipt", "product")
    val receiptCallBack = ReceiptCallback(Some("rejected"), None, Some("1469444737194"), "4298-90a8-97ee920abaaf",
      Some("SOME RANDOM TEXT"), Data("target", "07/23/2016", 12.329999923706055, 6.0, List(Products("14800002737", 3, Some("test"), 2.0))))

    val userProfile = Profile(1, "firstname_test", "lastname_test", "test@example.com",
      new DateTime(System.currentTimeMillis()), new DateTime(System.currentTimeMillis()), false)

    val payload =
      """{
        |	"behaviorId.jpg": {
        |		"status": "rejected",
        |		"submissionDate": "1469444737194",
        |		"UUID": "4298-90a8-97ee920abaaf",
        |		"rawData": "SOME RANDOM TEXT",
        |		"data": {
        |			"store": "target",
        |			"date": "07/23/2016",
        |			"amount": 12.329999923706055,
        |			"qualifyingAmount": 6.0,
        |			"products": [{
        |				"upcCode": "14800002737",
        |				"quantity": 3,
        |				"description": "test",
        |				"productAmount": 2.0
        |			}]
        |		}
        |	}
        |}""".stripMargin

    val request = FakeRequest().withBody(Json.parse(payload).as[JsObject])

    when(callbackObjects.receiptRepository.fetchByReceiptId("4298-90a8-97ee920abaaf")) thenReturn Future.successful(Some(receipt))
    when(callbackObjects.userProfileRepository.findByEmail("test@example.com")) thenReturn Future.successful(Some(userProfile))
    when(callbackObjects.receiptRepository.updateRejectedEmail(receiptCallBack, "serializedProductList")) thenReturn Future.successful(true)
    when(callbackObjects.sendGridService.sendEmailForRejection("test@example.com", "firstname_test", "http://localhost/user/userId/unsubscribe/email")) thenReturn Some("success")

    val response = callbackObjects.callbackController.receive()(request)
    status(response) must be equalTo OK
    contentAsString(response) mustEqual "Email sent"
  }

  "return message when could not send email for rejected receipt" in {
    val callbackObjects = testObjects

    val dateTime = new DateTime(System.currentTimeMillis())
    val dob = "%s-%s-%s" format(1991, 9, 8)

    val receipt = Receipt(1, "receiptId", 2, 15, 2, 2017, "test@example.com", None, "userId", "clientId", "image",
      None, Some(2000), Some(1000), dateTime, 123456789, "dateInReceipt", "product")
    val receiptCallBack = ReceiptCallback(Some("rejected"), None, Some("1469444737194"), "4298-90a8-97ee920abaaf",
      Some("SOME RANDOM TEXT"), Data("target", "07/23/2016", 12.329999923706055, 6.0, List(Products("14800002737", 3, Some("test"), 2.0))))

    val userProfile = Profile(1, "firstname_test", "lastname_test", "test@example.com",
      new DateTime(System.currentTimeMillis()), new DateTime(System.currentTimeMillis()), false)

    val payload =
      """{
        |	"behaviorId.jpg": {
        |		"status": "rejected",
        |		"submissionDate": "1469444737194",
        |		"UUID": "4298-90a8-97ee920abaaf",
        |		"rawData": "SOME RANDOM TEXT",
        |		"data": {
        |			"store": "target",
        |			"date": "07/23/2016",
        |			"amount": 12.329999923706055,
        |			"qualifyingAmount": 6.0,
        |			"products": [{
        |				"upcCode": "14800002737",
        |				"quantity": 3,
        |				"description": "test",
        |				"productAmount": 2.0
        |			}]
        |		}
        |	}
        |}""".stripMargin

    val request = FakeRequest().withBody(Json.parse(payload).as[JsObject])

    when(callbackObjects.receiptRepository.fetchByReceiptId("4298-90a8-97ee920abaaf")) thenReturn Future.successful(Some(receipt))
    when(callbackObjects.userProfileRepository.findByEmail("test@example.com")) thenReturn Future.successful(Some(userProfile))
    when(callbackObjects.receiptRepository.updateRejectedEmail(receiptCallBack, "serializedProductList")) thenReturn Future.successful(true)
    when(callbackObjects.sendGridService.sendEmailForRejection("test@example.com", "firstname_test", "http://localhost/user/userId/unsubscribe/email")) thenReturn None

    val response = callbackObjects.callbackController.receive()(request)
    status(response) must be equalTo INTERNAL_SERVER_ERROR
  }

  "return message when receipt is invalid" in {
    val callbackObjects = testObjects

    val dateTime = new DateTime(System.currentTimeMillis())
    val dob = "%s-%s-%s" format(1991, 9, 8)

    val receipt = Receipt(1, "receiptId", 2, 15, 2, 2017, "test@example.com", None, "userId", "clientId", "image",
      None, Some(2000), Some(1000), dateTime, 123456789, "dateInReceipt", "product")
    val receiptCallBack = ReceiptCallback(Some("invalid"), None, Some("1469444737194"), "4298-90a8-97ee920abaaf",
      Some("SOME RANDOM TEXT"), Data("target", "07/23/2016", 12.329999923706055, 6.0, List(Products("14800002737", 3, Some("test"), 2.0))))

    val userProfile = Profile(1, "firstname_test", "lastname_test", "test@example.com",
      new DateTime(System.currentTimeMillis()), new DateTime(System.currentTimeMillis()), false)
    val payload =
      """{
        |	"behaviorId.jpg": {
        |		"status": "invalid",
        |		"submissionDate": "1469444737194",
        |		"UUID": "4298-90a8-97ee920abaaf",
        |		"rawData": "SOME RANDOM TEXT",
        |		"data": {
        |			"store": "target",
        |			"date": "07/23/2016",
        |			"amount": 12.329999923706055,
        |			"qualifyingAmount": 6.0,
        |			"products": [{
        |				"upcCode": "14800002737",
        |				"quantity": 3,
        |				"description": "test",
        |				"productAmount": 2.0
        |			}]
        |		}
        |	}
        |}""".stripMargin

    val request = FakeRequest().withBody(Json.parse(payload).as[JsObject])

    when(callbackObjects.receiptRepository.fetchByReceiptId("4298-90a8-97ee920abaaf")) thenReturn Future.successful(Some(receipt))
    when(callbackObjects.userProfileRepository.findByEmail("test@example.com")) thenReturn Future.successful(Some(userProfile))
    when(callbackObjects.receiptRepository.updateRejectedEmail(receiptCallBack, "serializedProductList")) thenReturn Future.successful(true)
    when(callbackObjects.sendGridService.sendEmailForRejection("test@example.com", "firstname_test", "http://localhost/user/userId/unsubscribe/email")) thenReturn Some("success")

    val response = callbackObjects.callbackController.receive()(request)

    status(response) must be equalTo OK
    contentAsString(response) mustEqual "Email sent"
  }

  "return message when receipt is duplicate" in {
    val callbackObjects = testObjects

    val dateTime = new DateTime(System.currentTimeMillis())
    val dob = "%s-%s-%s" format(1991, 9, 8)

    val receipt = Receipt(1, "receiptId", 2, 15, 2, 2017, "test@example.com", None, "userId", "clientId", "image",
      None, Some(2000), Some(1000), dateTime, 123456789, "dateInReceipt", "product")
    val receiptCallBack = ReceiptCallback(Some("duplicate"), None, Some("1469444737194"), "4298-90a8-97ee920abaaf",
      Some("SOME RANDOM TEXT"), Data("target", "07/23/2016", 12.329999923706055, 6.0, List(Products("14800002737", 3, Some("test"), 2.0))))

    val userProfile = Profile(1, "firstname_test", "lastname_test", "test@example.com",
      new DateTime(System.currentTimeMillis()), new DateTime(System.currentTimeMillis()), false)
    val payload =
      """{
        |	"behaviorId.jpg": {
        |		"status": "duplicate",
        |		"submissionDate": "1469444737194",
        |		"UUID": "4298-90a8-97ee920abaaf",
        |		"rawData": "SOME RANDOM TEXT",
        |		"data": {
        |			"store": "target",
        |			"date": "07/23/2016",
        |			"amount": 12.329999923706055,
        |			"qualifyingAmount": 6.0,
        |			"products": [{
        |				"upcCode": "14800002737",
        |				"quantity": 3,
        |				"description": "test",
        |				"productAmount": 2.0
        |			}]
        |		}
        |	}
        |}""".stripMargin

    val request = FakeRequest().withBody(Json.parse(payload).as[JsObject])

    when(callbackObjects.receiptRepository.fetchByReceiptId("4298-90a8-97ee920abaaf")) thenReturn Future.successful(Some(receipt))
    when(callbackObjects.userProfileRepository.findByEmail("test@example.com")) thenReturn Future.successful(Some(userProfile))
    when(callbackObjects.receiptRepository.updateRejectedEmail(receiptCallBack, "serializedProductList")) thenReturn Future.successful(true)
    when(callbackObjects.sendGridService.sendEmailForRejection("test@example.com", "firstname_test", "http://localhost/user/userId/unsubscribe/email")) thenReturn Some("success")

    val response = callbackObjects.callbackController.receive()(request)

    status(response) must be equalTo OK
    contentAsString(response) mustEqual "Email sent"
  }

  private def testObjects: TestObjects = {
    val mockedUserProfileRepository = mock[UserProfileRepository]
    val mockedReceiptRepository = mock[ReceiptRepository]
    val mockedWsClient = mock[WSClient]
    val mockedSendGridService = mock[SendGridService]
    val mockedConfig = mock[ConfigLoader]

    val controller = new Callbacks(stubControllerComponents(), mockedReceiptRepository, mockedUserProfileRepository,
      mockedConfig, mockedWsClient, mockedSendGridService)

    TestObjects(stubControllerComponents(), mockedReceiptRepository, mockedUserProfileRepository,
      mockedConfig, mockedWsClient,
      mockedSendGridService, controller)
  }

  case class TestObjects(controllerComponent: ControllerComponents,
                         receiptRepository: ReceiptRepository,
                         userProfileRepository: UserProfileRepository,
                         config: ConfigLoader,
                         wsClient: WSClient,
                         sendGridService: SendGridService,
                         callbackController: Callbacks)

}
