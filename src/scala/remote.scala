/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2002-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |                                         **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: remote.scala 16894 2009-01-13 13:09:41Z cunei $


package scala

/**
 * An annotation that designates the class to which it is applied as remotable.
 *
 * @see Method <a href="ScalaObject.html#$tag()">$tag</a> in trait
 *      <a href="ScalaObject.html">scala.ScalaObject</a>.
 */
class remote extends StaticAnnotation {}
