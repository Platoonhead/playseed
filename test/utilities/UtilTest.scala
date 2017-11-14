package utilities

import org.scalatestplus.play.PlaySpec

class UtilTest extends PlaySpec {
  val utilObj = new Util

  "test md5Hash" in {
    val result = utilObj.md5Hash("Image Title" + "-" + "currentTime")

    result must equal("3v636g1s5mz4d2i6h3th1h1q113e3y")
  }

}
