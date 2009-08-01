package net.liftweb.http.provider

import net.liftweb.util._
import _root_.java.util.{Locale, ResourceBundle}
import Helpers._

trait HTTPProvider {
  private var actualServlet: LiftServlet = _

  def context: HTTPServiceContext

  def terminate {
    if (actualServlet != null) {
      actualServlet.destroy
      actualServlet = null
    }
  }

  def service[T](req: HTTPRequest, resp: HTTPResponse)(chain : => T) = {
    tryo {
       LiftRules.early.toList.foreach(_(req))
    }

    val newReq = Req(req, LiftRules.rewriteTable(req), System.nanoTime)

    URLRewriter.doWith(url => NamedPF.applyBox(resp.encodeURL(url), LiftRules.urlDecorate.toList) openOr resp.encodeURL(url)) {
      if (!(isLiftRequest_?(newReq) && actualServlet.service(newReq, resp))) {
        chain
      }
    }
  }
  
  /**
   * Executes Lift's Boot
   */
  def bootLift(loader : Box[String]) : Unit =
  {
    try
    {
      val b : Bootable = loader.map(b => Class.forName(b).newInstance.asInstanceOf[Bootable]) openOr DefaultBootstrap
      preBoot
      b.boot
      postBoot

      actualServlet = new LiftServlet(context)
      actualServlet.init

    } catch {
      case e => Log.error("Failed to Boot", e); None
    }
  }

  private def preBoot() {
    LiftRules.dispatch.prepend(NamedPF("Classpath service") {
        case r @ Req(mainPath :: subPath, suffx, _) if (mainPath == LiftRules.resourceServerPath) =>
          ResourceServer.findResourceInClasspath(r, r.path.wholePath.drop(1))
      })
  }

  private def postBoot {
    try {
      ResourceBundle getBundle (LiftRules.liftCoreResourceName)

      if (Props.productionMode && LiftRules.templateCache.isEmpty) {
        // Since we're in productin mode and user did not explicitely set any template caching, we're setting it
        LiftRules.templateCache = Full(InMemoryCache(500))
      }
    } catch {
      case _ => Log.error("LiftWeb core resource bundle for locale " + Locale.getDefault() + ", was not found ! ")
    } finally {
      LiftRules.doneBoot = true;
    }
  }


  //This function tells you wether a resource exists or not, could probably be better
  private def liftHandled(in: String): Boolean = (in.indexOf(".") == -1) || in.endsWith(".html") || in.endsWith(".xhtml") ||
  in.endsWith(".htm") ||
  in.endsWith(".xml") || in.endsWith(".liftjs") || in.endsWith(".liftcss")

  /**
   * Tests if a request should be handled by Lift or passed to the container to be executed by other potential filters or servlets.
   */
  def isLiftRequest_?(session: Req): Boolean = {
    NamedPF.applyBox(session, LiftRules.liftRequest.toList) match {
      case Full(b) => b
      case _ =>  session.path.endSlash ||
        (session.path.wholePath.takeRight(1) match
         {case Nil => true case x :: xs => liftHandled(x)}) ||
        context.resource(session.uri) == null
    }
  }

}
