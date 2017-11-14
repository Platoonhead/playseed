package services

import javax.inject.Inject

import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import utilities.ConfigLoader
import utilities.Constants._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class EventData(time: Long,
                     instanceId: Long,
                     behaviorId: String,
                     clientId: String,
                     channel: String,
                     userId: String,
                     userName: String,
                     actionType: String,
                     objectType: String,
                     p3UserId: String)

class EventSenderService @Inject()(wSClient: WSClient, config: ConfigLoader) {
  implicit val eventDataFormat = Json.format[EventData]

  def insertSignUpEvent(p3UserId: String, userName: String): Boolean = {
    val timestamp = System.currentTimeMillis
    val data = EventData(timestamp,
      timestamp,
      config.load.signUpBehaviourId,
      config.load.p3ClientId,
      SIGNUP_CHANNEL,
      p3UserId,
      userName,
      SIGNUP_ACTION_TYPE,
      SIGNUP_OBJECT_TYPE,
      p3UserId)

    insertEvent(data)
  }

  def insertLoginEvent(p3UserId: String, userName: String): Boolean = {
    val timestamp = System.currentTimeMillis
    val data = EventData(timestamp,
      timestamp,
      config.load.signInBehaviourId,
      config.load.p3ClientId,
      LOGIN_CHANNEL,
      p3UserId,
      userName,
      LOGIN_ACTION_TYPE,
      LOGIN_OBJECT_TYPE,
      p3UserId)

    insertEvent(data)
  }

  def insertUploadEvent(p3UserId: String, userName: String): Boolean = {
    val timestamp = System.currentTimeMillis
    val data = EventData(timestamp,
      timestamp,
      config.load.uploadBehaviourId,
      config.load.p3ClientId,
      RECEIPT_UPLOAD_CHANNEL,
      p3UserId,
      userName,
      RECEIPT_UPLOAD_ACTION_TYPE,
      RECEIPT_UPLOAD_OBJECT_TYPE,
      p3UserId)

    insertEvent(data)
  }

  private def insertEvent(eventData: EventData): Boolean = {
    try {
      val url = "http://%s" format config.load.p3DomainEvent
      val eventDataSeq = Seq(eventData)
      val eventJson = Json.toJson(eventDataSeq)

      val responseCodeFuture = wSClient.url(url).post(eventJson)

      val responseCode = Await.result(responseCodeFuture map (_.status), 20.seconds)

      responseCode == 200
    } catch {
      case ex: Exception =>
        Logger.error(s"Got an exception while ingesting event $eventData, ex $ex")
        false
    }
  }
}
