package net.liftweb.mapper.view

import net.liftweb.http.{SHtml, S, DispatchSnippet}
import S.?

import net.liftweb.util.{Box, Full, Empty, Helpers, BindHelpers}
import Helpers._

import net.liftweb.mapper.{Mapper,
                           MetaMapper,
                           LongKeyedMetaMapper,
                           MappedField}

import Util._


import scala.xml.{NodeSeq, Text}

/**
 * Keeps track of pending adds to and removes from a list of mappers.
 * Supports in-memory sorting by a field.
 * Usage: override metaMapper with a MetaMapper instance, call sortBy
 * to specify the field to sort by. If it is already sorted by that
 * field it will sort descending, otherwise ascending.
 * Call save to actualize changes.
 * @author nafg
 */
trait ItemsList[T <: Mapper[T]] {
  /**
   * The MetaMapper that provides create and findAll functionality etc.
   * Must itself be a T (the mapper type it represents)
   */
  def metaMapper: T with MetaMapper[T]
  
  var current: List[T] = Nil
  var added: List[T] = Nil
  var removed: List[T] = Nil
  import scala.util.Sorting._
  def items: Seq[T] = {
    val unsorted: List[T] = current ++ added filter {i => removed.forall(i.ne)}
    sortField match {
      case None =>
        unsorted
      case Some(field) =>
        unsorted.sort {
          (a, b) => ((field.actualField(a).is: Any, field.actualField(b).is: Any) match {
            case (aval: String, bval: String) => aval.toLowerCase < bval.toLowerCase
            case (aval: Ordered[Any], bval: Ordered[Any]) => aval < bval
            case (aval, bval) => println(aval.asInstanceOf[AnyRef].getClass);aval.toString < bval.toString
          }) match {
            case cmp =>
              if(ascending) cmp else !cmp
          }
        }
    }
  }
  def add {
    added = metaMapper.create :: added
  }
  def remove(i: T) {
    removed = i :: removed
  }
  def reload {
    current = metaMapper.findAll
    added = Nil
    removed = Nil
  }
  def save {
    val toSave = (added++current) filter {i=>removed.forall(i.ne)}
    val toRemove = removed filter {_.saved_?}
    toSave.forall(_.save)
    toRemove.forall(_.delete_!)
    reload
  }
  
  var sortField: Option[MappedField[_, T]] = None
  var ascending = true
  
  def sortBy(field: MappedField[_, T]) = (sortField, ascending) match {
    case (Some(f), true) if f eq field =>
      ascending = false
    case _ | null =>
      sortField = Some(field)
      ascending = true
  }
  
  reload
}


/**
 * Holds a registry of TableEditor delegates
 * Call TableEditor.registerTable(name_to_use_in_view, meta_mapper_for_the_table, display_title)
 * in Boot after DB.defineConnectionManager.
 * Referencing TableEditor triggers registering its snippet package and enabling
 * the provided template, /classpath/tableeditor/default.html.
 * @author nafg
 */
object TableEditor {
  net.liftweb.http.LiftRules.addToPackages("net.liftweb.mapper.view")
  
  private[view] val map = new scala.collection.mutable.HashMap[String, TableEditorImpl[_]]
  def registerTable[T<:Mapper[T]](name: String, meta: T with MetaMapper[T], title: String) =
    map(name) = new TableEditorImpl(title, meta)
}

package snippet {
  /**
   * This is the snippet that the view references.
   * @author nafg
   */
  class TableEditor extends DispatchSnippet {
    private def getInstance: Box[TableEditorImpl[_]] = S.attr("table").map(TableEditor.map(_))
    def dispatch = {
      case "edit" =>
        val o = getInstance.open_!
        o.edit _
    }
  }
}

/**
 * This class contains the actual view binding against a ItemsList.
 * @author nafg
 */
protected class TableEditorImpl[T <: Mapper[T]](title: String, meta: T with MetaMapper[T]) {  
  var items = new ItemsList[T] {
    def metaMapper = meta
  } 
    
  
  def edit(xhtml: NodeSeq) = {
    xhtml.bind("header",
         "fields" -> eachField[T](
             items.metaMapper, (f: MappedField[_,T]) => Seq(
               "name" -> SHtml.link(S.uri, ()=>items.sortBy(f), Text(capify(f.displayName)))
             )
         )
    ).bind("table",
         "title" -> title,
         "insertBtn" -> SHtml.submit(?("Insert"), ()=>{items.add}),
         "items" -> ((ns:NodeSeq)=>NodeSeq.fromSeq(items.items.flatMap {i =>
           bind("item",
                ns,
                "fields" -> eachField(i, (f: MappedField[_,T]) => Seq(
                  "form" -> f.toForm
                )),
                "removeBtn" -> SHtml.submit(?("Remove"), ()=>items.remove(i))
           )
         })),
         "saveBtn" -> SHtml.submit(?("Save"), ()=>items.save)
    )
  }
}
