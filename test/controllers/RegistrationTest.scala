package controllers

import java.sql.Timestamp
import java.util.UUID

import actors.RewardActor
import akka.actor.{ActorSystem, Props}
import forms.{DateOfBirth, Email, User, UserForm}
import models.{RewardRepository, _}
import org.joda.time.DateTime
import org.mockito.Mockito.when
import org.scalatestplus.play.PlaySpec
import org.specs2.mock.Mockito
import play.api.mvc.ControllerComponents
import play.api.test.CSRFTokenHelper._
import play.api.test.Helpers._
import play.api.test._
import services.{EventSenderService, GoogleReCaptchaService, PlatformBridgeService, SendGridService}
import utilities.Util

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationTest extends PlaySpec with Mockito {

  import play.api.i18n._

  implicit val lang = Lang("en-US")

  val date = Util.getPacificTime
  val time = new Timestamp(System.currentTimeMillis)

  "should login the user when user already registered and submitted the receipt today" in {
    val controller = getMockedObject

    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "NY",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val remoteIp = request.remoteAddress
    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, user.address1, user.city,
      user.province, user.postalCode, user.phoneNumber, date, suspended = false)

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future(true)
    when(controller.userProfileRepository.findByEmail(user.email.email)) thenReturn Future(Some(profile))
    when(controller.p3UserInfoRepository.fetchByEmail(profile.email)) thenReturn
      Future(Some(P3UserInfo(123456L, "p3User_Id", profile.email, DateTime.now)))
    when(controller.eventSender.insertLoginEvent("p3User_Id", profile.firstName + " " + profile.lastName)) thenReturn true
    when(controller.eventRepository.store(any[Event])) thenReturn Future(true)
    when(controller.rewardRepository.isRewarded(0)) thenReturn Future.successful(true)

    val result = controller.registrationController.saveContestant(request)
    status(result) must equal(SEE_OTHER)
  }

  "should login the user when user already registered when user has not submitted the receipt today" in {
    val controller = getMockedObject

    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "NY",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken
    val remoteIp = request.remoteAddress

    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
      "1245678903", date, suspended = false)

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future(true)
    when(controller.userProfileRepository.findByEmail(user.email.email)) thenReturn Future(Some(profile))
    when(controller.p3UserInfoRepository.fetchByEmail(profile.email)) thenReturn
      Future(Some(P3UserInfo(123456L, "p3User_Id", profile.email, DateTime.now)))
    when(controller.eventSender.insertLoginEvent("p3User_Id", profile.firstName + " " + profile.lastName)) thenReturn true
    when(controller.eventRepository.store(any[Event])) thenReturn Future(true)
    when(controller.messagesApi("receipt.uploaded.message")) thenReturn "You have already submitted a receipt today, please try again tomorrow"
    when(controller.eventRepository.store(any[Event])) thenReturn Future(true)
    when(controller.rewardRepository.isRewarded(0)) thenReturn Future.successful(false)

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(SEE_OTHER)
  }


  "should not login the user when user already registered" in {
    val controller = getMockedObject

    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "NY",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val remoteIp = request.remoteAddress
    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
      "1245678903", date, suspended = false)

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future(true)
    when(controller.userProfileRepository.findByEmail(user.email.email)) thenReturn Future(Some(profile))
    when(controller.p3UserInfoRepository.fetchByEmail(profile.email)) thenReturn Future(None)
    when(controller.messagesApi("common.error.message")) thenReturn "Something went wrong, please try again"

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(SEE_OTHER)
  }

  "should be able to register a contestant from register with purchase state" in {
    val controller = getMockedObject

    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "NY",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
      "1245678903", date, suspended = false)
    val remoteIp = request.remoteAddress

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future.successful(true)
    when(controller.userProfileRepository.findByEmail(user.email.email)) thenReturn Future.successful(None)
    when(controller.platformBridgeService.checkUserExistsByEmail(user.email.email)) thenReturn None
    when(controller.platformBridgeService.createUser(any[Profile])) thenReturn Future.successful(Some("user_Id"))
    when(controller.platformBridgeService.createProfile(any[String], any[Profile])) thenReturn Some("user_Id")
    when(controller.userProfileRepository.store(any[Profile])) thenReturn Future.successful(Some(123456L))
    when(controller.associateRepository.store(Associate(0, 123456L, "user_Id", None, None, Some("userpass")))) thenReturn Future.successful(true)
    when(controller.eventRepository.store(any[Event])) thenReturn Future(true)
    when(controller.p3UserInfoRepository.fetchByEmail(profile.email)) thenReturn
      Future(Some(P3UserInfo(123456L, "p3User_Id", profile.email, DateTime.now)))
    when(controller.messagesApi("registration.success")) thenReturn "Thank you for registering"
    when(controller.eventSender.insertSignUpEvent("p3User_Id", profile.firstName + " " + profile.lastName)) thenReturn true

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(SEE_OTHER)
  }

  "should be able to register a contestant from register without purchase state" in {
    val controller = getMockedObject

    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "AL",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
      "1245678903", date, suspended = false)
    val remoteIp = request.remoteAddress

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future.successful(true)
    when(controller.userProfileRepository.findByEmail(user.email.email)) thenReturn Future.successful(None)
    when(controller.platformBridgeService.checkUserExistsByEmail(user.email.email)) thenReturn None
    when(controller.platformBridgeService.createUser(any[Profile])) thenReturn Future.successful(Some("user_Id"))
    when(controller.platformBridgeService.createProfile(any[String], any[Profile])) thenReturn Some("user_Id")
    when(controller.userProfileRepository.store(any[Profile])) thenReturn Future.successful(Some(123456L))
    when(controller.associateRepository.store(Associate(0, 123456L, "user_Id", None, None, Some("userpass")))) thenReturn Future.successful(true)
    when(controller.eventRepository.store(any[Event])) thenReturn Future(true)
    when(controller.p3UserInfoRepository.fetchByEmail(profile.email)) thenReturn
      Future(Some(P3UserInfo(123456L, "p3User_Id", profile.email, DateTime.now)))
    when(controller.messagesApi("registration.success")) thenReturn "Thank you for registering"
    when(controller.eventSender.insertSignUpEvent("p3User_Id", profile.firstName + " " + profile.lastName)) thenReturn true
    when(controller.rewardRepository.isRewarded(123456L)) thenReturn Future.successful(false)
    when(controller.rewardRepository.markedCodeUsed("code1",123456L,None)).thenReturn(Future.successful(true))
    when(controller.sendGridService.sendEmailForRegistration("test@example.com", "test")) thenReturn Some("sent")
    when(controller.sendGridService.sendEmailForApproval("test@example.com", "test", "code1")) thenReturn Some("sent")

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(OK)
  }

  "should not register a contestant from register without purchase state who is already rewarded" in {
    val controller = getMockedObject

    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "AL",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
      "1245678903", date, suspended = false)
    val remoteIp = request.remoteAddress

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future.successful(true)
    when(controller.userProfileRepository.findByEmail(user.email.email)) thenReturn Future.successful(None)
    when(controller.platformBridgeService.checkUserExistsByEmail(user.email.email)) thenReturn None
    when(controller.platformBridgeService.createUser(any[Profile])) thenReturn Future.successful(Some("user_Id"))
    when(controller.platformBridgeService.createProfile(any[String], any[Profile])) thenReturn Some("user_Id")
    when(controller.userProfileRepository.store(any[Profile])) thenReturn Future.successful(Some(123456L))
    when(controller.associateRepository.store(Associate(0, 123456L, "user_Id", None, None, Some("userpass")))) thenReturn Future.successful(true)
    when(controller.eventRepository.store(any[Event])) thenReturn Future(true)
    when(controller.p3UserInfoRepository.fetchByEmail(profile.email)) thenReturn
      Future(Some(P3UserInfo(123456L, "p3User_Id", profile.email, DateTime.now)))
    when(controller.messagesApi("registration.success")) thenReturn "Thank you for registering"
    when(controller.eventSender.insertSignUpEvent("p3User_Id", profile.firstName + " " + profile.lastName)) thenReturn true
    when(controller.rewardRepository.isRewarded(123456L)) thenReturn Future.successful(true)

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(OK)
  }

  "should throw internal server error if email not sent" in {
    val controller = getMockedObject

    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "AL",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
      "1245678903", date, suspended = false)
    val remoteIp = request.remoteAddress

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future.successful(true)
    when(controller.userProfileRepository.findByEmail(user.email.email)) thenReturn Future.successful(None)
    when(controller.platformBridgeService.checkUserExistsByEmail(user.email.email)) thenReturn None
    when(controller.platformBridgeService.createUser(any[Profile])) thenReturn Future.successful(Some("user_Id"))
    when(controller.platformBridgeService.createProfile(any[String], any[Profile])) thenReturn Some("user_Id")
    when(controller.userProfileRepository.store(any[Profile])) thenReturn Future.successful(Some(123456L))
    when(controller.associateRepository.store(Associate(0, 123456L, "user_Id", None, None, Some("userpass")))) thenReturn Future.successful(true)
    when(controller.eventRepository.store(any[Event])) thenReturn Future(true)
    when(controller.p3UserInfoRepository.fetchByEmail(profile.email)) thenReturn
      Future(Some(P3UserInfo(123456L, "p3User_Id", profile.email, DateTime.now)))
    when(controller.messagesApi("registration.success")) thenReturn "Thank you for registering"
    when(controller.eventSender.insertSignUpEvent("p3User_Id", profile.firstName + " " + profile.lastName)) thenReturn true
    when(controller.rewardRepository.isRewarded(123456L)) thenReturn Future.successful(false)
    when(controller.rewardRepository.markedCodeUsed("code1",123456L,None)).thenReturn(Future.successful(true))
    when(controller.sendGridService.sendEmailForRegistration("test@example.com", "test")) thenReturn None

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(INTERNAL_SERVER_ERROR)
  }

  "should not able to register a contestant when user exist in p3 for the client" in {
    val controller = getMockedObject

    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "NY",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
      "1245678903", date, suspended = false)
    val remoteIp = request.remoteAddress

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future(true)
    when(controller.userProfileRepository.findByEmail(user.email.email)) thenReturn Future(None)
    when(controller.platformBridgeService.checkUserExistsByEmail(user.email.email)) thenReturn None
    when(controller.platformBridgeService.createUser(any[Profile])) thenReturn Future(Some("409"))

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(SEE_OTHER)
  }

  "should not able to register a contestant because of server issue" in {
    val controller = getMockedObject

    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "NY",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
      "1245678903", date, suspended = false)
    val remoteIp = request.remoteAddress

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future(true)
    when(controller.userProfileRepository.findByEmail(user.email.email)) thenReturn Future(None)
    when(controller.platformBridgeService.checkUserExistsByEmail(user.email.email)) thenReturn None
    when(controller.platformBridgeService.createUser(any[Profile])) thenReturn Future(None)

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(SEE_OTHER)
  }

  "should not be able to register a contestant if already there in p3 for different client" in {
    val controller = getMockedObject


    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "NY",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
      "1245678903", date, suspended = false)
    val remoteIp = request.remoteAddress
    val event = Event(0, 123456L, "signup", Some(remoteIp), date)

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future(true)
    when(controller.userProfileRepository.findByEmail(user.email.email)) thenReturn Future(None)
    when(controller.platformBridgeService.checkUserExistsByEmail(user.email.email)) thenReturn None
    when(controller.platformBridgeService.createUser(any[Profile])) thenReturn Future(None)
    when(controller.messagesApi("common.error.message")) thenReturn "Something went wrong, please try again"

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(SEE_OTHER)
  }

  "bad request with malformed data" in {
    val controller = getMockedObject


    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "UT",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
      "1245678903", date, suspended = false)
    val remoteIp = request.remoteAddress
    val event = Event(0, 123456L, "signup", Some(remoteIp), date)

    when(controller.userForm.signUpForm) thenReturn userForm

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(BAD_REQUEST)
  }

  "bad request with malformed data as not permitted state" in {
    val controller = getMockedObject

    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "UT",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    when(controller.userForm.signUpForm) thenReturn userForm

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(BAD_REQUEST)
  }


  "should not able to register a contestant for invalid captcha" in {
    val controller = getMockedObject

    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "NY",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val remoteIp = request.remoteAddress

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("invalidcaptcha", remoteIp)) thenReturn false
    when(controller.messagesApi("registration.suspicious")) thenReturn "Your actions have been deemed suspicious"

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(SEE_OTHER)
  }

  "should not able to register a blocked contestant" in {
    val controller = getMockedObject


    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "NY",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val remoteIp = request.remoteAddress

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future(false)
    when(controller.messagesApi("registration.suspicious")) thenReturn "Your actions have been deemed suspicious"

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(SEE_OTHER)
  }

  "should not able to register when user not able to store entry in p3userInfo table" in {
    val controller = getMockedObject

    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "NY",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val remoteIp = request.remoteAddress

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future(true)
    when(controller.userProfileRepository.findByEmail(user.email.email)) thenReturn Future(None)
    when(controller.platformBridgeService.checkUserExistsByEmail(user.email.email)) thenReturn Some("p3User_Id")
    when(controller.p3UserInfoRepository.store(any[P3UserInfo])) thenReturn Future(false)

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(SEE_OTHER)
  }

  "should not able to register contestant(not able to fetch details by email from p3 userInfo)" in {
    val controller = getMockedObject


    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"), "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "NY",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
      "1245678903", date, suspended = false)
    val remoteIp = request.remoteAddress
    val event = Event(0, 123456L, "signup", Some(remoteIp), date)

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future(true)
    when(controller.userProfileRepository.findByEmail(user.email.email)) thenReturn Future(None)
    when(controller.platformBridgeService.checkUserExistsByEmail(user.email.email)) thenReturn None
    when(controller.platformBridgeService.createUser(any[Profile])) thenReturn Future(Some("user_Id"))
    when(controller.platformBridgeService.createProfile(any[String], any[Profile])) thenReturn Some("user_Id")
    when(controller.userProfileRepository.store(any[Profile])) thenReturn Future(Some(123456L))
    when(controller.associateRepository.store(Associate(0, 123456L, "user_Id", None, None, Some("userpass")))) thenReturn Future.successful(true)
    when(controller.eventRepository.store(any[Event])) thenReturn Future(true)
    when(controller.p3UserInfoRepository.fetchByEmail(profile.email)) thenReturn
      Future(None)
    when(controller.messagesApi("common.error.message")) thenReturn "Something went wrong, please try again"

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(SEE_OTHER)
  }

  "should not able to register contestant(not able to store entry in associate table)" in {
    val controller = getMockedObject

    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "NY",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
      "1245678903", date, suspended = false)
    val remoteIp = request.remoteAddress
    val event = Event(0, 123456L, "signup", Some(remoteIp), date)

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future(true)
    when(controller.userProfileRepository.findByEmail(user.email.email)) thenReturn Future(None)
    when(controller.platformBridgeService.checkUserExistsByEmail(user.email.email)) thenReturn None
    when(controller.platformBridgeService.createUser(any[Profile])) thenReturn Future(Some("user_Id"))
    when(controller.platformBridgeService.createProfile(any[String], any[Profile])) thenReturn Some("user_Id")
    when(controller.userProfileRepository.store(any[Profile])) thenReturn Future(Some(123456L))
    when(controller.associateRepository.store(Associate(0, 123456L, "user_Id", None, None, Some("userpass")))) thenReturn Future.successful(false)

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(SEE_OTHER)
  }

  "should not able to register contestant(not able to store entry in userprofile table)" in {
    val controller = getMockedObject

    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "NY",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
      "1245678903", date, suspended = false)
    val remoteIp = request.remoteAddress
    val event = Event(0, 123456L, "signup", Some(remoteIp), date)

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future(true)
    when(controller.userProfileRepository.findByEmail(user.email.email)) thenReturn Future(None)
    when(controller.platformBridgeService.checkUserExistsByEmail(user.email.email)) thenReturn None
    when(controller.platformBridgeService.createUser(any[Profile])) thenReturn Future(Some("user_Id"))
    when(controller.platformBridgeService.createProfile(any[String], any[Profile])) thenReturn Some("user_Id")
    when(controller.userProfileRepository.store(any[Profile])) thenReturn Future(None)
    when(controller.messagesApi("common.error.message")) thenReturn "Something went wrong, please try again"

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(SEE_OTHER)
  }

  "should not able to register a contestant(not able to fetch profile by guid from p3) " in {
    val controller = getMockedObject

    val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
      "address1", Some("city"), "12345", "1234567891", "province",  "recaptcha", isAgree = true)
    val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
    val dob = DateTime.parse(dateInString)

    val userForm = new UserForm(controller.controllerComponent.langs,
      controller.registrationController.messagesApi) {}.signUpForm.fill(user)

    val request = FakeRequest(POST, "/savecontestent").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "emailGroup.email" -> "test@example.com",
      "emailGroup.confirmedEmail" -> "test@example.com", "firstName" -> "test", "lastName" -> "last", "dob.birthDay" -> "8",
      "dob.birthMonth" -> "9", "dob.birthYear" -> "1991", "address1" -> "address1", "city" -> "city", "province" -> "NY",
      "phoneNumber" -> "1234567891", "postalCode" -> "12345", "g-recaptcha-response" -> "recaptcha",
      "isAgree" -> "true", "isAgreeArea" -> "true")
      .withCSRFToken

    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, "address1", None, "province",  "V1V1 V1",
      "1245678903", date, suspended = false)
    val remoteIp = request.remoteAddress
    val event = Event(0, 123456L, "signup", Some(remoteIp), date)

    when(controller.userForm.signUpForm) thenReturn userForm
    when(controller.googleReCaptchaService.checkReCaptchaValidity("recaptcha", remoteIp)) thenReturn true
    when(controller.blockListRepository.shouldEnter(user.email.email)) thenReturn Future(true)
    when(controller.userProfileRepository.findByEmail(user.email.email)) thenReturn Future(None)
    when(controller.platformBridgeService.checkUserExistsByEmail(user.email.email)) thenReturn Some("p3User_Id")
    when(controller.p3UserInfoRepository.store(any[P3UserInfo])) thenReturn Future(true)
    when(controller.platformBridgeService.checkProfileExistsByGuid("p3User_Id")) thenReturn None
    when(controller.messagesApi("common.error.message")) thenReturn "Something went wrong, please try again"

    val result = controller.registrationController.saveContestant(request)

    status(result) must equal(SEE_OTHER)
  }

  def getMockedObject: TestObjects = {
    val codes = Set("code1", "code2", "code3", "code4")

    val mockedBlockListRepository = mock[BlockListRepository]
    val mockedUserProfileRepository = mock[UserProfileRepository]
    val mockedPlatformBridgeService = mock[PlatformBridgeService]
    val mockedEventRepository = mock[EventRepository]
    val mockedP3UserInfoRepository = mock[P3UsersInfoRepository]
    val mockedGoogleReCaptchaService = mock[GoogleReCaptchaService]
    val mockedEventSender = mock[EventSenderService]
    val mockedUserForm = mock[UserForm]
    val mockedSendGridService = mock[SendGridService]
    val mockedMessagesApi = mock[MessagesApi]
    val mockedReceiptRepository = mock[ReceiptRepository]
    val mockAssociateRepository = mock[AssociateRepository]
    val mockedRewardRepository = mock[RewardRepository]
    when(mockedRewardRepository.getUnusedCode).thenReturn(codes)

    val actorSystem = ActorSystem("RewardTestSystem")

    val rewardActor = actorSystem.actorOf(Props(classOf[RewardActor], mockedRewardRepository), s"reward-test-actor-${UUID.randomUUID}")

    val controller = new Registration(stubControllerComponents(), mockedUserForm, mockedBlockListRepository, mockedUserProfileRepository,
      mockedPlatformBridgeService, mockedEventRepository, mockedP3UserInfoRepository, mockedGoogleReCaptchaService,
      mockedEventSender, mockedSendGridService, mockedReceiptRepository, mockedRewardRepository, mockAssociateRepository, rewardActor)

    TestObjects(stubControllerComponents(), mockedUserForm, mockedBlockListRepository, mockedUserProfileRepository,
      mockedPlatformBridgeService, mockedEventRepository, mockedP3UserInfoRepository, mockedGoogleReCaptchaService,
      mockedEventSender, mockedSendGridService, mockedMessagesApi, mockedReceiptRepository, mockAssociateRepository, mockedRewardRepository, controller)
  }



  case class TestObjects(controllerComponent: ControllerComponents,
                         userForm: UserForm,
                         blockListRepository: BlockListRepository,
                         userProfileRepository: UserProfileRepository,
                         platformBridgeService: PlatformBridgeService,
                         eventRepository: EventRepository,
                         p3UserInfoRepository: P3UsersInfoRepository,
                         googleReCaptchaService: GoogleReCaptchaService,
                         eventSender: EventSenderService,
                         sendGridService: SendGridService,
                         messagesApi: MessagesApi,
                         receiptRepository: ReceiptRepository,
                         associateRepository: AssociateRepository,
                         rewardRepository: RewardRepository,
                         registrationController: Registration)

}
