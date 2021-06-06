package scalaprops

import shapeless3.deriving.K0

sealed abstract class ScalapropsShapelessInstances {

  inline implicit def cogenCoproduct[A](using inst: => K0.CoproductInstances[Cogen, A]): Cogen[A] =
    new Cogen[A] {
      override def cogen[B](a: A, s: CogenState[B]) = {
        inst.fold(a)([t] => (c: Cogen[t], t: t) => c.cogen(t, s))
      }
    }

  inline implicit def cogenProduct[A](using inst: => K0.ProductInstances[Cogen, A]): Cogen[A] =
    new Cogen[A] {
      override def cogen[B](a: A, s: CogenState[B]) = {
        inst.foldLeft[CogenState[B]](a)(s)(
          [t] => (acc: CogenState[B], c: Cogen[t], t: t) => c.cogen(t, acc)
        )
      }
    }

  inline implicit def genProduct[A](using inst: => K0.ProductInstances[Gen, A]): Gen[A] =
    Gen.gen[A]((size, rand) =>
      val (x, y) = inst.unfold[Rand](rand) {
        [t] =>
          (r: Rand, g: Gen[t]) => {
            val (next, a) = g.f(size, r)
            (next, Option(a))
        }
      }
      (x, y.get)
    )

  inline implicit def genCoproduct[A](using inst: => K0.CoproductInstances[Gen, A]): Gen[A] =
    Gen.gen[A]((size, r1) =>
      val (r2, i) = r1.nextInt
      val index = if (i == Int.MinValue) {
        0
      } else {
        i.abs % inst.is.length
      }
      val (x, y) = inst.project[Rand](index)(r2) {
        [t] =>
          (r3: Rand, g: Gen[t]) => {
            val (r4, a) = g.f(size, r3)
            (r4, Option(a))
        }
      }
      (x, y.get)
    )

}

object ScalapropsShapeless extends ScalapropsShapelessInstances {

  inline implicit def deriveGen[A](using gen: K0.Generic[A]): Gen[A] =
    gen.derive(genProduct, genCoproduct)

  inline implicit def deriveCogen[A](using gen: K0.Generic[A]): Cogen[A] =
    gen.derive(cogenProduct, cogenCoproduct)

}
