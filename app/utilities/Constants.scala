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

  val STATES_REGISTER_WITHOUT_PURCHASE: List[String] =
    List("AL", "CA", "HI", "IL", "IN", "ME", "MO", "NC", "TX", "HIH", "HIM", "HIK")

  val STATES_REGISTER_WITH_PURCHASE: List[String] =
    List("AK", "AZ", "AR", "CO", "CT", "DE",
      "DC", "FL", "GA", "ID", "IA", "KS",
      "KY", "LA", "MD", "MA", "MI", "MS",
      "MT", "NE", "NV", "NH", "NM", "NY",
      "ND", "OH", "OK", "RI", "SC", "TN",
      "VT", "VA", "WA", "WV", "WI", "WY")

  val STATES_NOT_PERMITTED: List[String] = List("MN", "NJ", "OR", "PA", "SD", "UT")

  val GEN_PROVINCE: Seq[(String, String)] =
    ("AL", "Alabama") ::
      ("AK", "Alaska") ::
      ("AZ", "Arizona") ::
      ("AR", "Arkansas") ::
      ("CA", "California") ::
      ("CO", "Colorado") ::
      ("CT", "Connecticut") ::
      ("DE", "Delaware") ::
      ("DC", "District of Columbia") ::
      ("FL", "Florida") ::
      ("GA", "Georgia") ::
      ("HI", "Hawaii (Country)") ::
      ("HIH", "Honolulu (Country)") ::
      ("ID", "Idaho") ::
      ("IL", "Illinois") ::
      ("IN", "Indiana") ::
      ("IA", "Iowa") ::
      ("KS", "Kansas") ::
      ("HIK", "Kauai (Country)") ::
      ("KY", "Kentucky") ::
      ("LA", "Louisiana") ::
      ("ME", "Maine") ::
      ("MD", "Maryland") ::
      ("MA", "Massachusetts") ::
      ("HIM", "Maui (Country)") ::
      ("MI", "Michigan") ::
      ("MN", "Minnesota") ::
      ("MS", "Mississippi") ::
      ("MO", "Missouri") ::
      ("MT", "Montana") ::
      ("NE", "Nebraska") ::
      ("NV", "Nevada") ::
      ("NH", "New Hampshire") ::
      ("NJ", "New Jersey") ::
      ("NM", "New Mexico") ::
      ("NY", "New York") ::
      ("NC", "North Carolina") ::
      ("ND", "North Dakota") ::
      ("OH", "Ohio") ::
      ("OK", "Oklahoma") ::
      ("OR", "Oregon") ::
      ("PA", "Pennsylvania") ::
      ("RI", "Rhode Island") ::
      ("SC", "South Carolina") ::
      ("SD", "South Dakota") ::
      ("TN", "Tennessee") ::
      ("TX", "Texas") ::
      ("UT", "Utah") ::
      ("VT", "Vermont") ::
      ("VA", "Virginia") ::
      ("WA", "Washington") ::
      ("WV", "West Virginia") ::
      ("WI", "Wisconsin") ::
      ("WY", "Wyoming") ::
      Nil
}
