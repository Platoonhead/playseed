package models

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ForeignKeyQuery
import slick.lifted.ProvenShape.proveShapeOf
import utilities.Util._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class RewardCode(id: Long,
                      code: String,
                      profileId: Option[Long],
                      receiptId: Option[Long],
                      used: Boolean,
                      created: Option[DateTime])

class RewardRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends RewardBaseRepository with
  RewardBaseRepositoryImpl with RewardReadRepository with RewardReadRepositoryImpl with RewardTable

// =====================================================================================================================
// Write Repository
// =====================================================================================================================
trait RewardBaseRepository {
  def markedCodeUsed(code: String, profileId: Long, receiptId: Option[Long]): Future[Boolean]
  def isRewarded(profileId: Long): Future[Boolean]
}

trait RewardBaseRepositoryImpl extends RewardBaseRepository {
  self: RewardTable =>

  import profile.api._

  def markedCodeUsed(code: String, profileId: Long, receiptId: Option[Long]): Future[Boolean] = {
    val time = getPacificTime
    db.run(rewardQuery.filter(_.code === code)
      .map(rewardObj => (rewardObj.profileId, rewardObj.receiptId, rewardObj.used, rewardObj.created))
      .update(Some(profileId), receiptId, true, Some(time))) map (_ > 0)

  }

  def isRewarded(profileId: Long): Future[Boolean] = {
    val futureResult = db.run(rewardQuery.filter(reward =>
      reward.profileId === profileId &&
        reward.used === true).size.result)

    futureResult map { result =>
      if (result < 1) false else true
    }
  }

}

// =====================================================================================================================
// Read Repository
// =====================================================================================================================
trait RewardReadRepository {
  def checkUsedRewards: Future[Boolean]

  def getUnusedCode: Set[String]
}

trait RewardReadRepositoryImpl extends RewardReadRepository {
  self: RewardTable =>

  import profile.api._

  def getUnusedCode: Set[String] = {
    val futureResult = db.run(rewardQuery.filter(_.used === false).map(_.code).to[Set].result)

    Await.result(futureResult, Duration.Inf)
  }

  def checkUsedRewards: Future[Boolean] = {
    db.run(rewardQuery.filter(_.used === false).size.result) map (_ == 0)
  }
}

trait RewardTable extends HasDatabaseConfigProvider[JdbcProfile] with ReceiptTable with UserProfileRepositoryTable {

  import profile.api._

  val rewardQuery: TableQuery[Rewards] = TableQuery[Rewards]

  private[models] class Rewards(tag: Tag) extends Table[RewardCode](tag, "reward_code") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def code: Rep[String] = column[String]("code")

    def profileId: Rep[Option[Long]] = column[Option[Long]]("profile_id")

    def receiptId: Rep[Option[Long]] = column[Option[Long]]("receipt_id")

    def used: Rep[Boolean] = column[Boolean]("used")

    def created: Rep[Option[DateTime]] = column[Option[DateTime]]("created_date")

    def * = (id, code, profileId, receiptId, used, created) <>(RewardCode.tupled, RewardCode.unapply) // scalastyle:ignore

    def receiptFkId: ForeignKeyQuery[Receipts, Receipt] = foreignKey("receipt_id_fk", receiptId, receiptQuery)(_.id.?)

    def profileFkId: ForeignKeyQuery[UserProfile, Profile] = foreignKey("profileId_fk", profileId, userProfileQuery)(_.id.?)
  }

}
