package scala.collection.immutable;

/**
 * @author David MacIver
 */
private[immutable] object LongMapUtils{
  def zero(i : Long, mask : Long) = (i & mask) == 0;    
  def mask(i : Long, mask : Long) = i & (complement(mask - 1) ^ mask)
  def hasMatch(key : Long, prefix : Long, m : Long) = mask(key, m) == prefix;
  def unsignedCompare(i : Long, j : Long) = (i < j) ^ (i < 0) ^ (j < 0)
  def shorter(m1 : Long, m2 : Long) = unsignedCompare(m2, m1)
  def complement(i : Long) = (-1) ^ i;
  def branchMask(i : Long, j : Long) = highestOneBit(i ^ j);

  def highestOneBit(j : Long) = {
    var i = j;
    i |= (i >>  1);
    i |= (i >>  2);
    i |= (i >>  4);
    i |= (i >>  8);
    i |= (i >> 16);
    i |= (i >> 32);
    i - (i >>> 1);
  }

  def join[T](p1 : Long, t1 : LongMap[T], p2 : Long, t2 : LongMap[T]) : LongMap[T] = {
    val m = branchMask(p1, p2);
    val p = mask(p1, m);
    if (zero(p1, m)) LongMap.Bin(p, m, t1, t2)
    else LongMap.Bin(p, m, t2, t1);
  }
    
  def bin[T](prefix : Long, mask : Long, left : LongMap[T], right : LongMap[T]) : LongMap[T] = (left, right) match {
    case (left, LongMap.Nil) => left;
    case (LongMap.Nil, right) => right;
    case (left, right) => LongMap.Bin(prefix, mask, left, right);
  }
}

import LongMapUtils._

object LongMap{
  def empty[T] : LongMap[T]  = LongMap.Nil;
  def singleton[T](key : Long, value : T) : LongMap[T] = LongMap.Tip(key, value);
  def apply[T](elems : (Long, T)*) : LongMap[T] = 
    elems.foldLeft(empty[T])((x, y) => x.update(y._1, y._2));


  private[immutable] case object Nil extends LongMap[Nothing]{
    override def equals(that : Any) = this eq that.asInstanceOf[AnyRef] 
  };
  private[immutable] case class Tip[+T](key : Long, value : T) extends LongMap[T]{
    def withValue[S](s : S) = 
      if (s.asInstanceOf[AnyRef] eq value.asInstanceOf[AnyRef]) this.asInstanceOf[LongMap.Tip[S]];
      else LongMap.Tip(key, s);
  }
  private[immutable] case class Bin[+T](prefix : Long, mask : Long, left : LongMap[T], right : LongMap[T]) extends LongMap[T]{
    def bin[S](left : LongMap[S], right : LongMap[S]) : LongMap[S] = {
      if ((this.left eq left) && (this.right eq right)) this.asInstanceOf[LongMap.Bin[S]];
      else LongMap.Bin[S](prefix, mask, left, right);
    }
  }

}

import LongMap._

// Iterator over a non-empty LongMap.
private[immutable] abstract class LongMapIterator[V, T](it : LongMap[V]) extends Iterator[T]{

  // Basically this uses a simple stack to emulate conversion over the tree. However 
  // because we know that Longs are only 64 bits we can have at most 64 LongMap.Bins and 
  // one LongMap.Tip sitting on the tree at any point. Therefore we know the maximum stack 
  // depth is 65 
  var index = 0;
  var buffer = new Array[AnyRef](65);
 
  def pop = {
    index -= 1;
    buffer(index).asInstanceOf[LongMap[V]];
  }

  def push(x : LongMap[V]) {
    buffer(index) = x.asInstanceOf[AnyRef];
    index += 1; 
  }
  push(it);

  /**
   * What value do we assign to a tip?
   */
  def valueOf(tip : LongMap.Tip[V]) : T;

  def hasNext = index != 0; 
  final def next : T = 
    pop match {      
      case LongMap.Bin(_,_, t@LongMap.Tip(_, _), right) => {
        push(right);
        valueOf(t);
      }
      case LongMap.Bin(_, _, left, right) => {
        push(right);
        push(left);
        next;
      }
      case t@LongMap.Tip(_, _) => valueOf(t);
      // This should never happen. We don't allow LongMap.Nil in subtrees of the LongMap
      // and don't return an LongMapIterator for LongMap.Nil.
      case LongMap.Nil => error("Empty maps not allowed as subtrees");
    }    
}

private[immutable] class LongMapEntryIterator[V](it : LongMap[V]) extends LongMapIterator[V, (Long, V)](it){
  def valueOf(tip : LongMap.Tip[V]) = (tip.key, tip.value);
}

private[immutable] class LongMapValueIterator[V](it : LongMap[V]) extends LongMapIterator[V, V](it){
  def valueOf(tip : LongMap.Tip[V]) = tip.value;
}

private[immutable] class LongMapKeyIterator[V](it : LongMap[V]) extends LongMapIterator[V, Long](it){
  def valueOf(tip : LongMap.Tip[V]) = tip.key;
}

import LongMap._;

/**
 * Specialised immutable map structure for long keys, based on 
 * <a href="http://citeseer.ist.psu.edu/okasaki98fast.html">Fast Mergeable Long Maps</a>
 * by Okasaki and Gill. Essentially a trie based on binary digits of the the integers.
 */
sealed abstract class LongMap[+T] extends scala.collection.immutable.Map[Long, T]{
  def empty[S] : LongMap[S] = LongMap.Nil;

  override def toList = {
    val buffer = new scala.collection.mutable.ListBuffer[(Long, T)];
    foreach(buffer += _);
    buffer.toList;
  } 

  /**
   * Iterator over key, value pairs of the map in unsigned order of the keys.
   */
  def elements : Iterator[(Long, T)] = this match {
    case LongMap.Nil => Iterator.empty;
    case _ => new LongMapEntryIterator(this);
  }

  /**
   * Loops over the key, value pairs of the map in unsigned order of the keys. 
   */
  override final def foreach(f : ((Long, T)) => Unit) : Unit = this match {
    case LongMap.Bin(_, _, left, right) => {left.foreach(f); right.foreach(f); }
    case LongMap.Tip(key, value) => f((key, value));
    case LongMap.Nil => {};
  }

  override def keys : Iterator[Long] = this match {
    case LongMap.Nil => Iterator.empty;
    case _ => new LongMapKeyIterator(this);
  }

  /**
   * Loop over the keys of the map. The same as keys.foreach(f), but may
   * be more efficient.
   *
   * @param f The loop body
   */
  final def foreachKey(f : Long => Unit) : Unit = this match {
    case LongMap.Bin(_, _, left, right) => {left.foreachKey(f); right.foreachKey(f); }
    case LongMap.Tip(key, _) => f(key);
    case LongMap.Nil => {}
  }

  override def values : Iterator[T] = this match {
    case LongMap.Nil => Iterator.empty;
    case _ => new LongMapValueIterator(this);
  }

  /**
   * Loop over the keys of the map. The same as keys.foreach(f), but may
   * be more efficient.
   *
   * @param f The loop body
   */
  final def foreachValue(f : T => Unit) : Unit = this match {
    case LongMap.Bin(_, _, left, right) => {left.foreachValue(f); right.foreachValue(f); }
    case LongMap.Tip(_, value) => f(value);
    case LongMap.Nil => {};
  }  

  override def stringPrefix = "LongMap"

  override def isEmpty = this == LongMap.Nil;

  override def filter(f : ((Long, T)) => Boolean) : LongMap[T] = this match {
    case LongMap.Bin(prefix, mask, left, right) => {
      val (newleft, newright) = (left.filter(f), right.filter(f));
      if ((left eq newleft) && (right eq newright)) this;
      else bin(prefix, mask, newleft, newright);
    }
    case LongMap.Tip(key, value) => 
      if (f((key, value))) this
      else LongMap.Nil;
    case LongMap.Nil => LongMap.Nil;
  }

  override def transform[S](f : (Long, T) => S) : LongMap[S] = this match {
    case b@LongMap.Bin(prefix, mask, left, right) => b.bin(left.transform(f), right.transform(f));
    case t@LongMap.Tip(key, value) => t.withValue(f(key, value));  
    case LongMap.Nil => LongMap.Nil;
  }

  final def size : Int = this match {
    case LongMap.Nil => 0;
    case LongMap.Tip(_, _) => 1;
    case LongMap.Bin(_, _, left, right) => left.size + right.size;
  }

  final def get(key : Long) : Option[T] = this match {
    case LongMap.Bin(prefix, mask, left, right) => if (zero(key, mask)) left.get(key) else right.get(key);
    case LongMap.Tip(key2, value) => if (key == key2) Some(value) else None;
    case LongMap.Nil => None;
  }

  final override def getOrElse[S >: T](key : Long, default : =>S) : S = this match {
    case LongMap.Nil => default;
    case LongMap.Tip(key2, value) => if (key == key2) value else default; 
    case LongMap.Bin(prefix, mask, left, right) => if (zero(key, mask)) left.getOrElse(key, default) else right.getOrElse(key, default);
  } 

  final override def apply(key : Long) : T = this match {
    case LongMap.Bin(prefix, mask, left, right) => if (zero(key, mask)) left(key) else right(key);
    case LongMap.Tip(key2, value) => if (key == key2) value else error("Key not found"); 
    case LongMap.Nil => error("key not found");
  } 

  def update[S >: T](key : Long, value : S) : LongMap[S] = this match {
    case LongMap.Bin(prefix, mask, left, right) => if (!hasMatch(key, prefix, mask)) join(key, LongMap.Tip(key, value), prefix, this);
                                          else if (zero(key, mask)) LongMap.Bin(prefix, mask, left.update(key, value), right)
                                          else LongMap.Bin(prefix, mask, left, right.update(key, value)); 
    case LongMap.Tip(key2, value2) => if (key == key2) LongMap.Tip(key, value);
                             else join(key, LongMap.Tip(key, value), key2, this);
    case LongMap.Nil => LongMap.Tip(key, value);
  }

  /**
   * Updates the map, using the provided function to resolve conflicts if the key is already present.
   * Equivalent to 
   * <pre>this.get(key) match { 
   *         case None => this.update(key, value); 
   *         case Some(oldvalue) => this.update(key, f(oldvalue, value) }
   * </pre>
   * 
   * @param key The key to update
   * @param value The value to use if there is no conflict
   * @param f The function used to resolve conflicts.
   */
  def updateWith[S >: T](key : Long, value : S, f : (T, S) => S) : LongMap[S] = this match {
    case LongMap.Bin(prefix, mask, left, right) => if (!hasMatch(key, prefix, mask)) join(key, LongMap.Tip(key, value), prefix, this);
                                          else if (zero(key, mask)) LongMap.Bin(prefix, mask, left.updateWith(key, value, f), right)
                                          else LongMap.Bin(prefix, mask, left, right.updateWith(key, value, f)); 
    case LongMap.Tip(key2, value2) => if (key == key2) LongMap.Tip(key, f(value2, value));
                             else join(key, LongMap.Tip(key, value), key2, this);
    case LongMap.Nil => LongMap.Tip(key, value);
  }

  def -(key : Long) : LongMap[T] = this match {
    case LongMap.Bin(prefix, mask, left, right) => 
      if (!hasMatch(key, prefix, mask)) this;
      else if (zero(key, mask)) bin(prefix, mask, left - key, right);
      else bin(prefix, mask, left, right - key);
    case LongMap.Tip(key2, _) => 
      if (key == key2) LongMap.Nil;
      else this;
    case LongMap.Nil => LongMap.Nil;
  }

  /**
   * A combined transform and filter function. Returns an LongMap such that for each (key, value) mapping
   * in this map, if f(key, value) == None the map contains no mapping for key, and if <code>f(key, value)
   * 
   * @param f The transforming function.
   */
  def modifyOrRemove[S](f : (Long, T) => Option[S]) : LongMap[S] = this match {
      case LongMap.Bin(prefix, mask, left, right) => {
        val newleft = left.modifyOrRemove(f);
        val newright = right.modifyOrRemove(f);
        if ((left eq newleft) && (right eq newright)) this.asInstanceOf[LongMap[S]];
        else bin(prefix, mask, newleft, newright)
      }              
    case LongMap.Tip(key, value) => f(key, value) match {
      case None => LongMap.Nil;
      case Some(value2) => 
        //hack to preserve sharing
        if (value.asInstanceOf[AnyRef] eq value2.asInstanceOf[AnyRef]) this.asInstanceOf[LongMap[S]]
        else LongMap.Tip(key, value2);        
      }
    case LongMap.Nil => LongMap.Nil;
  }
  
 
  /**
   * Forms a union map with that map, using the combining function to resolve conflicts.
   * 
   * @param that the map to form a union with.
   * @param f the function used to resolve conflicts between two mappings. 
   */ 
  def unionWith[S >: T](that : LongMap[S], f : (Long, S, S) => S) : LongMap[S] = (this, that) match{
    case (LongMap.Bin(p1, m1, l1, r1), that@(LongMap.Bin(p2, m2, l2, r2))) => 
      if (shorter(m1, m2)) {     
        if (!hasMatch(p2, p1, m1)) join(p1, this, p2, that);
        else if (zero(p2, m1)) LongMap.Bin(p1, m1, l1.unionWith(that, f), r1);
        else LongMap.Bin(p1, m1, l1, r1.unionWith(that, f)); 
      } else if (shorter(m2, m1)){
        if (!hasMatch(p1, p2, m2)) join(p1, this, p2, that);
        else if (zero(p1, m2)) LongMap.Bin(p2, m2, this.unionWith(l2, f), r2);
        else LongMap.Bin(p2, m2, l2, this.unionWith(r2, f));
      }
      else {
        if (p1 == p2) LongMap.Bin(p1, m1, l1.unionWith(l2,f), r1.unionWith(r2, f));
        else join(p1, this, p2, that); 
      } 
    case (LongMap.Tip(key, value), x) => x.updateWith(key, value, (x, y) => f(key, y, x));
    case (x, LongMap.Tip(key, value)) => x.updateWith[S](key, value, (x, y) => f(key, x, y));
    case (LongMap.Nil, x) => x;
    case (x, LongMap.Nil) => x;
  }

  /**
   * Forms the intersection of these two maps with a combinining function. The resulting map is
   * a map that has only keys present in both maps and has values produced from the original mappings
   * by combining them with f.
   *
   * @param that The map to intersect with.
   * @param f The combining function.
   */
  def intersectionWith[S, R](that : LongMap[S], f : (Long, T, S) => R) : LongMap[R] = (this, that) match {
    case (LongMap.Bin(p1, m1, l1, r1), that@LongMap.Bin(p2, m2, l2, r2)) => 
      if (shorter(m1, m2)) {
        if (!hasMatch(p2, p1, m1)) LongMap.Nil;
        else if (zero(p2, m1)) l1.intersectionWith(that, f);
        else r1.intersectionWith(that, f);
      } else if (m1 == m2) bin(p1, m1, l1.intersectionWith(l2, f), r1.intersectionWith(r2, f));
        else {
        if (!hasMatch(p1, p2, m2)) LongMap.Nil;
        else if (zero(p1, m2)) this.intersectionWith(l2, f);
        else this.intersectionWith(r2, f);
      }
    case (LongMap.Tip(key, value), that) => that.get(key) match {
      case None => LongMap.Nil;
      case Some(value2) => LongMap.Tip(key, f(key, value, value2));
    }
    case (_, LongMap.Tip(key, value)) => this.get(key) match {
      case None => LongMap.Nil;
      case Some(value2) => LongMap.Tip(key, f(key, value2, value));
    }
    case (_, _) => LongMap.Nil;
  }  

  /**
   * Left biased intersection. Returns the map that has all the same mappings as this but only for keys
   * which are present in the other map. 
   *
   * @param that The map to intersect with. 
   */
  def intersection[R](that : LongMap[R]) : LongMap[T] = this.intersectionWith(that, (key : Long, value : T, value2 : R) => value);

  override def ++[S >: T](that : Iterable[(Long, S)]) = that match {
    case (that : LongMap[_]) => this.unionWith[S](that.asInstanceOf[LongMap[S]], (key, x, y) => y);
    case that => that.foldLeft(this : LongMap[S])({case (m, (x, y)) => m.update(x, y)});
  }
}
