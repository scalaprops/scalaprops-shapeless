package scalaprops

import shapeless3.deriving.*

sealed abstract class ScalapropsShapelessInstances {

  inline given genProduct[A](using inst: => K0.ProductInstances[Gen, A]): Gen[A] =
    Gen.gen[A]((size, rand) =>
      val (x, y) = inst.unfold[Rand](rand){
        [t] => (r: Rand, g: Gen[t]) => {
          val (next, a) = g.f(size, r)
          (next, Option(a))
        }
      }
      (x, y.get)
    )

  inline given genCoproduct[A](using inst: => K0.CoproductInstances[Gen, A]): Gen[A] =
    Gen.gen[A]((size, r1) =>
      val (r2, i) = r1.nextInt
      val index = if (i == Int.MinValue) {
        0
      } else {
        i.abs % inst.is.length
      }
      val (x, y) = inst.project[Rand](index)(r2){
        [t] => (r3: Rand, g: Gen[t]) => {
          val (r4, a) = g.f(size, r3)
          (r4, Option(a))
        }
      }
      (x, y.get)
    )
}

object ScalapropsShapeless extends ScalapropsShapelessInstances {
  inline given derived[A](using gen: K0.Generic[A]): Gen[A] =
    gen.derive(genProduct, genCoproduct)
}
