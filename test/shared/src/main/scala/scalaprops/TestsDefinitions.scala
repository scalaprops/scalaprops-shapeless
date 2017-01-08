package scalaprops

import scalaprops.derive.Recursive

import shapeless.{Lazy => _, _}
import shapeless.record._
import shapeless.union._

object TestsDefinitions {

  case class Simple(i: Int, s: String, blah: Boolean)

  case object Empty
  case class EmptyCC()
  case class Composed(foo: Simple, other: String)
  case class TwiceComposed(foo: Simple, bar: Composed, v: Int)
  case class ComposedOptList(fooOpt: Option[Simple], other: String, l: List[TwiceComposed])

  sealed trait Base
  case class BaseIS(i: Int, s: String) extends Base
  case class BaseDB(d: Long, b: Boolean) extends Base
  case class BaseLast(c: Simple) extends Base

  case class CCWithSingleton(i: Int, s: Witness.`"aa"`.T)

  sealed trait BaseWithSingleton
  object BaseWithSingleton {
    case class Main(s: Witness.`"aa"`.T) extends BaseWithSingleton
    case class Dummy(i: Int) extends BaseWithSingleton
  }

  object T1 {
    sealed abstract class Tree
    object Tree {
      implicit val recursive: Recursive[Tree] = Recursive[Tree](Gen.value(Leaf))
    }
    final case class Node(left: Tree, right: Tree, v: Int) extends Tree
    case object Leaf extends Tree
  }

  object T2 {
    sealed abstract class Tree
    object Tree {
      implicit val recursive: Recursive[Tree] = Recursive[Tree](Gen.value(Leaf))
    }
    case class Node(left: Tree, right: Tree, v: Int) extends Tree
    case object Leaf extends Tree
  }

  object T1NoRecursiveTC {
    sealed abstract class Tree
    final case class Node(left: Tree, right: Tree, v: Int) extends Tree
    case object Leaf extends Tree
  }

  // cvogt's hierarchy
  sealed trait A
  sealed case class B(i: Int, s: String) extends A
  case object C extends A
  sealed trait D extends A
  final case class E(a: Long, b: Option[Int]) extends D
  case object F extends D
  sealed abstract class Foo extends D
  case object Baz extends Foo
  // Not supporting this one
  // final class Bar extends Foo
  // final class Baz(i1: Int)(s1: String) extends Foo

  type L = Int :: String :: HNil
  type Rec = Record.`'i -> Int, 's -> String`.T

  type C0 = Int :+: String :+: CNil
  type Un = Union.`'i -> Int, 's -> String`.T

  object NoTCDefinitions {
    trait NoGenitraryType
    case class ShouldHaveNoGen(n: NoGenitraryType, i: Int)
    case class ShouldHaveNoGenEither(s: String, i: Int, n: NoGenitraryType)

    sealed trait BaseNoGen
    case class BaseNoGenIS(i: Int, s: String) extends BaseNoGen
    case class BaseNoGenDB(d: Long, b: Boolean) extends BaseNoGen
    case class BaseNoGenN(n: NoGenitraryType) extends BaseNoGen
  }

}
