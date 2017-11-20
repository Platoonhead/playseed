package models

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape
import slick.lifted.ProvenShape.proveShapeOf
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

case class Profile(id: Long,
                   firstName: String,
                   lastName: String,
                   email: String,
                   dateOfBirth: DateTime,
                   created: DateTime,
                   suspended: Boolean)

class UserProfileRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends UserProfileBaseRepository
  with UserProfileReadRepository with UserProfileBaseRepositoryImpl with UserProfileRepositoryTable
  with UserProfileReadRepositoryImpl

// =====================================================================================================================
// Write Repository
// =====================================================================================================================
trait UserProfileBaseRepository {
  def store(userProfile: Profile): Future[Option[Long]]
}

trait UserProfileBaseRepositoryImpl extends UserProfileBaseRepository {
  self: UserProfileRepositoryTable =>

  import profile.api._

  def store(userProfile: Profile): Future[Option[Long]] = {
    val futureProfileId = db.run(userProfileQuery returning userProfileQuery.map(_.id) += userProfile)

    futureProfileId.flatMap {
      case profileId =>
        if (profileId > 0) Future.successful(Some(profileId)) else Future.successful(None)
    }
  }
}

// =====================================================================================================================
// Read Repository
// =====================================================================================================================
trait UserProfileReadRepository {
  def findByEmail(email: String): Future[Option[Profile]]

  def findByProfileId(profileId: Long): Future[Option[Profile]]
}

trait UserProfileReadRepositoryImpl extends UserProfileReadRepository {
  self: UserProfileRepositoryTable =>

  import profile.api._

  def findByEmail(email: String): Future[Option[Profile]] =
    db.run(userProfileQuery.filter(_.email.toLowerCase === email.toLowerCase).result.headOption)

  def findByProfileId(profileId: Long): Future[Option[Profile]] =
    db.run(userProfileQuery.filter(_.id === profileId).result.headOption)
}

trait UserProfileRepositoryTable extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val userProfileQuery: TableQuery[UserProfile] = TableQuery[UserProfile]

  private[models] class UserProfile(tag: Tag) extends Table[Profile](tag, "user_profile") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def firstName: Rep[String] = column[String]("first_name")

    def lastName: Rep[String] = column[String]("last_name")

    def email: Rep[String] = column[String]("email")

    def dateOfBirth: Rep[DateTime] = column[DateTime]("dob")

    def created: Rep[DateTime] = column[DateTime]("created")

    def suspended: Rep[Boolean] = column[Boolean]("suspended")

    def * : ProvenShape[Profile] = (id, firstName, lastName, email, dateOfBirth, // scalastyle:ignore
      created, suspended) <> (Profile.tupled, Profile.unapply)
  }

}
