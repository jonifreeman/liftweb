package net.liftweb.http.provider

import java.io.{InputStream}
import java.net.{URL}

trait HTTPServiceContext {

  def path: String

  def resource(path: String): URL

  def resourceAsStream(path: String): InputStream

  def mimeType(path: String): String
}
