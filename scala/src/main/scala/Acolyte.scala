// -*- mode: scala -*-
package acolyte

import java.util.{ ArrayList, List ⇒ JList }
import java.sql.Statement

import scala.language.implicitConversions
import scala.collection.JavaConversions

import acolyte.ParameterMetaData.ParameterDef
import acolyte.StatementHandler.Parameter
import acolyte.CompositeHandler.{ QueryHandler, UpdateHandler }
import acolyte.RowList.Column

// Acolyte DSL
object Acolyte extends ScalaRowLists {
  def handleStatement = new CompositeHandler()

  def connection(h: ConnectionHandler) = Driver.connection(h)
  def connection(h: StatementHandler) = Driver.connection(h)

  implicit def CompositeHandlerAsScala(h: CompositeHandler): ScalaCompositeHandler = new ScalaCompositeHandler(h)

  implicit def ResultRowAsScala[R <: Row](r: R): ScalaResultRow =
    new ScalaResultRow(r)

  implicit def PairAsColumn[T](c: (Class[T], String)): Column[T] =
    Column.defineCol(c._1, c._2)

}

case class Execution(
  sql: String,
  parameters: List[ExecutedParameter])

case class ExecutedParameter(
  value: Any,
  definition: ParameterDef) {

  override lazy val toString = s"Param($value, ${definition.sqlTypeName})"
}

final class ScalaCompositeHandler(
    b: CompositeHandler) extends CompositeHandler {

  def withUpdateHandler(h: Execution ⇒ Int): CompositeHandler = {
    b.withUpdateHandler(new UpdateHandler {
      def apply(sql: String, p: JList[Parameter]): Int = {
        val ps = JavaConversions.collectionAsScalaIterable(p).
          foldLeft(Nil: List[ExecutedParameter]) { (l, t) ⇒
            l :+ ExecutedParameter(t.right, t.left)
          }

        h(Execution(sql, ps))
      }
    })
  }

  def withQueryHandler(h: Execution ⇒ Result): CompositeHandler = {
    b.withQueryHandler(new QueryHandler {
      def apply(sql: String, p: JList[Parameter]): Result = {
        val ps = JavaConversions.collectionAsScalaIterable(p).
          foldLeft(Nil: List[ExecutedParameter]) { (l, t) ⇒
            l :+ ExecutedParameter(t.right, t.left)
          }

        h(Execution(sql, ps))
      }
    })
  }
}

final class ScalaResultRow(r: Row) extends Row {
  lazy val cells = r.cells

  lazy val list: List[Any] =
    JavaConversions.iterableAsScalaIterable(cells).foldLeft(List[Any]()) {
      (l, v) ⇒ l :+ v
    }

}

final class ScalaRowList[L <: RowList[R], R <: Row](l: L) {
  def :+(row: R): L = l.append(row).asInstanceOf[L]

  def withLabels(labels: (Int, String)*): L =
    labels.foldLeft(l) { (l, t) ⇒ l.withLabel(t._1, t._2).asInstanceOf[L] }

}
