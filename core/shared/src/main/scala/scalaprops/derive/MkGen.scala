package scalaprops
package derive

import shapeless._

/**
 * Derives `Gen[T]` instances for `T` an `HList`, a `Coproduct`,
 * a case class or an ADT (or more generally, a type represented
 * `Generic`ally as an `HList` or a `Coproduct`).
 *
 * Use like
 *     val gen: Gen[T] = MkGen[T].gen
 * or look up for an implicit `MkGen[T]`.
 */
trait MkGen[T] {

  /** `Gen[T]` instance built by this `MkGen[T]` */
  def gen: Gen[T]
}

abstract class MkGenLowPriority {

  implicit def genericNonRecursiveCoproduct[S, C <: Coproduct](implicit gen: Generic.Aux[S, C],
                                                               mkGen: shapeless.Lazy[MkCoproductGen[C]]): MkGen[S] =
    MkGen.instance(
      Gen.delay(mkGen.value.gen).map(gen.from)
    )
}

object MkGen extends MkGenLowPriority {
  def apply[T](implicit mkGen: MkGen[T]): MkGen[T] = mkGen

  def instance[T](g: => Gen[T]): MkGen[T] =
    new MkGen[T] {
      def gen = g
    }

  implicit def genericProduct[P, L <: HList](implicit gen: Generic.Aux[P, L],
                                             mkGen: shapeless.Lazy[MkHListGen[L]]): MkGen[P] =
    instance(
      Gen.delay(mkGen.value.gen).map(gen.from)
    )

  implicit def genericRecursiveCoproduct[S, C <: Coproduct](
    implicit rec: Recursive[S],
    gen: Generic.Aux[S, C],
    mkGen: shapeless.Lazy[MkRecursiveCoproductGen[C]]): MkGen[S] =
    instance(
      Gen.delay(mkGen.value.gen).flatMap {
        _.valueOpt match {
          case None =>
            rec.default
          case Some(c) =>
            Gen.value(gen.from(c))
        }
      }
    )
}

trait MkHListGen[L <: HList] { self =>

  /** `Gen[T]` instance built by this `MkGenHList[T]` */
  def gen: Gen[L]
}

object MkHListGen {
  def apply[L <: HList](implicit mkGen: MkHListGen[L]): MkHListGen[L] = mkGen

  def instance[L <: HList](g: => Gen[L]): MkHListGen[L] =
    new MkHListGen[L] {
      def gen = g
    }

  implicit val hnil: MkHListGen[HNil] =
    instance(Gen.value(HNil))

  implicit def hcons[H, T <: HList, N <: Nat](implicit headGen: Strict[Gen[H]],
                                              tailGen: MkHListGen[T],
                                              length: ops.hlist.Length.Aux[T, N],
                                              n: ops.nat.ToInt[N]): MkHListGen[H :: T] =
    instance(
      Gen.sized { size0 =>
        if (size0 < 0)
          // unlike positive values, don't split negative sizes any further, and let subsequent Gen handle them
          for {
            head <- headGen.map(_.resize(size0)).value
            tail <- Gen.delay(tailGen.gen).resize(size0)
          } yield head :: tail
        else {
          // take a fraction of approximately 1 / (n + 1) from size for the head, leave the
          // remaining for the tail

          val size = size0 max 0
          val remainder = size % (n() + 1)
          val fromRemainderGen =
            if (remainder > 0)
              Gen.choose(1, n()).map(r => if (r <= remainder) 1 else 0)
            else
              Gen.value(0)

          for {
            fromRemainder <- fromRemainderGen
            headSize = size / (n() + 1) + fromRemainder
            head <- headGen.map(_.resize(headSize)).value
            tail <- Gen.delay(tailGen.gen).resize(size - headSize)
          } yield head :: tail
        }
      }
    )
}

trait MkRecursiveCoproductGen[C <: Coproduct] {

  /** `Gen[T]` instance built by this `MkRecursiveCoproductGen[T]` */
  def gen: Gen[Recursive.Value[C]]
}

object MkRecursiveCoproductGen {
  def apply[C <: Coproduct](implicit mkGen: MkRecursiveCoproductGen[C]): MkRecursiveCoproductGen[C] = mkGen

  def instance[C <: Coproduct](g: => Gen[Recursive.Value[C]]): MkRecursiveCoproductGen[C] =
    new MkRecursiveCoproductGen[C] {
      def gen = g
    }

  implicit val cnil: MkRecursiveCoproductGen[CNil] =
    instance(Gen.gen((_, r) => (r, ???)))

  implicit def ccons[H, T <: Coproduct, N <: Nat](implicit headGen: Strict[Gen[H]],
                                                  tailGen: MkRecursiveCoproductGen[T],
                                                  length: ops.coproduct.Length.Aux[T, N],
                                                  n: ops.nat.ToInt[N]): MkRecursiveCoproductGen[H :+: T] =
    instance[H :+: T](
      Gen.sized[Recursive.Value[H :+: T]] {
        case x if x < 0 =>
          Gen.value(Recursive.Value(None))
        case size =>
          val nextSize = size - 1
          Gen.lazyFrequency(
            1 -> scalaprops.Lazy(headGen.value.resize(nextSize).map(h => Recursive.Value(Some[H :+: T](Inl(h))))),
            n() -> scalaprops.Lazy(Gen.delay(tailGen.gen).resize(nextSize).map(_.map(Inr(_): (H :+: T))))
          )
      }
    )
}

trait MkCoproductGen[C <: Coproduct] { self =>

  /** `Gen[T]` instance built by this `MkCoproductGen[T]` */
  def gen: Gen[C]
}

object MkCoproductGen {
  def apply[C <: Coproduct](implicit mkArb: MkCoproductGen[C]): MkCoproductGen[C] = mkArb

  def instance[C <: Coproduct](g: => Gen[C]): MkCoproductGen[C] =
    new MkCoproductGen[C] {
      def gen = g
    }

  implicit val cnil: MkCoproductGen[CNil] =
    instance(Gen.gen((_, r) => (r, ???)))

  implicit def ccons[H, T <: Coproduct, N <: Nat](implicit headGen: Strict[Gen[H]],
                                                  tailGen: MkCoproductGen[T],
                                                  length: ops.coproduct.Length.Aux[T, N],
                                                  n: ops.nat.ToInt[N]): MkCoproductGen[H :+: T] =
    instance(
      Gen.sized { size =>
        /*
         * Unlike MkCoproductGen above, try to generate a value no matter what (no Gen.fail).
         * This can blow the stack for recursive types, so should be avoided for those.
         */
        val nextSize = (size - 1) max 0
        Gen.lazyFrequency(
          1 -> scalaprops.Lazy(headGen.map(_.map(Inl(_): (H :+: T)).resize(nextSize)).value),
          n() -> scalaprops.Lazy(Gen.delay(tailGen.gen).map(Inr(_): (H :+: T)).resize(nextSize))
        )
      }
    )
}
