package scalaprops

import shapeless3.deriving.*

sealed abstract class ScalapropsShapelessInstances {

  /*
  https://java-src.appspot.com/com.github.scalaprops/scalaprops-gen_3/0.8.3/scalaprops/CogenInstances.scala

      def cogen[Z](a: Either[A, B], g: CogenState[Z]) =
        a match {
          case Right(x) =>
            variantInt(1, B.cogen(x, g))
          case Left(x) =>
            variantInt(0, A.cogen(x, g.copy(rand = g.rand.next)))
        }

     def inject[R](p: Int)(f: [t <: T] => F[t] => R): R =

     def fold[R](x: T)(f: [t] => (F[t], t) => R): R =

     def fold2[R](x: T, y: T)(a: => R)(f: [t] => (F[t], t, t) => R): R =

     def fold2[R](x: T, y: T)(g: (Int, Int) => R)(f: [t] => (F[t], t, t) => R): R =
  */

  inline given cogenCoproduct[A](using inst: => K0.CoproductInstances[Cogen, A]): Cogen[A] =
    new Cogen[A] {
      override def cogen[B](a: A, s: CogenState[B]) = {
        // TODO
        ???
      }
    }

  inline given cogenProduct[A](using inst: => K0.ProductInstances[Cogen, A]): Cogen[A] =
    new Cogen[A] {
      override def cogen[B](a: A, s: CogenState[B]) = {
        inst.foldRight[CogenState[B]](a)(s)(
          [t] => (acc: CogenState[B], c: Cogen[t], t: t) => c.cogen(t, acc)
        )
      }
    }

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
