package controllers

import forms._
import org.mockito.Mockito.when
import org.scalatestplus.play._
import org.specs2.mock.Mockito
import play.api.i18n._
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ApplicationTest extends PlaySpec with Mockito {
  val userForm = mock[UserForm]

  "Application Controller" should {
    import play.api.i18n._

    implicit val lang = Lang("en-US")

    "render register page" in {
      val controller = getMockedObject

      val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"), "recaptcha", isAgree = true)

      when(controller.lang.availables) thenReturn Seq(Lang("en-US"))

      val userForm = new UserForm(controller.lang, controller.messagesApi) {}.signUpForm.fill(user)

      when(controller.user.signUpForm) thenReturn userForm

      val result = controller.applicationController.register(FakeRequest().withCSRFToken)
      status(result) must equal(OK)
    }

    "render login page" in {
      val controller = getMockedObject

      when(controller.lang.availables) thenReturn Seq(Lang("en-US"))

      val loginForm = new UserForm(controller.lang, controller.messagesApi) {}.loginForm.fill("test@example.com")

      when(controller.user.loginForm) thenReturn loginForm

      val result = controller.applicationController.login(FakeRequest().withCSRFToken)
      status(result) must equal(OK)
    }

    "render support page" in {
      val controller = getMockedObject

      when(controller.lang.availables) thenReturn Seq(Lang("en-US"))

      val support = SupportForm("name", "test@example.com", "message")

      val supportForm = new UserForm(controller.lang, controller.messagesApi) {}.userSupportForm.fill(support)

      when(controller.user.userSupportForm) thenReturn supportForm

      val result = controller.applicationController.login(FakeRequest().withCSRFToken)
      status(result) must equal(OK)
    }

    "render upload page" in {
      val controller = getMockedObject

      when(controller.lang.availables) thenReturn Seq(Lang("en-US"))

      val result = controller.applicationController.upload(FakeRequest().withCSRFToken)
      status(result) must equal(SEE_OTHER)
    }
  }

  def getMockedObject: TestObjects = {
    val mockedUserForm = mock[UserForm]
    val mockedMessagesApi = mock[MessagesApi]
    val mockedLang = mock[Langs]

    val applicationController = new Application(stubControllerComponents(), mockedUserForm)

    TestObjects(mockedUserForm, mockedMessagesApi, mockedLang, applicationController)
  }

  case class TestObjects(user: UserForm,
                         messagesApi: MessagesApi,
                         lang: Langs,
                         applicationController: Application)

}
