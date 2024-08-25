package scalaprops

import shapeless3.deriving.K0
import scala.deriving.Mirror

sealed abstract class ScalapropsShapelessInstances {

  final def cogenFromPolyFunction[A](f: [t] => (a: A, s: CogenState[t]) => CogenState[t]): Cogen[A] =
    new Cogen[A] {
      override def cogen[B](a: A, s: CogenState[B]) = f[B](a, s)
    }

  inline implicit def cogenCoproduct[A](using inst: => K0.CoproductInstances[Cogen, A]): Cogen[A] =
    cogenFromPolyFunction[A](
      [B] => (a: A, s: CogenState[B]) => inst.fold(a)([t <: A] => (c: Cogen[t], t: t) => c.cogen(t, s))
    )

  inline implicit def cogenProduct[A](using inst: => K0.ProductInstances[Cogen, A]): Cogen[A] =
    cogenFromPolyFunction[A](
      [B] =>
        (a: A, s: CogenState[B]) =>
          inst.foldLeft[CogenState[B]](a)(s)(
            [t] => (acc: CogenState[B], c: Cogen[t], t: t) => c.cogen(t, acc)
        )
    )

  inline implicit def genProduct[A](using inst: => K0.ProductInstances[Gen, A]): Gen[A] =
    Gen.gen[A] { (size, rand) =>
      val (x, y) = inst.unfold[Rand](rand) {
        [t] =>
          (r: Rand, g: Gen[t]) => {
            val (next, a) = g.f(size, r)
            (next, Option(a))
        }
      }
      (x, y.get)
    }

  inline implicit def genCoproduct[A](using inst: => K0.CoproductInstances[Gen, A], mirror: Mirror.SumOf[A]): Gen[A] =
    Gen.gen[A] { (size, r1) =>
      val (r2, i) = r1.nextInt
      val index = if (i == Int.MinValue) {
        0
      } else {
        i.abs % valueOf[Tuple.Size[mirror.MirroredElemTypes]]
      }
      inst.inject[(Rand, A)](index) {
        [t <: A] => (g: Gen[t]) => g.f(size, r2)
      }
    }

}

object ScalapropsShapeless extends ScalapropsShapelessInstances {

  inline implicit def deriveGen[A](using gen: K0.Generic[A]): Gen[A] =
    gen.derive(genProduct, genCoproduct)

  inline implicit def deriveCogen[A](using gen: K0.Generic[A]): Cogen[A] =
    gen.derive(cogenProduct, cogenCoproduct)

}
