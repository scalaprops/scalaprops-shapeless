package scalaprops

import Shapeless._
import scalaprops.TestsDefinitions.{T1, T1NoRecursiveTC}

object PropertyTests extends Scalaprops {

  val oneElementAdt = {
    sealed trait Foo
    case object Bar extends Foo

    Property.forAll { (f: Int => Foo) =>
      f(0); true
    }
  }

  val twoElementAdt = {
    sealed trait Or[+A, +B] extends Product with Serializable
    case class Left[A](a: A) extends Or[A, Nothing]
    case class Right[B](b: B) extends Or[Nothing, B]

    Property.forAll { (f: Int => Float Or Boolean) =>
      f(0); true
    }
  }

  val recursiveADT = {
    case class Node[T](value: T, left: Option[Node[T]], right: Option[Node[T]])

    Property.forAll { (f: Int => Node[Int]) =>
      f(0)
      true
    }
  }.ignore("TODO stack overflow")

  val recursiveADT1 = Property.forAll { (f: Int => T1.Tree) =>
    f(0)
    true
  }.toProperties((), Param.minSuccessful(10000))

  val recursiveADT2 = {
    Property.forAll { (f: Int => T1NoRecursiveTC.Tree) =>
      f(0)
      true
    }
  }

}
