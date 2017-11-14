package utilities

import java.security.MessageDigest

import org.joda.time.{DateTime, DateTimeZone}

class Util {

  val digest = MessageDigest.getInstance("MD5")
  digest.reset()

  def encode(b: Byte): String = java.lang.Integer.toString(b & 0xff, 36)

  def md5Hash(input: String): String = {
    val result = ("" /: digest.digest(input.getBytes("UTF-8"))) (_ + encode(_))
    result
  }

  def getPacificTime: DateTime = {
    new DateTime()
  }
}

object Util extends Util
