package models

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape
import slick.lifted.ProvenShape.proveShapeOf

import scala.concurrent.Future

case class Support(id: Long,
                   name: String,
                   email: String,
                   message: String,
                   date: DateTime)

class SupportRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends SupportBaseRepository
  with SupportBaseRepositoryImpl with SupportRepositoryTable

// =====================================================================================================================
// Write Repository
// =====================================================================================================================
trait SupportBaseRepository {
  def store(supportForm: Support): Future[Long]
}

trait SupportBaseRepositoryImpl {
  self: SupportRepositoryTable =>

  import profile.api._

  def store(supportForm: Support): Future[Long] =
    db.run(supportQuery returning supportQuery.map(_.id) += supportForm)
}

trait SupportRepositoryTable extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val supportQuery: TableQuery[Supports] = TableQuery[Supports]

  private[models] class Supports(tag: Tag) extends Table[Support](tag, "user_support") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def name: Rep[String] = column[String]("name")

    def email: Rep[String] = column[String]("email")

    def message: Rep[String] = column[String]("message")

    def date: Rep[DateTime] = column[DateTime]("created_date")

    def * : ProvenShape[Support] = (id, name, email, message, date) <>(Support.tupled, Support.unapply) // scalastyle:ignore
  }

}
