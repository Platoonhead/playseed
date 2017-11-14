package models

import javax.inject.Inject

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape
import slick.lifted.ProvenShape.proveShapeOf
import com.github.tototoshi.slick.MySQLJodaSupport._
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

case class Event(id: Long,
                 profileID: Long,
                 eventType: String,
                 remoteIp: Option[String],
                 invokedAt: DateTime)

class EventRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends EventBaseRepository with
  EventBaseRepositoryImpl with EventTable

// =====================================================================================================================
// Write Repository
// =====================================================================================================================
trait EventBaseRepository {
  def store(event: Event): Future[Boolean]
}

trait EventBaseRepositoryImpl extends EventBaseRepository {
  self: EventTable =>

  import profile.api._

  def store(event: Event): Future[Boolean] =
    db.run(eventQuery returning eventQuery.map(_.id) += event) map (_ > 0)
}


trait EventTable extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val eventQuery: TableQuery[Events] = TableQuery[Events]

  private[models] class Events(tag: Tag) extends Table[Event](tag, "events") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def profileID: Rep[Long] = column[Long]("profile_id")

    def eventType: Rep[String] = column[String]("event_type")

    def remoteIp: Rep[Option[String]] = column[Option[String]]("remote_ip")

    def invokedAt: Rep[DateTime] = column[DateTime]("invoked_at")

    def * : ProvenShape[Event] = (id, profileID, eventType, remoteIp, invokedAt) <>(Event.tupled, Event.unapply) // scalastyle:ignore
  }

}
