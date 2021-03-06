/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2002-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: DefaultMarkupHandler.scala 16894 2009-01-13 13:09:41Z cunei $


package scala.xml.parsing


/** default implemenation of markup handler always returns NodeSeq.Empty */
abstract class DefaultMarkupHandler extends MarkupHandler {

  def elem(pos: Int, pre: String, label: String, attrs: MetaData,
           scope:NamespaceBinding, args: NodeSeq) = NodeSeq.Empty

  def procInstr(pos: Int, target: String, txt: String) = NodeSeq.Empty

  def comment(pos: Int, comment: String ): NodeSeq = NodeSeq.Empty

  def entityRef(pos: Int, n: String) = NodeSeq.Empty

  def text(pos: Int, txt:String) = NodeSeq.Empty

}
