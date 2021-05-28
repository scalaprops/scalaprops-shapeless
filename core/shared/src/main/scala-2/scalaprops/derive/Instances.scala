package scalaprops.derive

import scalaprops.{Cogen, Gen, Shrink}
import shapeless.{Coproduct, HList, LowPriority, Strict, Witness}
import shapeless.labelled._

trait SingletonInstances {
  implicit def genSingletonType[S](implicit w: Witness.Aux[S]): Gen[S] =
    Gen.gen((_, r) => (r, w.value))

  /**
   * Derives `Cogen[T]` instances for `T` a singleton type, like
   * `Witness.``"str"``.T` or `Witness.``true``.T` for example.
   *
   * The generated `Cogen[T]` behaves like `Cogen[Unit]`, as like
   * `Unit`, singleton types only have one instance.
   *
   * @note
   * Extra contramap, that inserts a `next` call on the returned seeds,
   * so that case objects are returned the same Cogen here and when derived through Generic.
   */
  implicit def cogenSingletonType[S](implicit w: Witness.Aux[S]): Cogen[S] =
    Cogen.cogenUnit.contramap[Unit](identity).contramap[S](_ => ())
}

trait FieldTypeInstances {
  implicit def genFieldType[K, H](implicit underlying: Gen[H]): Gen[FieldType[K, H]] =
    underlying.map(field[K](_))

  implicit def cogenFieldType[K, H](implicit underlying: Cogen[H]): Cogen[FieldType[K, H]] =
    underlying.contramap(h => h: H)

  implicit def shrinkFieldType[K, H](implicit underlying: Shrink[H]): Shrink[FieldType[K, H]] =
    underlying.xmap(field[K](_), h => h: H)
}

trait HListInstances {
  implicit def hlistGen[L <: HList](implicit gen: MkHListGen[L]): Gen[L] =
    gen.gen

  implicit def hlistCogen[L <: HList](implicit c: MkHListCogen[L]): Cogen[L] =
    c.cogen

  implicit def hlistShrink[L <: HList](implicit s: MkHListShrink[L]): Shrink[L] =
    s.shrink
}

trait CoproductInstances {
  implicit def coproductGen[C <: Coproduct](implicit gen: MkCoproductGen[C]): Gen[C] =
    gen.gen

  implicit def coproductCogen[C <: Coproduct](implicit c: MkCoproductCogen[C]): Cogen[C] =
    c.cogen

  implicit def coproductShrink[C <: Coproduct](implicit s: MkCoproductShrink[C]): Shrink[C] =
    s.shrink
}

trait DerivedInstances {
  implicit def derivedGen[T](implicit ev: LowPriority, underlying: Strict[MkGen[T]]): Gen[T] =
    underlying.value.gen

  implicit def derivedShrink[T](implicit ev: LowPriority, underlying: Strict[MkShrink[T]]): Shrink[T] =
    underlying.value.shrink

  implicit def derivedCogen[T](implicit ev: LowPriority, underlying: Strict[MkCogen[T]]): Cogen[T] =
    underlying.value.cogen
}
