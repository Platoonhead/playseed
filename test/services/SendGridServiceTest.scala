package services

import com.sendgrid._
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import play.api.i18n.{Lang, MessagesApi}
import play.api.test.PlaySpecification

class SendGridServiceTest extends PlaySpecification with Mockito {

  "SendGrid Service" should {

    "send email for receipt upload" in {
      val (sendGridObj, mockedSendGrid, mockedMessageApi) = testObject

      val emailBody =
        """{"from":{"name":"Hills Bros. Cappuccino","email":"Support@receiptprocessor.com"}""".stripMargin.replaceAll("\n", "")

      val mockedResponse = new Response(200, "Email send successfully!", new java.util.HashMap[String, String]())
      when(mockedMessageApi("email.subject")(Lang("en"))) thenReturn "This is for testing purpose"
      when(mockedMessageApi("email.name")(Lang("en"))) thenReturn "This is for testing purpose"
      when(mockedMessageApi("email.receipt.success", "test")(Lang("en"))) thenReturn "Receipt has been successfully submitted"
      when(mockedSendGrid.api(any[Request])) thenReturn mockedResponse

      val result = sendGridObj.sendEmailForUpload("test@example.com", "test")

      val requestCaptor = ArgumentCaptor.forClass(classOf[Request])
      verify(mockedSendGrid).api(requestCaptor.capture)
      val request = requestCaptor.getValue

      request.body must contain(emailBody)
      result must be equalTo Some("200")
    }
    
    "send email when receipt is approved" in {
      val (sendGridObj, mockedSendGrid, mockedMessageApi) = testObject

      val emailBody =
        """{"from":{"name":"Hills Bros. Cappuccino","email":"Support@receiptprocessor.com"}""".stripMargin.replaceAll("\n", "")

      val mockedResponse = new Response(200, "Email send successfully!", new java.util.HashMap[String, String]())
      when(mockedSendGrid.api(any[Request])) thenReturn mockedResponse
      when(mockedMessageApi("email.subject")(Lang("fr"))) thenReturn "OÙ SE TROUVE LE CHAT, LÀ EST LA MAISON"
      when(mockedMessageApi("email.name")(Lang("fr"))) thenReturn "OÙ SE TROUVE LE CHAT, LÀ EST LA MAISON"
      when(mockedMessageApi("email.receipt.approved", "test")(Lang("fr"))) thenReturn "Your receipt has been approved(french)"

      val result = sendGridObj.sendEmailForApproval("test@example.com", "test")

      val requestCaptor = ArgumentCaptor.forClass(classOf[Request])
      verify(mockedSendGrid).api(requestCaptor.capture)
      val request = requestCaptor.getValue

      request.body must contain(emailBody)
      result must be equalTo Some("200")
    }

    "send email when receipt is rejected" in {
      val (sendGridObj, mockedSendGrid, mockedMessageApi) = testObject

      val emailBody =
        """{"from":{"name":"Hills Bros. Cappuccino","email":"Support@receiptprocessor.com"}""".stripMargin.replaceAll("\n", "")

      val mockedResponse = new Response(200, "Email send successfully!", new java.util.HashMap[String, String]())
      when(mockedMessageApi("email.subject")(Lang("en"))) thenReturn "This is for testing purpose"
      when(mockedMessageApi("email.name")(Lang("en"))) thenReturn "This is for testing purpose"
      when(mockedMessageApi("email.receipt.rejection", "test")(Lang("en"))) thenReturn "Receipt could be accepted"
      when(mockedSendGrid.api(any[Request])) thenReturn mockedResponse

      val result = sendGridObj.sendEmailForRejection("test@example.com", "test")

      val requestCaptor = ArgumentCaptor.forClass(classOf[Request])
      verify(mockedSendGrid).api(requestCaptor.capture)
      val request = requestCaptor.getValue

      request.body must contain(emailBody)
      result must be equalTo Some("200")
    }

    "send email for user support" in {
      val (sendGridObj, mockedSendGrid, mockedMessageApi) = testObject

      val emailBody =
        """{"from":{"name":"Hills Bros. Cappuccino","email":"Support@receiptprocessor.com"}""".stripMargin.replaceAll("\n", "")

      val mockedResponse = new Response(200, "Email send successfully!", new java.util.HashMap[String, String]())
      when(mockedMessageApi("email.subject")(Lang("en"))) thenReturn "This is for testing purpose"
      when(mockedMessageApi("email.name")(Lang("en"))) thenReturn "This is for testing purpose"
      when(mockedSendGrid.api(any[Request])) thenReturn mockedResponse

      val result = sendGridObj.sendEmailForSupport("Hills Bros. Cappuccino", "Support@receiptprocessor.com", "message")

      val requestCaptor = ArgumentCaptor.forClass(classOf[Request])
      verify(mockedSendGrid).api(requestCaptor.capture)
      val request = requestCaptor.getValue

      request.body must contain(emailBody)
      result must be equalTo Some("200")
    }

    "send email for user registration" in {
      val (sendGridObj, mockedSendGrid, mockedMessageApi) = testObject

      val emailBody =
        """{"from":{"name":"Hills Bros. Cappuccino","email":"Support@receiptprocessor.com"}""".stripMargin.replaceAll("\n", "")

      val mockedResponse = new Response(200, "Email send successfully!", new java.util.HashMap[String, String]())
      when(mockedMessageApi("email.subject")(Lang("en"))) thenReturn "This is for testing purpose"
      when(mockedMessageApi("email.name")(Lang("en"))) thenReturn "This is for testing purpose"
      when(mockedMessageApi("email.user.register", "test")(Lang("en"))) thenReturn "Thank you for register"
      when(mockedSendGrid.api(any[Request])) thenReturn mockedResponse

      val result = sendGridObj.sendEmailForRegistration("test@example.com", "test")

      val requestCaptor = ArgumentCaptor.forClass(classOf[Request])
      verify(mockedSendGrid).api(requestCaptor.capture)
      val request = requestCaptor.getValue

      request.body must contain(emailBody)
      result must be equalTo Some("200")
    }

  }

  def testObject: (SendGridService, SendGrid, MessagesApi) = {
    val mockedSendGrid = mock[SendGrid]
    val mockedMessageApi = mock[MessagesApi]
    (new SendGridService(mockedSendGrid, mockedMessageApi), mockedSendGrid, mockedMessageApi)
  }

}
