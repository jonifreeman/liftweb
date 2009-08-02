package net.liftweb.http.provider.servlet

import _root_.javax.servlet.{ServletContext}
import _root_.java.net.URL
import _root_.java.io.InputStream

import net.liftweb.http.provider._
import net.liftweb.util._

class HTTPServletContext(ctx: ServletContext) extends HTTPServiceContext {

  def path: String = ctx.getContextPath

  def resource(path: String): URL = ctx getResource path

  def resourceAsStream(path: String): InputStream  = ctx getResourceAsStream path

  def mimeType(path: String) = Box !! ctx.getMimeType(path)

}
