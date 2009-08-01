package net.liftweb.http.provider

import java.io.{OutputStream}

trait HTTPResponse {

  def addCookies(cookies: List[HTTPCookie])

  def encodeURL(url: String): String

  def encodeRedirectURL(url: String): String

  def addHeaders(headers: List[HTTPParam])

  def setStatus(status: Int)

  def outputStream: OutputStream
}
