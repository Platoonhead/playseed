package services

import com.google.inject.Inject
import com.sendgrid._
import play.api.Logger
import play.api.i18n.MessagesApi

object SendGridService {
  def receiptUploadBody: String =
    "Your receipt has successfully uploaded and you’ve gained 10 entries to our #CappTheNight Sweepstakes." +
      " We need 24-48 hours to validate your receipt, so keep an eye on your inbox and we will let you know when " +
      "it's been cleared. Keep uploading receipts at CappTheNight.3kudos.com/login to earn even more entries."

  def receiptRejectionBody: String =
    "Thank you for trying, but we were unable to process your receipt. Please take a new picture and upload it again" +
      " at CappTheNight.3kudos.com/login. We are here to help, so if you have any problems or questions, email" +
      " us at support@receiptprocessor.com."


  def registrationBody: String =
    "You are all set to upload a receipt and enter our #CappTheNight Sweepstakes! To enter, purchase one of our" +
      " qualifying Hills Bros. Cappuccino products and upload your receipt to CappTheNight.3kudos.com/login. You" +
      " can also visit http://www.cappthenightsweepstakes.com/ daily for additional ways to earn sweepstake entries. " +
      "For each receipt you upload, you will earn 10 entries to win a $2,500 shopping spree and Hills Bros. Cappuccino " +
      "prize pack. 1 receipt upload per day."

  def approvalBody: String =
    "Your receipt has been approved! To earn additional entries, upload more receipts at " +
      "CappTheNight.3kudos.com/login or visit http://www.cappthenightsweepstakes.com."
}

class SendGridService @Inject()(sendGridUser: SendGrid, messageApi: MessagesApi) {

  private val subject = "Hills Bros Cappuccino 'Capp The Night'"
  private val fromName = "Hills Bros. Cappuccino"
  private val fromEmail = "Support@receiptprocessor.com"

  def sendEmailForUpload(email: String, user: String, unsubscribeLink: String): Option[String] = {

    val subject = "You’ve uploaded your Hills Bros. Cappuccino receipt."
    val greeting = "We Got It!"
    val body = views.html.content.email(user, greeting, SendGridService.receiptUploadBody, unsubscribeLink).toString()
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

  def sendEmailForApproval(email: String, user: String, unsubscribeLink: String): Option[String] = {
    val subject = "You are entered for our #CappTheNight Sweepstakes!"
    val greeting = "Congratulations! You’ve Earned 10 Entries!"
    val body = views.html.content.email(user, greeting, SendGridService.approvalBody, unsubscribeLink).toString()

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

  def sendEmailForRejection(email: String, user: String, unsubscribeLink: String): Option[String] = {
    val subject = "We’re having some technical difficulties, please try again"
    val greeting = "Oh No, Something Went Wrong!"

    val body = views.html.content.email(user, greeting, SendGridService.receiptRejectionBody, unsubscribeLink).toString()

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

  def sendEmailForRegistration(email: String, user: String, unsubscribeLink: String): Option[String] = {
    val subject = "Thank you for registering!"
    val greeting = "You’re Good To Go!"
    val emailTemplate = views.html.content.email(user, greeting, SendGridService.registrationBody, unsubscribeLink).toString()

    val from = new Email(fromEmail)
    from.setName(fromName)
    val to = new Email(email)

    val content = new Content("text/html", emailTemplate)
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
    from.setName(user)
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
