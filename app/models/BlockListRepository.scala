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

case class BlockList(id: Long,
                     email: String,
                     blockedDateTime: DateTime)

class BlockListRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends BlockListBaseRepository
  with BlockListReadRepository with BlockListBaseRepositoryImpl with BlockListReadRepositoryImpl with BlockListTable

// =====================================================================================================================
// Write Repository
// =====================================================================================================================
trait BlockListBaseRepository {
  def store(blockList: BlockList): Future[Long]
}

trait BlockListBaseRepositoryImpl extends BlockListBaseRepository {
  self: BlockListTable =>

  import profile.api._

  def store(blockList: BlockList): Future[Long] =
    db.run(blockListQuery returning blockListQuery.map(_.id) += blockList)
}

// =====================================================================================================================
// Read Repository
// =====================================================================================================================
trait BlockListReadRepository {
  def shouldEnter(email: String): Future[Boolean]
}

trait BlockListReadRepositoryImpl extends BlockListReadRepository {
  self: BlockListTable =>

  import profile.api._

  def shouldEnter(email: String): Future[Boolean] = {
    val result = db.run(blockListQuery.filter(_.email === email).result.headOption)

    result map {
      case Some(blockList) => blockList.blockedDateTime.isBefore(DateTime.now().minusHours(1))
      case None            => true
    }
  }

}

trait BlockListTable extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val blockListQuery: TableQuery[BlockLists] = TableQuery[BlockLists]

  private[models] class BlockLists(tag: Tag) extends Table[BlockList](tag, "user_blocklist") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def email: Rep[String] = column[String]("block_email")

    def blockedDateTime: Rep[DateTime] = column[DateTime]("blocked_date_time")

    def * : ProvenShape[BlockList] = (id, email, blockedDateTime) <>(BlockList.tupled, BlockList.unapply) // scalastyle:ignore
  }

}
