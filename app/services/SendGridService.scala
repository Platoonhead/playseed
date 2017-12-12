package services

import com.google.inject.Inject
import com.sendgrid._
import play.api.Logger
import play.api.i18n.MessagesApi

object SendGridService {
  def receiptUploadBody: String =
    """<p><span style="color: #222222;"><span style="font-family: open sans,sans-serif;"><span style="font-size: small;">We Got It!</span></span></span></p>
      |<p><span style="color: #222222;"><span style="font-family: open sans,sans-serif;"><span style="font-size: small;">Your receipt has successfully
      |uploaded and you&rsquo;ve gained 10 entries to our #CappTheNight Sweepstakes. We need 24-48 hours to validate your receipt, so keep an eye on your
      |inbox and we will let you know when it's been cleared. Keep uploading receipts at&nbsp;</span></span></span><a href="http://cappthenight.3kudos.com/login">
      |<span style="color: #1155cc;"><span style="font-family: open sans,sans-serif;"><span style="font-size: small;"><u>CappTheNight.3kudos.com/login</u></span></span>
      |</span></a> <span style="color: #222222;"><span style="font-family: open sans,sans-serif;"><span style="font-size: small;">to earn even more entries.</span></span>
      |</span></p>""".stripMargin

  def receiptRejectionBody: String =
    """<p><span style="color: #222222;"><span style="font-family: open sans,sans-serif;"><span style="font-size: small;"> Oh No, Something Went Wrong!</span>
      |</span></span></p>
      |<p><span style="color: #222222;"><span style="font-family: open sans,sans-serif;"><span style="font-size: small;">
      | Thank you for trying, but we were unable to process your receipt. Please take a new picture and upload it again at&nbsp;</span>
      | </span> </span> <a href="http://cappthenight.3kudos.com/login"><span style="color: #1155cc;">
      | <span style="font-family: open sans,sans-serif;"> <span style="font-size: small;"> <u>CappTheNight.3kudos.com/login</u></span></span>
      | </span></a><span style="color: #222222;">
      | <span style="font-family: open sans,sans-serif;"><span style="font-size: small;">.</span></span></span></p>
      |<p><span style="color: #222222;"><span style="font-family: open sans,sans-serif;">
      |<span style="font-size: small;"> We are here to help, so if you have any problems or questions, email us at&nbsp;</span></span>
      |</span><a href="mailto:support@receiptprocessor.com"><span style="color: #1155cc;"><span style="font-family: open sans,sans-serif;">
      |<span style="font-size: small;"><u>support@receiptprocessor.com</u></span></span></span></a><span style="color: #222222;">
      |<span style="font-family: open sans,sans-serif;">
      |<span style="font-size: small;">. </span></span></span></p>""".stripMargin

  def registrationBody: String =
    """<p style="text-align: left;"><span style="font-family: 'Times New Roman', serif;">
      |<span style="color: #222222;"> <span style="font-family: open sans,sans-serif;"> <span style="font-size: small;">
      |You&rsquo;re Good To Go!</span></span></span></span></p>
      |<p style="text-align: left;"><span style="color: #222222;"><span style="font-family: open sans,sans-serif;">
      |<span style="font-size: small;"> You are all set to upload a receipt and enter our #CappTheNight Sweepstakes!
      | To enter, purchase one of our qualifying Hills Bros. Cappuccino products and upload your receipt to&nbsp; </span>
      | </span></span><a href="http://cappthenight.3kudos.com/login"><span style="color: #1155cc;"> <span style="font-family: open sans,sans-serif;">
      | <span style="font-size: small;"> <u>CappTheNight.3kudos.com/login</u></span></span></span></a><span style="color: #000000;">
      | <span style="font-family: 'Times New Roman', serif;"><span style="font-size: small;">. </span> </span></span><span style="color: #000000;">
      | <span style="font-family: 'Times New Roman', serif;"><span style="font-size: small;"> You can also</span></span></span>
      | <span style="color: #222222;"> <span style="font-family: open sans,sans-serif;"><span style="font-size: small;">visit&nbsp;</span></span></span>
      | <a href="http://www.cappthenightsweepstakes.com/"><span style="color: #1155cc;"><span style="font-family: open sans,sans-serif;"><span style="font-size: small;">
      | <u>http://www.cappthenightsweepstakes.com/</u></span></span></span></a><span style="color: #222222;"><span style="font-family: open sans,sans-serif;">
      | <span style="font-size: small;">&nbsp;daily for additional ways to earn sweepstake entries.
      |  For each receipt you upload, you will earn 10 entries to win a $2,500 shopping spree and Hills Bros.
      |   Cappuccino prize pack. 1 receipt upload per day. </span></span></span></p>""".stripMargin

  def approvalBody: String =
    """<p><span style="color: #222222;"><span style="font-family: open sans,sans-serif;"> <span style="font-size: small;">
      | Congratulations! You&rsquo;ve Earned 10 Entries!</span></span></span></p>
      |<p><span style="color: #222222;"><span style="font-family: open sans,sans-serif;"><span style="font-size: small;">
      | Your receipt has been approved! To earn additional entries, upload more receipts at&nbsp;</span></span></span>
      | <a href="http://cappthenight.3kudos.com/login"><span style="color: #1155cc;"><span style="font-family: open sans,sans-serif;">
      | <span style="font-size: small;"><u>CappTheNight.3kudos.com/login</u></span></span></span>
      | </a><span style="color: #000000;"><span style="font-family: open sans,sans-serif;">
      | <span style="font-size: small;">&nbsp;or visit&nbsp;</span> </span></span>
      | <a href="http://www.cappthenightsweepstakes.com/"><span style="color: #1155cc;">
      | <span style="font-family: open sans,sans-serif;"> <span style="font-size: small;">
      | <u>http://www.cappthenightsweepstakes.com/</u></span></span></span></a>
      | <span style="color: #222222;"><span style="font-family: open sans,sans-serif;">
      | <span style="font-size: small;">. </span></span></span></p>""".stripMargin
}

class SendGridService @Inject()(sendGridUser: SendGrid, messageApi: MessagesApi) {

  private val subject = "Hills Bros Cappuccino 'Capp The Night'"
  private val fromName = "Hills Bros. Cappuccino"
  private val fromEmail = "Support@receiptprocessor.com"

  def sendEmailForUpload(email: String, user: String): Option[String] = {
    val subject = "You’ve uploaded your Hills Bros. Cappuccino receipt."
    val body = views.html.content.email(user, SendGridService.receiptUploadBody).toString()
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
    val subject = "You are entered for our #CappTheNight Sweepstakes!"
    val body = views.html.content.email(user, SendGridService.approvalBody).toString()

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
    val subject = "We’re having some technical difficulties, please try again"
    val body = views.html.content.email(user, SendGridService.receiptRejectionBody).toString()

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
    val subject = "Thank you for registering!"
    val emailTemplate = views.html.content.email(user, SendGridService.registrationBody).toString()

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
