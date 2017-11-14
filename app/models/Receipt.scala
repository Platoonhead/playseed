package models

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import controllers.ReceiptCallback
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape.proveShapeOf

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Receipt(id: Long,
                   ocrReceiptId: String,
                   profileId: Long,
                   day: Int,
                   month: Int,
                   year: Int,
                   email: String,
                   storeName: Option[String],
                   p3userId: String,
                   clientId: String,
                   image1: String,
                   status: Option[String],
                   amount: Option[Double],
                   qualifyingAmount: Option[Double],
                   requestSentDate: DateTime,
                   callBackReceivedDate: Long,
                   dateInReceipt: String,
                   products: String,
                   pointSent: Boolean = false)

class ReceiptRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends ReceiptBaseRepository
  with ReceiptReadRepository with ReceiptTable with ReceiptBaseRepositoryImpl with ReceiptReadRepositoryImpl

// =====================================================================================================================
// Write Repository
// =====================================================================================================================
trait ReceiptBaseRepository {
  def store(receipt: Receipt): Future[Boolean]

  def update(callbackResponse: ReceiptCallback, serializedProductList: String): Future[Option[Receipt]]

  def updateRejectedEmail(receiptCallback: ReceiptCallback, serializedProductList: String): Future[Boolean]
}

trait ReceiptBaseRepositoryImpl extends ReceiptBaseRepository {
  self: ReceiptTable =>

  import profile.api._

  def store(receipt: Receipt): Future[Boolean] =
    db.run(receiptQuery += receipt) map (_ > 0)

  def update(receiptCallback: ReceiptCallback, serializedProductList: String): Future[Option[Receipt]] = {
    val queryResult = for {receipt <- receiptQuery if receipt.snap3ReceiptId === receiptCallback.UUID} yield (
      receipt.storeName,
      receipt.status,
      receipt.amount,
      receipt.qualifyingAmount,
      receipt.callBackReceivedDate,
      receipt.dateInReceipt,
      receipt.products,
      receipt.pointSent)

    val futureUpdateResult = db.run(queryResult.update(
      Some(receiptCallback.data.store),
      Some(receiptCallback.status.getOrElse("")),
      Some(receiptCallback.data.amount),
      Some(receiptCallback.data.qualifyingAmount),
      receiptCallback.submissionDate.getOrElse("").toLong,
      receiptCallback.data.date,
      serializedProductList,
      true))

    futureUpdateResult flatMap { size =>
      if (size > 0) {
        db.run(receiptQuery.filter(receipt => receipt.snap3ReceiptId === receiptCallback.UUID
          && receipt.status === "approved" && receipt.pointSent === true).result.headOption)
      } else {
        Future(None)
      }
    }
  }

  def updateRejectedEmail(receiptCallback: ReceiptCallback, serializedProductList: String): Future[Boolean] = {
    val queryResult = for {receipt <- receiptQuery if receipt.snap3ReceiptId === receiptCallback.UUID} yield (
      receipt.storeName,
      receipt.status,
      receipt.amount,
      receipt.qualifyingAmount,
      receipt.callBackReceivedDate,
      receipt.dateInReceipt,
      receipt.products)

    db.run(queryResult.update(
      Some(receiptCallback.data.store),
      Some(receiptCallback.status.getOrElse("")),
      Some(receiptCallback.data.amount),
      Some(receiptCallback.data.qualifyingAmount),
      receiptCallback.submissionDate.getOrElse("").toLong,
      receiptCallback.data.date,
      serializedProductList)) map (_ > 0)
  }
}

// =====================================================================================================================
// Read Repository
// =====================================================================================================================
trait ReceiptReadRepository {
  def fetchByReceiptId(ocrReceiptId: String): Future[Option[Receipt]]

  def hasReceiptSubmittedByEmail(email: String): Future[Boolean]

  def shouldSubmit(profileId: Long): Future[Boolean]
}

trait ReceiptReadRepositoryImpl extends ReceiptReadRepository {
  self: ReceiptTable =>

  import profile.api._

  def fetchByReceiptId(ocrReceiptId: String): Future[Option[Receipt]] = {
    val asd = receiptQuery.filter(_.snap3ReceiptId === ocrReceiptId).groupBy(_.id).map{case(a, b) => (a, b.map(_.amount))}

    val qwe =db.run(asd.result)
    db.run(receiptQuery.filter(_.snap3ReceiptId === ocrReceiptId).result.headOption)
  }

  def hasReceiptSubmittedByEmail(email: String): Future[Boolean] =
    db.run(receiptQuery.filter(receipt => receipt.email === email).size.result) map (_ > 0)

  def shouldSubmit(profileId: Long): Future[Boolean] = {
    val futureResult = db.run(receiptQuery.filter(receipt =>
      receipt.profileId === profileId &&
        receipt.status === "approved").size.result)

    futureResult map { result =>
      if (result < 1) true else false
    }
  }
}

trait ReceiptTable extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val receiptQuery: TableQuery[Receipts] = TableQuery[Receipts]

  private[models] class Receipts(tag: Tag) extends Table[Receipt](tag, "receipts") {

    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def snap3ReceiptId: Rep[String] = column[String]("snap3_receipt_id")

    def profileId: Rep[Long] = column[Long]("profile_id")

    def day: Rep[Int] = column[Int]("receipt_day")

    def month: Rep[Int] = column[Int]("receipt_month")

    def year: Rep[Int] = column[Int]("receipt_year")

    def email: Rep[String] = column[String]("email")

    def storeName: Rep[Option[String]] = column[Option[String]]("store_name")

    def p3userId: Rep[String] = column[String]("p3user_id")

    def clientId: Rep[String] = column[String]("client_id")

    def image: Rep[String] = column[String]("image_name")

    def status: Rep[Option[String]] = column[Option[String]]("status")

    def amount: Rep[Option[Double]] = column[Option[Double]]("amount")

    def qualifyingAmount: Rep[Option[Double]] = column[Option[Double]]("qualifying_amount")

    def requestSentDate: Rep[DateTime] = column[DateTime]("request_sent_date")

    def callBackReceivedDate: Rep[Long] = column[Long]("callback_receive_date")

    def dateInReceipt: Rep[String] = column[String]("date_in_receipt")

    def products: Rep[String] = column[String]("products", O.SqlType("TEXT"))

    def pointSent: Rep[Boolean] = column[Boolean]("point_sent")

    def * = (id, snap3ReceiptId, profileId, day, month, year, email, storeName, p3userId, clientId, image, status, amount, // scalastyle:ignore
      qualifyingAmount, requestSentDate, callBackReceivedDate, dateInReceipt, products, pointSent) <> (Receipt.tupled,
      Receipt.unapply)
  }

}
