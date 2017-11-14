package controllers

import forms.{SupportForm, UserForm}
import models.{Support => UserSupport, SupportRepository}
import org.mockito.Mockito.when
import org.scalatestplus.play.PlaySpec
import org.specs2.mock.Mockito
import play.api.mvc.ControllerComponents
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SendGridService
import utilities.Util

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SupportTest extends PlaySpec with Mockito {

  import play.api.i18n._

  implicit val lang = Lang("en-US")

  "userSupport should not be able to send support email because of malformed data" in {
    val date = Util.getPacificTime
    val supportObject = testObject

    val supportForm = SupportForm("test", "test@example.com", "user query")

    val request = FakeRequest(POST, "/customersupport")
      .withFormUrlEncodedBody("csrfToken"
        -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "name" -> "", "email" -> "test@example.com",
        "message" -> "user query").withCSRFToken


    val userSupportForm =
      new UserForm(supportObject.controllerComponent.langs, supportObject.controllerComponent.messagesApi) {}.userSupportForm.fill(supportForm)

    when(supportObject.userForm.userSupportForm) thenReturn userSupportForm

    val action = supportObject.supportController.userSupport()
    val result = action(request)

    status(result) must equal(BAD_REQUEST)
  }

  "userSupport should be able to send support email" in {
    val date = Util.getPacificTime
    val supportObject = testObject

    val support = UserSupport(0, "test", "test@example.com", "user query", date)
    val supportForm = SupportForm("test", "test@example.com", "user query")

    val request = FakeRequest(POST, "/customersupport").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "name" -> "test", "email" -> "test@example.com",
      "message" -> "user query").withCSRFToken

    val userSupportForm =
      new UserForm(supportObject.controllerComponent.langs, supportObject.controllerComponent.messagesApi) {}.userSupportForm.fill(supportForm)

    when(supportObject.userForm.userSupportForm) thenReturn userSupportForm
    when(supportObject.supportRepository.store(support)) thenReturn Future(1L)
    when(supportObject.sendGridService.sendEmailForSupport("test", "test@example.com", "user query")) thenReturn Some("success")

    val response = supportObject.supportController.userSupport(request)

    status(response) must equal(SEE_OTHER)
  }

  "userSupport should not send support email because of some error" in {
    val date = Util.getPacificTime
    val supportObject = testObject

    val support = UserSupport(0, "test", "test@example.com", "user query", date)
    val supportForm = SupportForm("test", "test@example.com", "user query")

    val request = FakeRequest(POST, "/customersupport").withFormUrlEncodedBody("csrfToken"
      -> "9c48f081724087b31fcf6099b7eaf6a276834cd9-1487743474314-cda043ddc3d791dc500e66ea", "name" -> "test", "email" -> "test@example.com",
      "message" -> "user query").withCSRFToken

    val userSupportForm =
      new UserForm(supportObject.controllerComponent.langs, supportObject.controllerComponent.messagesApi) {}.userSupportForm.fill(supportForm)

    when(supportObject.userForm.userSupportForm) thenReturn userSupportForm
    when(supportObject.supportRepository.store(support)) thenReturn Future(1L)
    when(supportObject.sendGridService.sendEmailForSupport("test", "test@example.com", "user query")) thenReturn None

    val response = supportObject.supportController.userSupport(request)

    status(response) must equal(INTERNAL_SERVER_ERROR)
    contentAsString(response) mustEqual "Email not sent"
  }

  def testObject: TestObjects = {
    val mockedUserForm = mock[UserForm]
    val mockedSupportRepository = mock[SupportRepository]
    val mockedSendGridService = mock[SendGridService]
    val mockedMessagesApi = mock[MessagesApi]

    val controller = new Support(stubControllerComponents(), mockedUserForm, mockedSupportRepository, mockedSendGridService)

    TestObjects(stubControllerComponents(), mockedUserForm, mockedSupportRepository, mockedSendGridService, mockedMessagesApi, controller)

  }

  case class TestObjects(controllerComponent: ControllerComponents,
                         userForm: UserForm,
                         supportRepository: SupportRepository,
                         sendGridService: SendGridService,
                         messagesApi: MessagesApi,
                         supportController: Support)

}
