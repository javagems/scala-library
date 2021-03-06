/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2006-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: Lexical.scala 16894 2009-01-13 13:09:41Z cunei $


package scala.util.parsing.combinator.lexical

import scala.util.parsing.syntax._
import scala.util.parsing.input.CharArrayReader.EofCh

/** <p>
 *    This component complements the <code>Scanners</code> component with
 *    common operations for lexical parsers.
 *  </p>
 *  <p>
 *   {@see StdLexical} for a concrete implementation for a simple, Scala-like
 *   language.
 *  </p>
 *
 * @author Martin Odersky, Adriaan Moors
 */
abstract class Lexical extends Scanners with Tokens {

  /** A character-parser that matches a letter (and returns it)*/
  def letter = elem("letter", _.isLetter)

  /** A character-parser that matches a digit (and returns it)*/  
  def digit = elem("digit", _.isDigit)

  /** A character-parser that matches any character except the ones given in `cs' (and returns it)*/  
  def chrExcept(cs: Char*) = elem("", ch => (cs forall (ch !=)))

  /** A character-parser that matches a white-space character (and returns it)*/  
  def whitespaceChar = elem("space char", ch => ch <= ' ' && ch != EofCh)
}
