package scalaprops

import scalaprops.ScalapropsShapeless.*

object ScalapropsShapelessTest extends Scalaprops {
  sealed trait A1 extends Product
  object A1 {
    final case class A2(x: Int) extends A1
    final case class A3(x1: Boolean, x2: Long) extends A1
  }

  val genTest1 = Property.forAll { (seed: Long) =>
    Gen[A1].infiniteIterator(seed = seed).take(100).toList.groupBy(_.getClass.getName).size == 2
  }

  val cogenTest1 = Property.forAll { (seed1: Long, r: Rand) =>
    val g = Gen[A1 => A1].f(100, r)._2
    val values: List[A1] = Gen[A1].infiniteIterator(seed = seed1).take(100).toList.map(g.apply)
    values.groupBy(_.getClass.getName).size == 2
  }
}
