package net.liftweb.http.provider

import net.liftweb.util.{Box, Empty, Full}

object HTTPCookie {

  def apply(name: String, value: String) = new HTTPCookie(name, Full(value), Empty, Empty, Empty, Empty, Empty)
}

case class HTTPCookie(name: String,
                      value: Box[String],
                      domain: Box[String],
                      path: Box[String],
                      maxAge: Box[Int],
                      version: Box[Int],
                      secure_? : Box[Boolean]) extends java.lang.Cloneable {

  override def clone(): HTTPCookie = {
    super.clone()
    new HTTPCookie(name, value, domain, path, maxAge, version, secure_?)
  }

  def setValue(newValue: String): HTTPCookie = new HTTPCookie(name, Box !! newValue, domain, path, maxAge, version, secure_?)
  def setDomain(newDomain: String): HTTPCookie = new HTTPCookie(name, value, Box !! newDomain, path, maxAge, version, secure_?)
  def setPath(newPath: String): HTTPCookie = new HTTPCookie(name, value, domain, Box !! newPath, maxAge, version, secure_?)
  def setMaxAge(newMaxAge: Int): HTTPCookie = new HTTPCookie(name, value, domain, path, Box !! newMaxAge, version, secure_?)
  def setVersion(newVersion: Int): HTTPCookie = new HTTPCookie(name, value, domain, path, maxAge, Box !! newVersion, secure_?)
  def setSecure(newSecure: Boolean): HTTPCookie = new HTTPCookie(name, value, domain, path, maxAge, version, Box !! newSecure)


}



