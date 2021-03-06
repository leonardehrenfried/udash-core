package io.udash.properties.seq

import java.util.UUID

import io.udash.properties.single.ReadableProperty
import io.udash.properties.{CallbackSequencer, ModelValue, PropertyCreator}
import io.udash.utils.{Registration, SetRegistration}

import scala.collection.mutable
import scala.scalajs.js

private[properties]
abstract class ZippedSeqPropertyUtils[O] extends ReadableSeqProperty[O, ReadableProperty[O]] {
  override val id: UUID = PropertyCreator.newID()
  override protected[properties] val parent: ReadableProperty[_] = null

  protected final val children: js.Array[ReadableProperty[O]] = js.Array[ReadableProperty[O]]()
  private val structureListeners: mutable.Set[Patch[ReadableProperty[O]] => Any] = mutable.Set()

  protected def update(fromIdx: Int): Unit

  protected final val originListener: Patch[ReadableProperty[_]] => Unit = (patch: Patch[ReadableProperty[_]]) => {
    val idx = patch.idx
    val removed = children.jsSlice(patch.idx, children.length)
    children.splice(idx, children.length - idx)
    update(idx)
    val added = children.jsSlice(patch.idx)
    if (added.length != 0 || removed.length != 0) {
      val mappedPatch = Patch(patch.idx, removed, added, patch.clearsProperty)
      CallbackSequencer.queue(
        s"${this.id.toString}:fireElementsListeners:${patch.hashCode()}",
        () => structureListeners.foreach(_.apply(mappedPatch))
      )
      valueChanged()
    }
  }

  override def get: Seq[O] =
    children.map(_.get)

  override def elemProperties: Seq[ReadableProperty[O]] =
    children

  override def listenStructure(structureListener: (Patch[ReadableProperty[O]]) => Any): Registration = {
    structureListeners += structureListener
    new SetRegistration(structureListeners, structureListener)
  }
}

private[properties]
class ZippedReadableSeqProperty[A, B, O : ModelValue]
                               (s: ReadableSeqProperty[A, ReadableProperty[A]],
                                p: ReadableSeqProperty[B, ReadableProperty[B]],
                                combiner: (A, B) => O)
  extends ZippedSeqPropertyUtils[O] {

  protected final def appendChildren(toCombine: Seq[(ReadableProperty[A], ReadableProperty[B])]): Unit =
    toCombine.foreach { case (x, y) => children.push(x.combine(y, this)(combiner)) }

  protected def update(fromIdx: Int): Unit =
    appendChildren(s.elemProperties.zip(p.elemProperties).drop(fromIdx))

  update(0)
  s.listenStructure(originListener)
  p.listenStructure(originListener)
}

private[properties]
class ZippedAllReadableSeqProperty[A, B, O : ModelValue]
                                  (s: ReadableSeqProperty[A, ReadableProperty[A]],
                                   p: ReadableSeqProperty[B, ReadableProperty[B]],
                                   combiner: (A, B) => O, defaultA: ReadableProperty[A], defaultB: ReadableProperty[B])
  extends ZippedReadableSeqProperty(s, p, combiner) {

  override protected def update(fromIdx: Int): Unit =
    appendChildren(s.elemProperties.zipAll(p.elemProperties, defaultA, defaultB).drop(fromIdx))
}

private[properties]
class ZippedWithIndexReadableSeqProperty[A](s: ReadableSeqProperty[A, ReadableProperty[A]])
  extends ZippedSeqPropertyUtils[(A, Int)] {

  protected final def appendChildren(toCombine: Seq[(ReadableProperty[A], Int)]): Unit =
    toCombine.foreach { case (x, y) => children.push(x.transform(v => (v, y))) }

  protected def update(fromIdx: Int): Unit =
    appendChildren(s.elemProperties.zipWithIndex.drop(fromIdx))

  update(0)
  s.listenStructure(originListener)
}