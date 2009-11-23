/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2002-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: IntRef.java 16881 2009-01-09 16:28:11Z cunei $


package scala.runtime;


public class IntRef implements java.io.Serializable {
    private static final long serialVersionUID = 1488197132022872888L;

    public int elem;
    public IntRef(int elem) { this.elem = elem; }
    public String toString() { return Integer.toString(elem); }
}