/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2006-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: PagedSeq.scala 14497 2008-04-04 12:09:06Z washburn $


package scala.collection.immutable

import java.io._
import util.matching.Regex


/** The PagedSeq object defines a lazy implementations of 
 *  a random access sequence. 
 */  
object PagedSeq {
  final val UndeterminedEnd = Math.MAX_INT

  /** Constructs a character sequence from a character iterator */
  def fromIterator[T](source: Iterator[T]): PagedSeq[T] = 
    new PagedSeq[T]((data: Array[T], start: Int, len: Int) => {
      var i = 0
      while (i < len && source.hasNext) {
        data(start + i) = source.next
        i += 1
      }
      if (i == 0) -1 else i
    })

  /** Constructs a character sequence from a character iterable */
  def fromIterable[T](source: Iterable[T]): PagedSeq[T] = 
    fromIterator(source.elements)

  /** Constructs a character sequence from a string iterator */
  def fromStrings(source: Iterator[String]): PagedSeq[Char] = {
    var current: String = ""
    def more(data: Array[Char], start: Int, len: Int): Int =
      if (current.length != 0) {
        val cnt = current.length min len
        current.getChars(0, cnt, data, start)
        current = current.substring(cnt)
        if (cnt == len) cnt
        else (more(data, start + cnt, len - cnt) max 0) + cnt
      } else if (source.hasNext) {
        current = source.next
        more(data, start, len)
      } else -1
    new PagedSeq(more(_: Array[Char], _: Int, _: Int))
  }

  /** Constructs a character sequence from a string iterable */
  def fromStrings(source: Iterable[String]): PagedSeq[Char] = 
    fromStrings(source.elements)

  /** Constructs a character sequence from a line iterator
   *  Lines do not contain trailing `\n' characters; The method inserts
   *  a line separator `\n' between any two lines in the sequence.
   */
  def fromLines(source: Iterator[String]): PagedSeq[Char] = {
    var isFirst = true
    fromStrings(source map { line =>
      if (isFirst) line 
      else { 
        isFirst = false
        "\n"+line
      }
    }) 
  }                    

  /** Constructs a character sequence from a line iterable
   *  Lines do not contain trailing `\n' characters; The method inserts
   *  a line separator `\n' between any two lines in the sequence.
   */
  def fromLines(source: Iterable[String]): PagedSeq[Char] =
    fromLines(source.elements)

  /** Constructs a character sequence from an input reader
   */
  def fromReader(source: Reader): PagedSeq[Char] =
    new PagedSeq(source.read(_: Array[Char], _: Int, _: Int))

  /** Constructs a character sequence from an input file
   */
  def fromFile(source: File): PagedSeq[Char] = 
    fromReader(new FileReader(source))
    
  /** Constructs a character sequence from a file with given name
   */
  def fromFile(source: String): PagedSeq[Char] = 
    fromFile(new File(source))

  /** Constructs a character sequence from a scala.io.Source value
   */
  def fromSource(source: io.Source) = 
    fromLines(source.getLines)
}


import PagedSeq._

/** An implementation of lazily computed sequences, where elements are stored
 *  in ``pages'', i.e. arrays of fixed size.
 *
 * @author Martin Odersky 
 */
class PagedSeq[T] protected (more: (Array[T], Int, Int) => Int, 
                             first: Page[T], start: Int, end: Int) extends RandomAccessSeq[T] {

  /**  A paged sequence is constructed from a method that produces more characters when asked.
   *  The producer method is analogous to the read method in java.io.Reader.
   *  It takes three parameters: an array of characters, a start index, and an end index.
   *  It should try to fill the array between start and end indices (not including end index).
   *  It returns the number of characters produced, or -1 if end of logical input stream was reached
   *  before any character was read.
   */  
  def this(more: (Array[T], Int, Int) => Int) = this(more, new Page[T](0), 0, UndeterminedEnd)
  
  private var current: Page[T] = first

  private def latest = first.latest

  private def addMore() = latest.addMore(more)

  private def page(absindex: Int) = {
    if (absindex < current.start) 
      current = first
    while (absindex >= current.end && current.next != null)
      current = current.next
    while (absindex >= current.end && !current.isLast) {
      current = addMore()
    }
    current
  }

  /** The length of the character sequence
   *  Note: calling this method will force sequence to be read until the end.
   */
  def length: Int = {
    while (!latest.isLast) addMore()
    (latest.end min end) - start
  }

  /** The character at position `index'. 
   */
  def apply(index: Int) =
    if (isDefinedAt(index)) page(index + start)(index + start)
    else throw new IndexOutOfBoundsException(index.toString)

  /** Is character sequence defined at `index'?
   *  Unlike `length' this operation does not force reading
   *  a lazy sequence to the end.
   */
  override def isDefinedAt(index: Int) = 
    index >= 0 && index < end - start && {
      val p = page(index + start); index + start < p.end
    }
    
  /** the subsequence from index `start' up to and excluding 
   *  the minimum of index `end' and the length of the current sequence.
   */
  override def slice(_start: Int, _end: Int) = {
    page(start)
    val s = start + _start
    val e = if (_end == UndeterminedEnd) _end else start + _end
    var f = first
    while (f.end <= s && !f.isLast) f = f.next
    new PagedSeq(more, f, s, e)
  }

  /** the subsequence from index `start' up to the  
   *  length of the current sequence.
   */
  override def slice(start: Int) = slice(start, UndeterminedEnd)

  /** Convert sequence to string */
  override def toString = {
    val buf = new StringBuilder
    for (ch <- elements) buf append ch
    buf.toString
  }
}


/** Page containing up to PageSize characters of the input sequence. 
 */
private class Page[T](val num: Int) {

  private final val PageSize = 4096

  /** The next page in the sequence */
  var next  : Page[T] = null

  /** A later page in the sequence, serves a cachae for pointing to last page */
  var later : Page[T] = this

  /** The number of characters read into this page */
  var filled: Int = 0

  /** Is this page the permamnently last one in the sequence? Only true once `more'
   *  method has returned -1 to signal end of input. */
  var isLast: Boolean = false

  /** The character array */
  final val data = new Array[T](PageSize)

  /** The index of the first character in this page relative to the whole sequence */ 
  final def start = num * PageSize

  /** The index of the character following the last charcater in this page relative 
   *  to the whole sequence */ 
  final def end = start + filled

  /** The currently last page in the sequence; might change as more charcaters are appended */
  final def latest: Page[T] = {
    if (later.next != null) later = later.next.latest
    later
  }

  /** The character at given sequence index. 
   *  That index is relative to the whole sequence, not the page. */
  def apply(index: Int) = {
    if (index < start || index - start >= filled) throw new IndexOutOfBoundsException(index.toString)
    data(index - start)
  }

  /** produces more characters by calling `more' and appends them on the current page, 
   *  or fills a subsequent page if current page is full 
   *  pre: if current page is full, it is the last one in the sequence.
   */
  final def addMore(more: (Array[T], Int, Int) => Int): Page[T] =
    if (filled == PageSize) {
      next = new Page[T](num + 1)
      next.addMore(more)
    } else {
      val count = more(data, filled, PageSize - filled) 
      if (count < 0) isLast = true
      else filled += count
      this
    }
}
