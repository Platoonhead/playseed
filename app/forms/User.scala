package forms

import javax.inject.Inject

import org.joda.time.{LocalDate, Years}
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, Mapping}
import play.api.i18n._

case class SupportForm(name: String, email: String, message: String)

case class Email(email: String, confirmedEmail: String)

case class DateOfBirth(birthDay: String, birthMonth: String, birthYear: String)

case class User(email: Email,
                firstName: String,
                lastName: String,
                dob: DateOfBirth,
                reCaptcha: String,
                isAgree: Boolean)

class UserForm @Inject()(langs: Langs, val messagesApi: MessagesApi) {
  val lang: Lang = langs.availables.head
  implicit val messages: Messages = MessagesImpl(lang, messagesApi)

  private val emailRegex =
    """([a-zA-Z0-9._-]+)(@)([a-zA-Z0-9]+)([.])([a-zA-Z.]{2,})"""
  val validEmail: Mapping[String] = email.verifying(messages("validation.invalid.email"), email => email.matches(emailRegex))

  val userSupportForm = Form(
    mapping(
      "name" -> text.verifying(messages("validation.support.name"), !_.trim.isEmpty),
      "email" -> text.verifying(emailAddress),
      "message" -> text.verifying(messages("validation.support.message"), !_.trim.isEmpty)
    )(SupportForm.apply)(SupportForm.unapply)
  )

  val loginForm = Form(
    single(
      "email" -> validEmail
    )
  )

  val signUpForm = Form(mapping(
    "emailGroup" -> mapping(
      "email" -> text.verifying(emailAddress),
      "confirmedEmail" -> text.verifying(confirmEmailAddress))(Email.apply)(Email.unapply)
      .verifying(messages("validation.email.matchError"), emailGroup => emailGroup.email.toUpperCase.trim == emailGroup.confirmedEmail.toUpperCase.trim),
    "firstName" -> text.verifying(messages("validation.firstName.empty"), !_.trim.isEmpty),
    "lastName" -> text.verifying(messages("validation.lastName.empty"), !_.trim.isEmpty),
    "dob" -> mapping(
      "birthDay" -> text.verifying(messages("validation.birthDay.empty"), !_.isEmpty),
      "birthMonth" -> text.verifying(messages("validation.birthMonth.empty"), !_.isEmpty),
      "birthYear" -> text.verifying(messages("validation.birthYear.empty"), !_.isEmpty)
    )(DateOfBirth.apply)(DateOfBirth.unapply)
      .verifying(messages("validation.dateOfBirth.invalid"), dob => isValidDate(dob.birthDay.toInt, dob.birthMonth.toInt, dob.birthYear.toInt)),
    "g-recaptcha-response" -> text.verifying(messages("validation.captcha.error"), !_.isEmpty),
    "isAgree" -> boolean.verifying(messages("validation.isAgree.empty"), isAgree => isAgree.equals(true))
  )(User.apply)(User.unapply))

  private def emailAddress: Constraint[String] = Constraint[String]("constraint.email") { email =>
    if (email == null) {
      // scalastyle:ignore
      Invalid(ValidationError(messages("validation.invalid.email")))
    } else if (email.trim.isEmpty) {
      Invalid(ValidationError(messages("validation.invalid.email")))
    } else {
      emailRegex.r.findFirstMatchIn(email.trim)
        .map(_ => Valid)
        .getOrElse(Invalid(ValidationError(messages("validation.invalid.email"))))
    }
  }

  private def confirmEmailAddress: Constraint[String] = Constraint[String]("constraint.email") { email =>
    if (email == null) {
      // scalastyle:ignore
      Invalid(ValidationError(messages("validation.email.matchError")))
    } else if (email.trim.isEmpty) {
      Invalid(ValidationError(messages("validation.email.matchError")))
    } else {
      emailRegex.r.findFirstMatchIn(email.trim)
        .map(_ => Valid)
        .getOrElse(Invalid(ValidationError(messages("validation.email.matchError"))))
    }
  }

  private def isValidDate(date: Int, month: Int, year: Int): Boolean = {
    try {
      val dob = new LocalDate(year, month, date)
      Years.yearsBetween(dob, new LocalDate()).getYears
      true
    } catch {
      case ex: Exception => false
    }
  }
}

