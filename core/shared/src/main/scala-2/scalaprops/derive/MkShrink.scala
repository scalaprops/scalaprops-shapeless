package scalaprops
package derive

import shapeless.*

/**
 * Derives `Shrink[T]` instances for `T` an `HList`, a `Coproduct`,
 * a case class or an ADT (or more generally, a type represented
 * `Generic`ally as an `HList` or a `Coproduct`).
 *
 * Use like
 *     val shrink: Shrink[T] = MkShrink[T].shrink
 * or look up for an implicit `MkShrink[T]`.
 */
trait MkShrink[T] {

  /** `Shrink[T]` instance built by this `MkShrink[T]` */
  def shrink: Shrink[T]
}

object MkShrink {
  def apply[T](implicit mkShrink: MkShrink[T]): MkShrink[T] = mkShrink

  def instance[T](shrink0: => Shrink[T]): MkShrink[T] =
    new MkShrink[T] {
      def shrink = shrink0
    }

  private[this] def lazyxmap[T, U](from: T => U, to: U => T)(st: => Shrink[T]): Shrink[U] =
    Shrink.shrink[U] { u => st(to(u)).map(from) }

  implicit def genericProduct[P, L <: HList](implicit
    gen: Generic.Aux[P, L],
    shrink: shapeless.Lazy[MkHListShrink[L]]
  ): MkShrink[P] =
    instance(
      lazyxmap(gen.from, gen.to)(shrink.value.shrink)
    )

  implicit def genericCoproduct[S, C <: Coproduct](implicit
    gen: Generic.Aux[S, C],
    shrink: shapeless.Lazy[MkCoproductShrink[C]]
  ): MkShrink[S] =
    instance(
      lazyxmap(gen.from, gen.to)(shrink.value.shrink)
    )
}

trait MkHListShrink[L <: HList] {

  /** `Shrink[T]` instance built by this `MkHListShrink[T]` */
  def shrink: Shrink[L]
}

object MkHListShrink {
  def apply[L <: HList](implicit mkShrink: MkHListShrink[L]): MkHListShrink[L] = mkShrink

  def instance[L <: HList](shrink0: => Shrink[L]): MkHListShrink[L] =
    new MkHListShrink[L] {
      def shrink = shrink0
    }

  implicit val hnil: MkHListShrink[HNil] =
    instance(Shrink.empty)

  implicit def hcons[H, T <: HList](implicit
    headShrink: Strict[Shrink[H]],
    tailShrink: MkHListShrink[T]
  ): MkHListShrink[H :: T] =
    instance(
      Shrink.shrink { case h :: t =>
        headShrink.value(h).map(_ :: t) #:::
          tailShrink.shrink(t).map(h :: _)
      }
    )
}

trait MkCoproductShrink[C <: Coproduct] {

  /** `Shrink[T]` instance built by this `MkCoproductShrink[T]` */
  def shrink: Shrink[C]
}

object MkCoproductShrink {
  def apply[T <: Coproduct](implicit mkShrink: MkCoproductShrink[T]): MkCoproductShrink[T] = mkShrink

  def instance[T <: Coproduct](shrink0: => Shrink[T]): MkCoproductShrink[T] =
    new MkCoproductShrink[T] {
      def shrink = shrink0
    }

  implicit val cnil: MkCoproductShrink[CNil] =
    instance(Shrink.empty)

  implicit def ccons[H, T <: Coproduct](implicit
    headShrink: Strict[Shrink[H]],
    tailShrink: MkCoproductShrink[T],
    headSingletons: Strict[Singletons[H]],
    tailSingletons: Strict[Singletons[T]]
  ): MkCoproductShrink[H :+: T] =
    instance(
      Shrink.shrink {
        case Inl(h) =>
          tailSingletons.value().toStream.map(Inr(_)) ++ headShrink.value.apply(h).map(Inl(_))
        case Inr(t) =>
          headSingletons.value().toStream.map(Inl(_)) ++ tailShrink.shrink.apply(t).map(Inr(_))
      }
    )
}
