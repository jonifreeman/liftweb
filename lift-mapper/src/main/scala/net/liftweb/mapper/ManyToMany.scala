package net.liftweb.mapper


/**
 * Add this trait to a Mapper to add support for many-to-many relationships
 * @author nafg
 */
trait ManyToMany[K,T<:KeyedMapper[K, T]] extends KeyedMapper[K,T] { this: T =>
  private var manyToManyFields: List[MappedManyToMany[_,_,_]] = Nil
  
  /**
   * An override for save to propagate the save to all children
   * of this parent.
   * Returns false as soon as the parent or a one-to-many field returns false.
   * If they are all successful returns true.
   */
  override def save = {
    super.save &&
      manyToManyFields.forall(_.save)
  }
  
  /**
   * An override for delete_! to propogate the deletion to all children
   * of this parent.
   * Returns false as soon as the parent or a one-to-many field returns false.
   * If they are all successful returns true.
   */
  override def delete_! = {
    super.delete_! && 
      manyToManyFields.forall( _.delete_!)
  }

  
  /**
   * This is the base class to use for fields that track many-to-many relationships.
   * @param joinMeta The singleton of the join table
   * @param thisField The foreign key in the join table that refers to this mapper's primaryKey.
   * @param otherField The foreign key in the join table that refers to the other mapper's primaryKey
   * @param otherMeta The singleton of the other mapper
   * @param qp Any QueryParams to limit entries in the join table (other than matching thisField to primaryKey)
   * To limit children based on fields in the other table (not the join table), it is currently necessary
   * to point the join mapper to a view which pulls the join table's fields as well as fields of the other table.
   */
  class MappedManyToMany[O<:Mapper[O], K2, T2 <: KeyedMapper[K2,T2]](
    val joinMeta: MetaMapper[O],
    thisField: MappedForeignKey[K,O,T],
    val otherField: MappedForeignKey[K2, O, T2],
    val otherMeta: MetaMapper[T2],
    val qp: QueryParam[O]*) extends scala.collection.mutable.Buffer[T2] {
    
    def field(join: O) = thisField.actualField(join).asInstanceOf[MappedForeignKey[K,O,T]]
    
    protected def children: List[T2] =
      joins.map(otherField.actualField(_).asInstanceOf[MappedForeignKey[K2,O,T2]].obj.openOr(error("Broken join")))
    protected var joins: List[O] = _
    protected var removedJoins: List[O] = Nil
    refresh
    manyToManyFields = this :: manyToManyFields
    
    protected def own(e: T2) = {
      joins.find(otherField.actualField(_).is == e.primaryKeyField.is) match {
        case None =>
          removedJoins.find { // first check if we can recycle a removed join
            otherField.actualField(_).is == e.primaryKeyField
          } match {
            case Some(removedJoin) =>
              removedJoins = removedJoins filter removedJoin.ne
              removedJoin // well, noLongerRemovedJoin...
            case None =>
              val newJoin = joinMeta.create
              field(newJoin).set(ManyToMany.this.primaryKeyField)
              otherField.actualField(newJoin).set(e.primaryKeyField)
              newJoin
          }
        case Some(join) =>
          join
      }
    }
    protected def unown(e: T2) = {
      joins.find(otherField.actualField(_).is == e.primaryKeyField.is) match {
        case Some(join) =>
          removedJoins = join :: removedJoins
          val o = otherField.actualField(join)
          o.set(o.defaultValue)
          val f = field(join)
          f.set(f.defaultValue)
          Some(join)
        case None =>
          None
      }
    }
    def all = children

    def readOnly = all
    def length = children.length
    def elements = children.elements
    protected def childAt(n: Int) = children(n)
    def apply(n: Int) = childAt(n)
    def indexOf(e: T2) =
      children.findIndexOf(e eq)


    def +=(elem: T2) {
      joins = joins ++ List(own(elem))
    }
    def +:(elem: T2) = {
      joins ::= own(elem)
      this
    }

    def insertAll(n: Int, iter: Iterable[T2]) {
      //TODO if children uses flatMap n needs to be converted
      val (before, after) = joins.splitAt(n)
      val owned = iter map own
      joins = before ++ owned ++ after
    }

    def update(n: Int, newelem: T2) {
      unown(childAt(n))
      val (before, after) = (joins.take(n), joins.drop(n+1))
      joins = before ++ List(own(newelem)) ++ after
    }

    def remove(n: Int) = {
      val child = childAt(n)
      unown(child) match {
        case Some(join) =>
          joins = joins remove join.eq
        case None =>
      }
      child
    }


    def clear() {
      children foreach unown
      joins = Nil
    }
    
    def refresh = {
      joins = joinMeta.findAll( (By(thisField, primaryKeyField) :: qp.toList): _*)
      all
    }
    
    def save = {
      joins = joins.filter { join =>
          field(join).is ==
            ManyToMany.this.primaryKeyField.is && {
              val f = otherField.actualField(join)
              f.is != f.defaultValue
          }
      }

      removedJoins.forall {_.delete_!} & ( // continue saving even if deleting fails
        children.forall(_.save) &&
          joins.forall(_.save)
      )
    }
    
    def delete_! = {
      removedJoins.forall(_.delete_!) &
        joins.forall(_.delete_!)
    }
  }
}

