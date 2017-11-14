package models

import controllers.{ReceiptCallback, Data}
import org.joda.time.DateTime
import org.specs2.mutable.Specification

class ReceiptTest extends Specification {

  val receiptRepository = new ModelsTest[ReceiptRepository]
  val dateTime = new DateTime(System.currentTimeMillis())
  val receipt = Receipt(1, "receiptId", 1, 12, 2, 2017, "test@example.com", None, "userId", "clientId", "image", None,
    Some(2000), Some(1000), dateTime, 123456789, "dateInReceipt", "product")

  val receipt1 = Receipt(2, "receiptId123", 2, 15, 2, 2017, "test@example.com", None, "userId", "clientId", "image", None,
    Some(2000), Some(1000), dateTime, 123456789, "dateInReceipt", "product")

  val receiptCallBack = ReceiptCallback(Some("approved"), None, Some("123456"), "receiptId", Some("rawdata"), Data("store", "01-01-1998", 2000, 1000, List()))

  "ReceiptRepository" should {

    "store receipt details" in {
      val storeResult = receiptRepository.result(receiptRepository.repository.store(receipt))
      receiptRepository.result(receiptRepository.repository.store(receipt1))

      storeResult must equalTo(true)
    }

    "update receipt" in {
      val receiptUpdateResult = receiptRepository.result(receiptRepository.repository.update(receiptCallBack, "serializedProductList"))

      receiptUpdateResult.get.id must equalTo(1L)
    }

    "update rejected receipt" in {
      val receiptUpdateResult = receiptRepository.result(receiptRepository.repository.updateRejectedEmail(receiptCallBack, "serializedProductList"))

      receiptUpdateResult must equalTo(true)
    }

    "fetch receipt by receiptId" in {
      val receiptResult = receiptRepository.result(receiptRepository.repository.fetchByReceiptId("receiptId"))
      val receiptId = receiptResult.get.ocrReceiptId

      receiptId must equalTo("receiptId")
    }
  }
}
