package utilities

import play.api.i18n.Lang

object Constants {

  val SIGNUP_ACTION_TYPE = "signup"
  val SIGNUP_CHANNEL = "microsite"
  val SIGNUP_OBJECT_TYPE = "page"

  val LOGIN_ACTION_TYPE = "login"
  val LOGIN_CHANNEL = "microsite"
  val LOGIN_OBJECT_TYPE = "page"

  val RECEIPT_UPLOAD_CHANNEL = "microsite"
  val RECEIPT_UPLOAD_ACTION_TYPE = "upload"
  val RECEIPT_UPLOAD_OBJECT_TYPE = "page"

  def genMonth(lang: Lang): Seq[(String, String)] =
    if (lang.language == "en") {
      ("01", "Jan") ::
        ("02", "Feb") ::
        ("03", "Mar") ::
        ("04", "Apr") ::
        ("05", "May") ::
        ("06", "Jun") ::
        ("07", "Jul") ::
        ("08", "Aug") ::
        ("09", "Sept") ::
        ("10", "Oct") ::
        ("11", "Nov") ::
        ("12", "Dec") ::
        Nil
    } else {
      ("01", "janv") ::
        ("02", "févr") ::
        ("03", "mars") ::
        ("04", "avril") ::
        ("05", "mai") ::
        ("06", "juin") ::
        ("07", "juil") ::
        ("08", "août") ::
        ("09", "sept") ::
        ("10", "oct") ::
        ("11", "nov") ::
        ("12", "déc") ::
        Nil
    }


  val GEN_DATE: Seq[(String, String)] = {
    var seqDate: Seq[(String, String)] = Nil
    for (date <- 1 to 31) {
      seqDate = seqDate :+ ((date.toString, date.toString): (String, String))
    }
    seqDate
  }

  val GEN_YEAR: Seq[(String, String)] = {
    var seqYear: Seq[(String, String)] = Nil
    for (year <- 2000 to 1947 by -1) {
      seqYear = seqYear :+ ((year.toString, year.toString): (String, String))
    }
    seqYear
  }

}
