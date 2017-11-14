import actors.RewardActor
import com.google.inject.AbstractModule
import com.sendgrid.SendGrid
import controllers.ErrorHandler
import net.codingwell.scalaguice.ScalaModule
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.libs.json.{JodaReads, JodaWrites}

class Module extends AbstractModule with ScalaModule with AkkaGuiceSupport{
  val sendGrid = new SendGrid("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")

  override def configure(): Unit = {
    bind[SendGrid].toInstance(sendGrid)
    bind[JodaReads].toInstance(JodaReads)
    bind[JodaWrites].toInstance(JodaWrites)
    bind[ErrorHandler].asEagerSingleton()
    bindActor[RewardActor]("reward-codes-actor")
  }
}
