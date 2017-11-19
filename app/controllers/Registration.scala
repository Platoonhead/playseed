package controllers

import javax.inject.Inject

import forms.{User, UserForm}
import models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.i18n._
import play.api.libs.json._
import play.api.mvc._
import services._
import utilities.Util._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Registration @Inject()(controllerComponent: ControllerComponents,
                             userForm: UserForm,
                             blockListRepository: BlockListRepository,
                             userProfileRepository: UserProfileRepository,
                             platformBridgeService: PlatformBridgeService,
                             eventRepository: EventRepository,
                             p3UserInfoRepository: P3UsersInfoRepository,
                             googleReCaptchaService: GoogleReCaptchaService,
                             eventSender: EventSenderService,
                             sendGridService: SendGridService,
                             receiptRepository: ReceiptRepository,
                             associateRepository: AssociateRepository
                            ) extends AbstractController(controllerComponent) {
  implicit val jodaDateReads = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss'Z'")
  implicit val jodaDateWrites = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss'Z'")

  implicit val formatProfile = Json.format[Profile]

  val lang: Lang = controllerComponent.langs.availables.head
  implicit val message: Messages = MessagesImpl(lang, messagesApi)

  def saveContestant: Action[AnyContent] = Action.async { implicit request =>

    userForm.signUpForm.bindFromRequest.fold(
      formWithError => {
        Logger.info(s"Bad request while registration ${formWithError.errors}")
        Future.successful(BadRequest(views.html.content.register(formWithError)))
      },
      data => {
        val userEmail = data.email.email.trim.toLowerCase
        val remoteIp = request.remoteAddress
        val time = getPacificTime
        val messages: Messages = controllerComponent.messagesApi.preferred(request)

        googleReCaptchaService.checkReCaptchaValidity(data.reCaptcha, remoteIp) match {
          case true  =>
            blockListRepository.shouldEnter(userEmail).flatMap {
              case true  =>
                userProfileRepository.findByEmail(userEmail).flatMap {
                  case Some(userProfile) =>
                    redirectExistingUser(userProfile, remoteIp, data)
                  case None              =>
                    val dateInString = "%s-%s-%s" format(data.dob.birthYear, data.dob.birthMonth, data.dob.birthDay)
                    val dob = DateTime.parse(dateInString)

                    val profile = Profile(0, data.firstName.trim, data.lastName.trim, userEmail, dob, time, suspended = false)

                    createP3UserAndProfile(profile).flatMap {
                      case Some(p3UserId) => createUserLocalProfile(remoteIp, request.host, profile, p3UserId, data)
                      case None           =>
                        Logger.info(s"Registration: some error occurred, cannot create user in p3 for email $userEmail")
                        Future.successful(Redirect(routes.Application.register())
                          .flashing("error" -> messages("common.error.message")))
                    }
                }
              case false =>
                Logger.info(s"Blocked user trying to register with email $userEmail")
                Future.successful(Redirect(routes.Application.register()).flashing("error" -> messages("registration.ineligible")))
            }
          case false =>
            Logger.error(s"Can not validate recaptcha response for email $userEmail")
            Future.successful(Redirect(routes.Application.register()).flashing("error" -> messages("registration.suspicious")))
        }
      })
  }

  private def redirectExistingUser(userProfile: Profile, remoteIp: String, data: User)
                                  (implicit request: Request[AnyContent]): Future[Result] = {
    val time = getPacificTime
    val jsonStringProfile = Json.toJson(userProfile).toString()
    val messages: Messages = controllerComponent.messagesApi.preferred(request)

    Logger.info(s"Registration: user already exists with email ${userProfile.email}")
    p3UserInfoRepository.fetchByEmail(userProfile.email).flatMap {
      case Some(userInfo) =>
        eventRepository.store(Event(0, userProfile.id, "signin", Some(remoteIp), time)).flatMap {
          case true  =>
            eventSender.insertLoginEvent(userInfo.p3UserId, userProfile.firstName + " " + userProfile.lastName)
            Future.successful(Redirect(routes.Application.upload()).withSession("userInfo" -> jsonStringProfile))
          case false =>
            Logger.error(s"Could not store sign in event in events table for user profile id ${userProfile.id}")
            Future.successful(Redirect(routes.Application.register()).flashing("error" -> messages("common.error.message")))
        }
      case None           =>
        Logger.info(s"P3UserInfo is not found by email ${userProfile.email}")
        Future.successful(Redirect(routes.Application.register())
          .flashing("error" -> messages("common.error.message")))
    }
  }

  private def createP3UserAndProfile(profile: Profile): Future[Option[String]] = {
    val time = getPacificTime
    val userCheckStatus = platformBridgeService.checkUserExistsByEmail(profile.email)

    userCheckStatus match {
      case None           =>
        platformBridgeService.createUser(profile).flatMap {
          case Some("409") => Future.successful(None)
          case None        => Future.successful(None)
          case p3userId    => Future.successful(platformBridgeService.createProfile(p3userId.get, profile))
        }
      case Some(p3UserId) =>
        p3UserInfoRepository.store(P3UserInfo(0, p3UserId, profile.email, time)).flatMap {
          case true  => Future.successful(createProfileProcess(p3UserId, profile))
          case false => Future.successful(None)
        }
    }
  }

  private def createProfileProcess(p3UserId: String, profile: Profile): Option[String] = {
    platformBridgeService.checkProfileExistsByGuid(p3UserId) match {
      case Some(_) => platformBridgeService.createProfile(p3UserId, profile)
      case None    => None
    }
  }

  private def createUserLocalProfile(remoteIp: String, requestHost: String, profile: Profile,
                                     p3UserId: String, data: User)(implicit request: Request[AnyContent]) = {
    val time = getPacificTime
    val messages: Messages = controllerComponent.messagesApi.preferred(request)
    userProfileRepository.store(profile).flatMap {
      case Some(profileId) =>
        val userProfile = Profile(profileId, profile.firstName, profile.lastName, profile.email, profile.dateOfBirth,
          time, suspended = false)
        val jsonStringProfile = Json.toJson(userProfile).toString()
        val userAssociateInfo = Associate(0, profileId, p3UserId, None, None, Some("userpass"))
        associateRepository.store(userAssociateInfo).flatMap {
          case true  =>
            eventRepository.store(Event(0, profileId, "signup", Some(remoteIp), time)).flatMap {
              case true  =>
                p3UserInfoRepository.fetchByEmail(profile.email).flatMap {
                  case Some(userInfo) =>
                    eventSender.insertSignUpEvent(userInfo.p3UserId, profile.firstName + " " + profile.lastName)
                    val emailSent = sendGridService.sendEmailForRegistration(profile.email, profile.firstName)
                    Logger.info(s"Email sent for user registration for email ${userInfo.email} and p3 user id ${userInfo.p3UserId}, $emailSent " +
                      s"for register with purchase")
                    Future.successful(Redirect(routes.Application.upload()).withSession("userInfo" -> jsonStringProfile)
                      .flashing("success" -> messages("registration.success")))
                  case None           =>
                    Logger.error(s"P3 user information could not fetch by user email ${userProfile.email}")
                    Future.successful(Redirect(routes.Application.login()).flashing("error" -> messages("common.error.message")))
                }
              case false =>
                Logger.error(s"Could not store sign up event in events table for user profile id ${userProfile.id}")
                Future.successful(Redirect(routes.Application.register()).flashing("error" -> messages("common.error.message")))
            }
          case false =>
            Logger.error(s"User entry could not be stored in associate table for user email ${profile.email}")
            Future.successful(Redirect(routes.Application.register()).flashing("error" -> "Something went wrong, please try again."))
        }
      case None            =>
        Logger.error(s"User profile could not be stored for user email ${profile.email}")
        Future.successful(Redirect(routes.Application.register()).flashing("error" -> messages("common.error.message")))
    }
  }

}
