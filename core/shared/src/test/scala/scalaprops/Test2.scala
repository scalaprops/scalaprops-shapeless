package xxx

import scalaprops.ScalapropsShapeless.given
import scalaprops.Gen

case class Foo(i: Int, s: String, blah: Boolean)
case class Bar(foo: Foo, other: String)
sealed trait Base
case class BaseIntString(i: Int, s: String) extends Base
case class BaseDoubleBoolean(d: Double, b: Boolean) extends Base

sealed trait Tree
case class A(x1: Tree, x2: Tree) extends Tree
case class B(value: Int) extends Tree

object Main {
  given Gen[String] = Gen.alphaNumString

  Gen[Foo]
  Gen[Bar]
  Gen[Base]

  def main(args: Array[String]): Unit = {
    val treeGen = Gen[Tree]
    treeGen.samples(seed = System.nanoTime).foreach(println)
  }
}
