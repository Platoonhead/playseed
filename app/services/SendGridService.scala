package services

import com.google.inject.Inject
import com.sendgrid._
import play.api.Logger
import play.api.i18n.MessagesApi

object SendGridService {
  def receiptUploadBody(username: String): String =
    s"""<p><span style="font-family: Cambria;"><span style="font-size: 12px;">Hi $username,</span></span></p>
       |<p><span style="font-family: Cambria;"><span style="font-size: 12px;">Thank you for submitting your receipt.
       |Great news - it has been successfully uploaded and submitted. Please allow 24-48 hours for us to validate your
       |submission. To continue your participation, visit us at: </span></span><a href="http://www.zabwine.com">
       |<span style="font-size: 12px; font-family: Cambria;">www.</span><span style="font-family: Calibri, serif;">
       |<span style="font-size: 12px;">zabwine.com</span></span></a>.</p>
       |<p><span style="font-size: 12px; font-family: Cambria;">Cheers!</span></p>
     """
      .stripMargin

  def receiptRejectionBody(username: String): String =
    s"""<p><span style="font-family: Cambria;"><span style="font-size: 12px;">Hi $username,</span></span></p>
       |<p><span style="font-family: Cambria;"><span style="font-size: 12px;">Thank you for submitting your receipt to
       |for the Z.Alexander Brown Song Download. Unfortunately, an error has occurred and we are unable to process your
       |receipt at this time. Please re-submit at </span></span><a href="http://www.zabwine.com">
       |<span style="font-size: 12px; font-family: Cambria;">www.</span><span style="font-family: Calibri, serif;">
       |<span style="font-size: 12px;">zabwine.com</span></span></a>.</p>
       |<span style="font-size: 12px; font-family: Cambria;">We are sorry for the inconvenience this has caused. If you
       |have any questions, please let us know.</span>
     """
      .stripMargin

  def registrationBody(username: String): String =
    s"""<p><span style="font-family: Cambria;"><span style="font-size: 12px;">Hi $username,</span></span></p>
       |<p class="western"><span style="font-family: Cambria;"><span style="font-size: 12px;">Thank you for registering
       |for a </span></span> <span style="color: #000000;"><span style="font-family: Cambria;">
       |<span style="font-size: 12px;"> <strong>Zac Brown Band song download.</strong></span></span></span>
       |<span style="font-family: Calibri, serif;"><span style="font-size: 12px;"> Visit us at </span></span>
       |<a href="http://www.zabwine.com"><span style="font-size: 12px; font-family: Cambria;">
       |  www.</span><span style="font-family: Calibri,serif;"><span style="font-size: 12px;">zabwine.com</span></span></a>
       |<span style="font-family: Cambria;"><span style="font-size: 12px;">for next steps.</span></span>&nbsp;</p>
       |<p><span style="font-size: 12px; font-family: Cambria;">Cheers!</span></p>
     """.stripMargin

  def approvalBody(username: String, link: String = ""): String =
    s"""<p>Hi $username,</p>
       |<p>Thank you for participating in our Z. Alexander Brown song download. Your submission has been validated;
       |please use the below link to access your song.</p>
       |<p><a href="$link">$link</a></p>
       |<p>Tip: If using an iOS device &ndash; please download directly to your PC.</p>
       |<p>Cheers!</p>
     """.stripMargin
}

class SendGridService @Inject()(sendGridUser: SendGrid, messageApi: MessagesApi) {

  private val subject = "Hills Bros Cappuccino 'Capp The Night'"
  private val fromName = "Hills Bros. Cappuccino"
  private val fromEmail = "Support@receiptprocessor.com"

  def sendEmailForUpload(email: String, user: String): Option[String] = {
    val body = SendGridService.receiptUploadBody(user)
    val from = new Email(fromEmail)
    from.setName(fromName)

    val to = new Email(email)
    val content = new Content("text/html", body)

    val mail = new Mail(from, subject, to, content)

    val request = new Request
    request.method = Method.POST
    request.endpoint = "mail/send"
    request.body = mail.build

    try {
      val response = sendGridUser.api(request)

      Some(response.statusCode.toString)
    } catch {
      case ex: Exception =>
        Logger.error(s"Got an exception while sending email after uploading the receipt to email $email, ex $ex")
        ex.printStackTrace()
        None
    }
  }

  def sendEmailForApproval(email: String, user: String): Option[String] = {
    val body = SendGridService.approvalBody(user)

    val from = new Email(fromEmail)
    from.setName(fromName)
    val to = new Email(email)

    val content = new Content("text/html", body)

    val mail = new Mail(from, subject, to, content)

    val request = new Request
    request.method = Method.POST
    request.endpoint = "mail/send"
    request.body = mail.build

    try {
      val response = sendGridUser.api(request)

      Some(response.statusCode.toString)
    } catch {
      case ex: Exception =>
        Logger.error(s"Got an exception while sending email for approved receipt to email $email, ex $ex")
        ex.printStackTrace()
        None
    }
  }

  def sendEmailForRejection(email: String, user: String): Option[String] = {
    val body = SendGridService.receiptRejectionBody(user)

    val from = new Email(fromEmail)
    from.setName(fromName)
    val to = new Email(email)

    val content = new Content("text/html", body)

    val mail = new Mail(from, subject, to, content)

    val request = new Request
    request.method = Method.POST
    request.endpoint = "mail/send"
    request.body = mail.build

    try {
      val response = sendGridUser.api(request)

      Some(response.statusCode.toString)
    } catch {
      case ex: Exception =>
        Logger.error(s"Got an exception while sending email for rejected receipt to email $email, ex $ex")
        ex.printStackTrace()
        None
    }
  }

  def sendEmailForRegistration(email: String, user: String): Option[String] = {
    val body = SendGridService.registrationBody(user)

    val from = new Email(fromEmail)
    from.setName(fromName)
    val to = new Email(email)

    val content = new Content("text/html", body)
    val mail = new Mail(from, subject, to, content)

    val request = new Request
    request.method = Method.POST
    request.endpoint = "mail/send"
    request.body = mail.build

    try {
      val response = sendGridUser.api(request)
      Some(response.statusCode.toString)
    } catch {
      case ex: Exception =>
        Logger.error(s"Got an exception while sending email for registration to email $email, ex $ex")
        ex.printStackTrace()
        None
    }
  }

  def sendEmailForSupport(user: String, email: String, message: String): Option[String] = {
    val from = new Email(email)
    from.setName(fromName)
    val to = new Email(fromEmail)

    val content = new Content("text/html", message)

    val mail = new Mail(from, subject, to, content)

    val request = new Request
    request.method = Method.POST
    request.endpoint = "mail/send"
    request.body = mail.build

    try {
      val response = sendGridUser.api(request)
      Some(response.statusCode.toString)
    } catch {
      case ex: Exception =>
        Logger.error(s"Got an exception while sending email for support from email $email, ex $ex")
        ex.printStackTrace()
        None
    }
  }

}
