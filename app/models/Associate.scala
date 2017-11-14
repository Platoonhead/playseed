package models

import javax.inject.Inject

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.{ForeignKeyQuery, ProvenShape}
import slick.lifted.ProvenShape.proveShapeOf

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class Associate(id: Long,
                     profileId: Long,
                     p3UserId: String,
                     facebookId: Option[String],
                     twitterId: Option[String],
                     emailPass: Option[String])

class AssociateRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends AssociatesBaseRepository
  with AssociatesBaseRepositoryImpl with AssociatesTable

// =====================================================================================================================
// Write Repository
// =====================================================================================================================
trait AssociatesBaseRepository {
  def store(associateUser: Associate): Future[Boolean]
}

trait AssociatesBaseRepositoryImpl extends AssociatesBaseRepository {
  self: AssociatesTable =>

  import profile.api._

  def store(associateUser: Associate): Future[Boolean] = {
    db.run(associateQuery += associateUser) map (_ > 0)
  }
}


trait AssociatesTable extends HasDatabaseConfigProvider[JdbcProfile] with UserProfileRepositoryTable {

  import profile.api._

  val associateQuery: TableQuery[Associates] = TableQuery[Associates]

  private[models] class Associates(tag: Tag) extends Table[Associate](tag, "associates") {

    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def profileId: Rep[Long] = column[Long]("profile_id")

    def p3UserId: Rep[String] = column[String]("p3_user_id")

    def facebookId: Rep[Option[String]] = column[Option[String]]("facebook_id")

    def twitterId: Rep[Option[String]] = column[Option[String]]("twitter_id")

    def emailPass: Rep[Option[String]] = column[Option[String]]("email_pass")

    def * : ProvenShape[Associate] = (id, profileId, p3UserId, facebookId, twitterId, emailPass) <>(Associate.tupled, Associate.unapply) // scalastyle:ignore

    def profileFkId: ForeignKeyQuery[UserProfile, Profile] = foreignKey("profile_id_fk", profileId, userProfileQuery)(_.id)
  }

}
