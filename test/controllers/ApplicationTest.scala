package controllers

import java.util.UUID

import actors.RewardActor
import akka.actor.{ActorSystem, Props}
import forms._
import models.RewardRepository
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

    "render index page" in {
      val controller = getMockedObject
      val supportData = SupportForm("test", "test@example.com", "message query")

      when(controller.lang.availables) thenReturn Seq(Lang("en-US"))

      val supportForm = new UserForm(controller.lang, controller.messagesApi) {}.userSupportForm.fill(supportData)

      when(controller.user.userSupportForm) thenReturn supportForm
      when(controller.messagesApi("home.page.title")) thenReturn "home.page.title"

      val result = controller.applicationController.index(FakeRequest().withCSRFToken)
      status(result) must equal(OK)
    }

    "render faq1" in {
      val controller = getMockedObject

      val result = controller.applicationController.faq1(FakeRequest().withCSRFToken)
      status(result) must equal(OK)
    }

    "render faq2" in {
      val controller = getMockedObject

      val result = controller.applicationController.faq2(FakeRequest().withCSRFToken)
      status(result) must equal(OK)
    }

    "render faq3" in {
      val controller = getMockedObject

      val result = controller.applicationController.faq3(FakeRequest().withCSRFToken)
      status(result) must equal(OK)
    }

    "render faq4" in {
      val controller = getMockedObject

      val result = controller.applicationController.faq4(FakeRequest().withCSRFToken)
      status(result) must equal(OK)
    }

    "render terms modal" in {
      val controller = getMockedObject

      val result = controller.applicationController.termsModal(FakeRequest().withCSRFToken)
      status(result) must equal(OK)
    }

    "render register page" in {
      val controller = getMockedObject

      val user = User(Email("test@example.com", "test@example.com"), "test", "last", DateOfBirth("8", "9", "1991"),
        "address1", Some("city"), "V1V 1V1", "1234567891", "province",  "recaptcha", isAgree = true)

      when(controller.lang.availables) thenReturn Seq(Lang("en-US"))

      val userForm = new UserForm(controller.lang, controller.messagesApi) {}.signUpForm.fill(user)

      when(controller.user.signUpForm) thenReturn userForm

      val result = controller.applicationController.register(FakeRequest().withCSRFToken)
      status(result) must equal(OK)
    }

    "render upload page" in {
      val controller = getMockedObject

      val email = "test@example.com"

      when(controller.lang.availables) thenReturn Seq(Lang("en-US"))

      val result = controller.applicationController.upload(FakeRequest().withCSRFToken)
      status(result) must equal(SEE_OTHER)
    }
  }

  def getMockedObject: TestObjects = {
    val codes = Set("code1", "code2", "code3", "code4")
    val mockedUserForm = mock[UserForm]
    val mockedMessagesApi = mock[MessagesApi]
    val mockedLang = mock[Langs]
    val actorSystem = ActorSystem("RewardTestSystem")
    val mockedRewardActor = mock[RewardActor]
    val mockedRewardRepository = mock[RewardRepository]
    when(mockedRewardRepository.getUnusedCode).thenReturn(codes)
    val rewardActor = actorSystem.actorOf(Props(classOf[RewardActor], mockedRewardRepository), s"reward-test-actor-${UUID.randomUUID}")

    val applicationController = new Application(stubControllerComponents(), mockedUserForm, rewardActor)

    TestObjects(mockedUserForm, mockedMessagesApi, mockedLang, applicationController)
  }

  case class TestObjects(user: UserForm,
                         messagesApi: MessagesApi,
                         lang: Langs,
                         applicationController: Application)

}
