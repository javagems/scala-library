/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2002-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org                **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: ByteRef.java 16881 2009-01-09 16:28:11Z cunei $


package scala.runtime;


public class ByteRef implements java.io.Serializable {
    private static final long serialVersionUID = -100666928446877072L;

    public byte elem;
    public ByteRef(byte elem) { this.elem = elem; }
    public String toString() { return Byte.toString(elem); }
}
