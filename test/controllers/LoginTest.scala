package controllers

import java.sql.Timestamp

import forms.{DateOfBirth, Email, User, UserForm}
import models._
import org.joda.time.DateTime
import org.mockito.Mockito.when
import org.scalatestplus.play.PlaySpec
import org.specs2.mock.Mockito
import play.api.mvc.ControllerComponents
import play.api.test.CSRFTokenHelper._
import play.api.test.Helpers._
import play.api.test._
import services.EventSenderService
import utilities.Util

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LoginTest extends PlaySpec with Mockito {

  import play.api.i18n._

  implicit val lang = Lang("en-US")

  val date = Util.getPacificTime
  val time = new Timestamp(System.currentTimeMillis)

  val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"), "recaptcha", isAgree = true)

  val dateInString = "%s-%s-%s" format(user.dob.birthYear, user.dob.birthMonth, user.dob.birthDay)
  val dob = DateTime.parse(dateInString)

  "should not login user with malformed email" in {
    val controller = getMockedObject

    val loginForm = new UserForm(controller.controllerComponent.langs,
      controller.loginController.messagesApi) {}.loginForm.fill("test@example.com")

    val request = FakeRequest(POST, "/login").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "email" -> "")
      .withCSRFToken

    when(controller.userForm.loginForm) thenReturn loginForm
    val result = controller.loginController.login(request)

    status(result) must equal(BAD_REQUEST)
  }

  "should not login user if no local user found" in {
    val controller = getMockedObject

    val loginForm = new UserForm(controller.controllerComponent.langs,
      controller.loginController.messagesApi) {}.loginForm.fill("test@example.com")

    val request = FakeRequest(POST, "/login").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "email" -> "test@example.com")
      .withCSRFToken

    when(controller.userForm.loginForm) thenReturn loginForm
    when(controller.userProfileRepository.findByEmail("test@example.com")) thenReturn Future(None)

    val result = controller.loginController.login(request)

    status(result) must equal(OK)
  }

  "should not login user if user email is blocked" in {
    val controller = getMockedObject
    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, date, suspended = false)

    val loginForm = new UserForm(controller.controllerComponent.langs,
      controller.loginController.messagesApi) {}.loginForm.fill("test@example.com")

    val request = FakeRequest(POST, "/login").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "email" -> "test@example.com")
      .withCSRFToken

    when(controller.userForm.loginForm) thenReturn loginForm
    when(controller.userProfileRepository.findByEmail("test@example.com")) thenReturn Future(Some(profile))
    when(controller.blockListRepository.shouldEnter("test@example.com")) thenReturn Future(false)

    val result = controller.loginController.login(request)

    status(result) must equal(OK)
  }

  "should not login user if P3 user info not found" in {
    val controller = getMockedObject
    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, date, suspended = false)

    val loginForm = new UserForm(controller.controllerComponent.langs,
      controller.loginController.messagesApi) {}.loginForm.fill("test@example.com")

    val request = FakeRequest(POST, "/login").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "email" -> "test@example.com")
      .withCSRFToken

    when(controller.userForm.loginForm) thenReturn loginForm
    when(controller.userProfileRepository.findByEmail("test@example.com")) thenReturn Future(Some(profile))
    when(controller.blockListRepository.shouldEnter("test@example.com")) thenReturn Future(true)
    when(controller.eventRepository.store(any[Event])) thenReturn Future(true)
    when(controller.p3UserInfoRepository.fetchByEmail("test@example.com")) thenReturn Future(None)

    val result = controller.loginController.login(request)

    status(result) must equal(OK)
  }

  "should login user with warning as already uploaded receipt for today" in {
    val controller = getMockedObject
    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, date, suspended = false)

    val loginForm = new UserForm(controller.controllerComponent.langs,
      controller.loginController.messagesApi) {}.loginForm.fill("test@example.com")

    val request = FakeRequest(POST, "/login").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "email" -> "test@example.com")
      .withCSRFToken

    when(controller.userForm.loginForm) thenReturn loginForm
    when(controller.userProfileRepository.findByEmail("test@example.com")) thenReturn Future(Some(profile))
    when(controller.blockListRepository.shouldEnter("test@example.com")) thenReturn Future(true)
    when(controller.eventRepository.store(any[Event])) thenReturn Future(true)
    when(controller.p3UserInfoRepository.fetchByEmail("test@example.com")) thenReturn
      Future(Some(P3UserInfo(123456L, "p3User_Id", profile.email, DateTime.now)))
    when(controller.eventSender.insertLoginEvent("p3User_Id", user.firstName + " " + user.lastName)) thenReturn true
    when(controller.receiptRepository.shouldSubmit(0)) thenReturn Future(false)

    val result = controller.loginController.login(request)

    status(result) must equal(SEE_OTHER)
  }

  "should login user" in {
    val controller = getMockedObject
    val profile = Profile(0, user.firstName, user.lastName, user.email.email, dob, date, suspended = false)

    val loginForm = new UserForm(controller.controllerComponent.langs,
      controller.loginController.messagesApi) {}.loginForm.fill("test@example.com")

    val request = FakeRequest(POST, "/login").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "email" -> "test@example.com")
      .withCSRFToken

    when(controller.userForm.loginForm) thenReturn loginForm
    when(controller.userProfileRepository.findByEmail("test@example.com")) thenReturn Future(Some(profile))
    when(controller.blockListRepository.shouldEnter("test@example.com")) thenReturn Future(true)
    when(controller.eventRepository.store(any[Event])) thenReturn Future(true)
    when(controller.p3UserInfoRepository.fetchByEmail("test@example.com")) thenReturn
      Future(Some(P3UserInfo(123456L, "p3User_Id", profile.email, DateTime.now)))
    when(controller.eventSender.insertLoginEvent("p3User_Id", user.firstName + " " + user.lastName)) thenReturn true
    when(controller.receiptRepository.shouldSubmit(0)) thenReturn Future(true)

    val result = controller.loginController.login(request)

    status(result) must equal(SEE_OTHER)
  }

  def getMockedObject: TestObjects = {
    val mockedBlockListRepository = mock[BlockListRepository]
    val mockedUserProfileRepository = mock[UserProfileRepository]
    val mockedEventRepository = mock[EventRepository]
    val mockedP3UserInfoRepository = mock[P3UsersInfoRepository]
    val mockedEventSender = mock[EventSenderService]
    val mockedUserForm = mock[UserForm]
    val mockedReceiptRepository = mock[ReceiptRepository]

    val controller = new Login(stubControllerComponents(), mockedUserForm, mockedUserProfileRepository, mockedBlockListRepository,
      mockedEventRepository, mockedP3UserInfoRepository, mockedEventSender, mockedReceiptRepository)

    TestObjects(stubControllerComponents(), mockedUserForm, mockedBlockListRepository, mockedUserProfileRepository,
      mockedEventRepository, mockedP3UserInfoRepository, mockedEventSender, mockedReceiptRepository, controller)
  }

  case class TestObjects(controllerComponent: ControllerComponents,
                         userForm: UserForm,
                         blockListRepository: BlockListRepository,
                         userProfileRepository: UserProfileRepository,
                         eventRepository: EventRepository,
                         p3UserInfoRepository: P3UsersInfoRepository,
                         eventSender: EventSenderService,
                         receiptRepository: ReceiptRepository,
                         loginController: Login)
}
