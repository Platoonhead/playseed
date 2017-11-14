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

case class P3UserInfo(id: Long,
                      p3UserId: String,
                      email: String,
                      createdAt: DateTime)

class P3UsersInfoRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends P3UserInfoBaseRepository
  with P3UserInfoReadRepository with P3UserInfoBaseRepositoryImpl with P3UserInfoReadRepositoryImpl with P3UserInfoTable

// =====================================================================================================================
// Write Repository
// =====================================================================================================================
trait P3UserInfoBaseRepository {
  def store(userInfo: P3UserInfo): Future[Boolean]
}

trait P3UserInfoBaseRepositoryImpl extends P3UserInfoBaseRepository {
  self: P3UserInfoTable =>

  import profile.api._

  def store(userInfo: P3UserInfo): Future[Boolean] = {
    db.run(P3UserInfoQuery += userInfo) map (_ > 0)
  }
}

// =====================================================================================================================
// Read Repository
// =====================================================================================================================
trait P3UserInfoReadRepository {
  def fetchByEmail(email: String): Future[Option[P3UserInfo]]
}

trait P3UserInfoReadRepositoryImpl extends P3UserInfoReadRepository {
  self: P3UserInfoTable =>

  import profile.api._

  def fetchByEmail(email: String): Future[Option[P3UserInfo]] =
    db.run(P3UserInfoQuery.filter(_.email.toLowerCase === email.toLowerCase).result.headOption)
}

trait P3UserInfoTable extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val P3UserInfoQuery: TableQuery[P3UserInformation] = TableQuery[P3UserInformation]

  private[models] class P3UserInformation(tag: Tag) extends Table[P3UserInfo](tag, "p3_user_info") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def guid: Rep[String] = column[String]("p3_user_id")

    def email: Rep[String] = column[String]("email")

    def createdAt: Rep[DateTime] = column[DateTime]("created_date")

    def * : ProvenShape[P3UserInfo] = (id, guid, email, createdAt) <>(P3UserInfo.tupled, P3UserInfo.unapply) // scalastyle:ignore
  }

}
