package foo

import scalaprops.Gen
import scalaprops.ScalapropsShapeless
import scalaprops.ScalapropsShapeless.given

sealed trait A[X]
case class B[X](i: Int, y: String, x: X) extends A[X]
case class C[X](b: Boolean, o: Option[X]) extends A[X]
case class D[X]() extends A[X]
object Main {
  given stringInstance: Gen[String] = Gen.alphaNumString
  val x1: Gen[A[Int]] = ScalapropsShapeless.derived[A[Int]]
  def main(args: Array[String]): Unit = {
    x1.samples(seed = System.nanoTime, listSize = 30).foreach(println)
  }
}
