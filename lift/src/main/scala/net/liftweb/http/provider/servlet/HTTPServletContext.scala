package net.liftweb.http.provider.servlet

import net.liftweb.http.provider._
import javax.servlet.{ServletContext}
import java.net.URL
import java.io.InputStream

class HTTPServletContext(ctx: ServletContext) extends HTTPServiceContext {

  def path: String = ctx.getContextPath

  def resource(path: String): URL = ctx getResource path

  def resourceAsStream(path: String): InputStream  = ctx getResourceAsStream path

  def mimeType(path: String) = ctx getMimeType path

}
