package net.liftweb.http.provider

import _root_.java.io.{InputStream}
import _root_.java.util.{Locale}
import net.liftweb.util.{Box}

trait HTTPRequest {

  def cookies: List[HTTPCookie]

  def authType:String

  def header(name: String): String

  def headers(name: String): List[String]

  def headers: List[HTTPParam]

  def path: String

  def contextPath: String

  def contentType: String

  def uri: String

  def queryString: String

  def param(name: String): List[String]

  def params: List[HTTPParam]

  def paramNames: List[String]

  def session: HTTPServiceSession

  def remoteAdress: String

  def remotePort: Int

  def remoteHost: String

  def serverName: String

  def scheme: String

  def serverPort: Int

  def method: String

  def hasSuspendResumeSupport_? : Option[Any]

  def suspend(timeout: Long): Nothing

  def resume(what: AnyRef): Unit

  def inputStream: InputStream

  def multipartContent_? : Boolean

  def extractFiles: List[ParamHolder]

  def locale: Locale

}
