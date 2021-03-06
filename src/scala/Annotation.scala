/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2006-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: Annotation.scala 16894 2009-01-13 13:09:41Z cunei $


package scala

/** <p>
 *   A base class for annotations. Annotations extending this class directly
 *   are not preserved for the Scala type checker and are also not stored
 *   as Java annotations in classfiles. To enable either or both of these,
 *   one needs to inherit from
 *   <a href="StaticAnnotation.html"><code>StaticAnnotation</code></a> or/and
 *   <a href="ClassfileAnnotation.html"><code>ClassfileAnnotation</code></a>.
 *  </p>
 *
 *  @author  Martin Odersky
 *  @version 1.1, 2/02/2007
 */
abstract class Annotation {}
