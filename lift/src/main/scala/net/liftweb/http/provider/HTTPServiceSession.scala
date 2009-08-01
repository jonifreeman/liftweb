package net.liftweb.http.provider

trait HTTPServiceSession {

  def sessionID: String

  def link(liftSession: LiftSession)

  def unlink(liftSession: LiftSession)

  def maxInactiveInterval: Long

  def setMaxInactiveInterval(interval: Long)

  def lastAccessedTime: Long

  def setAttribute(name: String, value: Any)

  def attribute(name: String): Any

  def removeAttribute(name: String): Unit
}
