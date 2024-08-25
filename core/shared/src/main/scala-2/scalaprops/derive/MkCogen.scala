package scalaprops
package derive

import shapeless.*

/**
 * Derives `Cogen[T]` instances for `T` an `HList`, a `Coproduct`,
 * a case class or an ADT (or more generally, a type represented
 * `Generic`ally as an `HList` or a `Coproduct`).
 *
 * Use like
 *     val cogen: Cogen[T] = MkCogen[T].cogen
 * or look up for an implicit `MkCogen[T]`.
 */
trait MkCogen[T] {

  /** `Cogen[T]` instance built by this `MkCogen[T]` */
  def cogen: Cogen[T]
}

object MkCogen {
  def apply[T](implicit mkCogen: MkCogen[T]): MkCogen[T] = mkCogen

  def instance[T](cogen0: => Cogen[T]): MkCogen[T] =
    new MkCogen[T] {
      def cogen = cogen0
    }

  implicit def genericProduct[P, L <: HList](implicit
    gen: Generic.Aux[P, L],
    cogen: shapeless.Lazy[MkHListCogen[L]]
  ): MkCogen[P] =
    instance(cogen.value.cogen.contramap(gen.to))

  implicit def genericCoproduct[S, C <: Coproduct](implicit
    gen: Generic.Aux[S, C],
    cogen: shapeless.Lazy[MkCoproductCogen[C]]
  ): MkCogen[S] =
    instance(cogen.value.cogen.contramap(gen.to))
}

trait MkHListCogen[L <: HList] {

  /** `Cogen[T]` instance built by this `MkCogen[T]` */
  def cogen: Cogen[L]
}

object MkHListCogen {
  def apply[L <: HList](implicit mkCogen: MkHListCogen[L]): MkHListCogen[L] = mkCogen

  def instance[L <: HList](cogen0: => Cogen[L]): MkHListCogen[L] =
    new MkHListCogen[L] {
      def cogen = cogen0
    }

  implicit lazy val hnil: MkHListCogen[HNil] =
    instance(Cogen.cogenUnit.contramap(_ => ()))

  implicit def hcons[H, T <: HList](implicit
    headCogen: Strict[Cogen[H]],
    tailCogen: MkHListCogen[T]
  ): MkHListCogen[H :: T] =
    instance(
      Cogen.divide(
        headCogen.value,
        tailCogen.cogen
      )(z => (z.head, z.tail))
    )
}

trait MkCoproductCogen[C <: Coproduct] {

  /** `Cogen[T]` instance built by this `MkCogen[T]` */
  def cogen: Cogen[C]
}

object MkCoproductCogen {
  def apply[C <: Coproduct](implicit mkCogen: MkCoproductCogen[C]): MkCoproductCogen[C] = mkCogen

  def instance[C <: Coproduct](cogen0: => Cogen[C]): MkCoproductCogen[C] =
    new MkCoproductCogen[C] {
      def cogen = cogen0
    }

  implicit lazy val cnil: MkCoproductCogen[CNil] =
    instance(Cogen.cogenUnit.contramap(_ => ()))

  implicit def ccons[H, T <: Coproduct](implicit
    headCogen: Strict[Cogen[H]],
    tailCogen: MkCoproductCogen[T]
  ): MkCoproductCogen[H :+: T] =
    instance(
      new Cogen[H :+: T] {
        def cogen[B](a: H :+: T, g: CogenState[B]): CogenState[B] =
          a match {
            case Inl(h) =>
              headCogen.value.cogen(h, g)
            case Inr(t) =>
              tailCogen.cogen.cogen(t, g)
          }
      }
    )
}
