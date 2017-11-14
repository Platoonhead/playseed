package utilities

import javax.inject.Inject

import play.api.Configuration

case class PlatformConfig(p3ClientId: String,
                          p3DomainAccount: String,
                          p3DomainEvent: String,
                          p3DomainIngestor: String,
                          googleRECaptchaCode: String,
                          p3CampaignId: String,
                          signUpBehaviourId: String,
                          signInBehaviourId: String,
                          uploadBehaviourId: String,
                          ocrApiUrl: String,
                          snap3ClientKey: String,
                          snap3ClientSecret: String)

class ConfigLoader @Inject()(config: Configuration) {
  private val P3_API_DOMAIN_ACCOUNT = config.get[String]("p3.api.domain.account")

  private val P3_API_DOMAIN_INGESTOR = config.get[String]("p3.api.domain.ingestor")

  private val P3_API_CLIENT_KEY = config.get[String]("p3.api.client.key")
  private val P3_API_DOMAIN_EVENT = config.get[String]("p3.api.domain.event")

  private val GOOGLE_CAPTCHA_SECRET = config.get[String]("google.captcha.secret")

  private val SIGNUP_BEHAVIOUR_ID = config.get[String]("sign.up.behavior.id")
  private val LOGIN_BEHAVIOUR_ID = config.get[String]("sign.in.behavior.id")
  private val UPLOAD_BEHAVIOUR_ID = config.get[String]("upload.behavior.id")

  private val P3_API_CAMPAIGN_ID = config.get[String]("p3.api.campaign.id")

  private val OCR_API_URL = config.get[String]("snap3.api.domain")
  private val SNAP3_CLIENT_KEY = config.get[String]("snap3.api.client.key")
  private val SNAP3_CLIENT_SECRET = config.get[String]("snap3.api.client.secret")

  def load: PlatformConfig = {
    PlatformConfig(P3_API_CLIENT_KEY,
      P3_API_DOMAIN_ACCOUNT,
      P3_API_DOMAIN_EVENT,
      P3_API_DOMAIN_INGESTOR,
      GOOGLE_CAPTCHA_SECRET,
      P3_API_CAMPAIGN_ID,
      SIGNUP_BEHAVIOUR_ID,
      LOGIN_BEHAVIOUR_ID,
      UPLOAD_BEHAVIOUR_ID,
      OCR_API_URL,
      SNAP3_CLIENT_KEY,
      SNAP3_CLIENT_SECRET)
  }
}
