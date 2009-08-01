package net.liftweb.http.provider


object HTTPParam {
  def apply(name: String, value: String) = new HTTPParam(name, List(value))
}

case class HTTPParam(name: String, values: List[String])