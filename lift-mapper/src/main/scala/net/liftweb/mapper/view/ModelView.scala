package net.liftweb.mapper.view

import net.liftweb.http.{StatefulSnippet, S}
import S.?
import net.liftweb.util.Helpers._

import net.liftweb.mapper.{Mapper, MetaMapper}

import scala.xml.{NodeSeq, Text}


/**
 * A snippet that can list and edit items of a particular Mapper class
 * @author nafg
 */
trait ModelSnippet[T <: Mapper[T]] extends StatefulSnippet {
  /**
   * The instance of ModelView that wraps the currently loaded entity
   */
  val view: ModelView[T]
  /**
   * The list snippet
   */
  def list(ns: NodeSeq): NodeSeq
  /**
   * The edit snippet
   */
  def edit(ns: NodeSeq): NodeSeq
  
  def load(entity: T) = view.entity = entity

  def dispatch = Map(
    "list" ->       list _,
    "edit" ->       edit _,
    "newOrEdit" ->  view.newOrEdit _
  )
}


/**
 * A wrapper around a Mapper that provides view-related utilities. Belongs to a parent snippet.
 * @author nafg
 */
abstract class ModelView[T <: Mapper[T]](var entity: T, val snippet: ModelSnippet[T]) {
  /**
   * If Some(string), will redirect to string on a successful save.
   * If None, will load the same page.
   * Defaults to Some("list").
   */
  var redirectOnSave: Option[String] = Some("list")
  /**
   * Loads this entity into the snippet so it can be edited 
   */
  def load = snippet.load(entity)
  
  /**
   * Delete the entity
   */
  def remove =
    entity.delete_!
  /**
   * This function is used as a snippet in the edit view
   * to provide alternate text depending on whether an
   * existing entity is being edited or a new one is being
   * created.
   */
  def newOrEdit(xhtml: NodeSeq) =
    chooseTemplate("if",
                   if(entity.saved_?) "edit" else "new",
                   xhtml)
  
  /**
   * This method checks whether the entity
   * validates; if so it saves it, and if
   * successful redirects to the location
   * specified by redirectOnSave, if any.
   * If save or validation fails, the
   * appropriate message(s) is/are displayed
   * and no redirect is performed.
   */
  def save {
    entity.validate match {
      case Nil =>
        if(entity.save)
          redirectOnSave.foreach(snippet.redirectTo)
        else
          S.error("Save failed")
      case errors =>
        S.error(errors)
      }
  }

  /**
   * returns a string that represents the id, or &lt;new&gt;
   * if the entity is a new entity.
   * If the entity has been saved then the id is determined
   * as follows: If it is a KeyedMapper then it calls toString
   * on the entity's primaryKeyField. Otherwise it
   * calls toString on a field named "id."
   */
  def idString = if(entity.saved_?)
    entity match {
      case e: net.liftweb.mapper.KeyedMapper[_,T] => 
        e.primaryKeyField.toString
      case _ => entity.fieldByName("id").toString
    }
  else
    "<new>"
  
  
  /**
   * Returns a BindParam that contains a link to load and edit this entity
   */
  val editAction = TheBindParam("edit", snippet.link("edit", ()=>load, Text(?("Edit"))))
  /**
   * Returns a BindParam that contains a link to delete this entity
   */
  val removeAction = TheBindParam("remove", snippet.link("list", ()=>remove, Text(?("Remove"))))
  /**
   * Returns a BindParam that binds "name" to the field named "name."
   * If the field has a Full toForm implementation then that is used;
   * otherwise its asHtml is called.
   */
  def edit(name: String) = {
    entity.fieldByName(name).map { (field: net.liftweb.mapper.MappedField[_,_]) =>
      TheBindParam(name, field.toForm.openOr(field.asHtml))
    }.open_!
  }
}
