package models

import actors.RewardActor
import com.google.inject.{AbstractModule, Module}
import com.sendgrid.SendGrid
import play.api.Application
import play.api.i18n.{Langs, MessagesApi}
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.libs.json.{JodaReads, JodaWrites}
import play.api.mvc.ControllerComponents
import play.api.test._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag

class ModelsTest[T: ClassTag] {

  def fakeApp: Application = {
    val sendGrid = new SendGrid("test")
    val stubControllerComponent =  Helpers.stubControllerComponents()
    val stubMessageApi = Helpers.stubMessagesApi()
    val stubLangs = Helpers.stubLangs()

    val testModule = Option(new AbstractModule with AkkaGuiceSupport{
      override def configure(): Unit = {
        bind(classOf[SendGrid]).toInstance(sendGrid)
        bind(classOf[MessagesApi]).toInstance(stubMessageApi)
        bind(classOf[Langs]).toInstance(stubLangs)
        bind(classOf[JodaReads]).toInstance(JodaReads)
        bind(classOf[JodaWrites]).toInstance(JodaWrites)
        bind(classOf[ControllerComponents])
          .toInstance(stubControllerComponent)
        bindActor[RewardActor]("reward-codes-actor")
      }
    })

    new GuiceApplicationBuilder()
      .overrides(testModule.map(GuiceableModule.guiceable).toSeq: _*)
      .disable[Module]
      .build
  }

  lazy val app2dao = Application.instanceCache[T]

  lazy val repository: T = app2dao(fakeApp)

  def result[R](response: Future[R]): R =
    Await.result(response, 2.seconds)
}
