package scalaprops

import scalaprops.derive.Singletons

object Util {
  def compareGenHelper[T](first: Gen[T], second: Gen[T])(
    len: Int
  ): Boolean = {
    val seed = System.currentTimeMillis()
    val generated = first.infiniteStream(seed = seed).zip(second.infiniteStream(seed = seed)).take(len)
    generated.forall { case (a, b) => a == b }
  }

  /** Ask each `Gen[T]` a sequence of values, given the same parameters and initial seed,
   * and return false if both sequences aren't equal. */
  def compareGen[T](first: Gen[T], second: Gen[T]): Boolean = {
    compareGenHelper(first, second)(100)
  }

  def compareCogenHelper[T: Gen](r: Rand)(first: Cogen[T], second: Cogen[T])(
    len: Int
  ): Boolean = {
    val values = Gen.infinite(Gen.defaultSize, r, Gen[T]).take(len).toList

    val s = CogenState(r, Gen[T])

    val firstSeeds = values.scanLeft(s)((x, y) => first.cogen(y, x))
    val secondSeeds = values.scanLeft(s)((x, y) => second.cogen(y, x))
    val seeds = firstSeeds zip secondSeeds

    seeds.forall {
      case (a, b) =>
        val s = System.currentTimeMillis()
        val size = 20
        (a.rand == b.rand) && {
          a.gen.samples(seed = s, listSize = size) == b.gen.samples(seed = s, listSize = size)
        }
    }
  }

  def compareCogen[T: Gen](first: Cogen[T], second: Cogen[T]): Boolean =
    compareCogenHelper(Rand.standard(System.currentTimeMillis()))(first, second)(50)

  def compareShrink[T: Gen](first: Shrink[T], second: Shrink[T]): Property =
    Property.forAll { t: T =>
      first(t) == second(t)
    }

  def validateSingletons[T: Singletons](expected: T*): Boolean = {
    val found = Singletons[T].apply()
    found == expected
  }
}
