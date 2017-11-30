package services

import com.google.inject.Inject
import com.sendgrid._
import play.api.Logger
import play.api.i18n.MessagesApi

object SendGridService {
  def receiptUploadBody(username: String): String =
    s"""<p class="western">Hi $username,</p>
        |<p class="western"><strong>AWESOME, YOUR RECEIPT HAS BEEN RECEIVED </strong></p>
        |<p class="western">Thank you for participating in the Hills Bros. #CappTheNight Promotion.
        |Your receipt has been successfully uploaded and submitted. Please allow 24-48 hours for us to validate your submission.
        |Upload more receipts at <a href="cappthenightsweepstakes.com/login">cappthenightsweepstakes.com/login</a>
        |to increase your chances of winning!</p>"""
      .stripMargin

  def receiptRejectionBody(username: String): String =
    s"""<p class="western">Hi $username,</p>
        |<p class="western"><strong>OH NO! HOUSTON, WE HAVE A PROBLEM </strong></p>
        |<p class="western">Thank you for submitting your receipt to the Hills Bros. #CappTheNight Promotion,
        |but unfortunately, an error has occurred and we are unable to process your receipt at this time. Please re-submit at
        |<a href="cappthenightsweepstakes.com/login">cappthenightsweepstakes.com/login</a>. We are sorry for the inconvenience
        |this has caused. If you have any questions, please contact us at <a href="support@receiptprocessor.com">support@receiptprocessor.com</a>.</p>"""
      .stripMargin

  def registrationBody(username: String): String =
    s"""<p class="western">Hi $username,</p>
        |<p class="western"><strong>BRAVO, YOU ARE REGISTERED</strong></p>
        |<p class="western">Thank you for registering for the Hills Bros. #CappTheNight Promotion.
        |If you want to enter our sweepstakes, please either buy one of our products and upload your receipt to
        |<a href="cappthenightsweepstakes.com/login">cappthenightsweepstakes.com/login</a> or X (how do they enter without
        |purhcase). Upload more receipts to increase your chances of winning!</p>""".stripMargin

  def approvalBody(username: String, link: String = ""): String =
    s"""<p class="western">Hi $username,</p>
        |<p class="western"><strong>HURRAY! YOU HAVE ENTERED THE SWEEPSTAKES </strong></p>
        |<p class="western">Your receipt has been validated and you are one step closer to winning your awesome  $$2,500
        |shopping spree! Thank you for your participation! To increase your chances of winning, upload more receipts at
        |<a href="cappthenightsweepstakes.com/login">cappthenightsweepstakes.com/login</a>.</p>""".stripMargin
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
