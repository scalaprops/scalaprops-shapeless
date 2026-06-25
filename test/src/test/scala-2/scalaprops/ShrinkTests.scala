package scalaprops

import scalaprops.ScalapropsShapeless.*
import scalaprops.derive.*
import shapeless.*
import shapeless.labelled.*
import shapeless.record.Record
import shapeless.union.Union

import Util.*

object ShrinkTests extends Scalaprops {
  import TestsDefinitions.*

  private[this] implicit val shrinkChar: Shrink[Char] =
    Shrink.long.xmap(_.toChar, x => x)

  private[this] implicit val genString: Gen[String] =
    Gen.asciiString

  private[this] implicit val shrinkString: Shrink[String] =
    Shrink[List[Char]].xmap[String](_.mkString, _.toCharArray.toList)

  lazy val expectedListIntShrink =
    Shrink.list[Int](Shrink.int)

  lazy val expectedOptionIntShrink =
    Shrink.option(Shrink.int)

  lazy val expectedIntStringBoolShrink =
    expectedIntStringBoolMkHListShrink.shrink

  lazy val expectedIntStringBoolMkHListShrink =
    MkHListShrink.hcons(
      Strict(Shrink.int),
      MkHListShrink.hcons(
        Strict(shrinkString),
        MkHListShrink.hcons(
          Strict(Shrink[Boolean]),
          MkHListShrink.hnil
        )
      )
    )

  lazy val expectedIntStringBoolCoproductShrink =
    MkCoproductShrink
      .ccons(
        Strict(Shrink[Int]),
        MkCoproductShrink.ccons(
          Strict(shrinkString),
          MkCoproductShrink.ccons(
            Strict(Shrink[Boolean]),
            MkCoproductShrink.cnil,
            Strict(Singletons[Boolean]),
            Strict(Singletons[CNil])
          ),
          Strict(Singletons[String]),
          Strict(Singletons[Boolean :+: CNil])
        ),
        Strict(Singletons[Int]),
        Strict(Singletons[String :+: Boolean :+: CNil])
      )
      .shrink

  lazy val expectedSimpleShrink =
    MkShrink
      .genericProduct(
        Generic[Simple],
        shapeless.Lazy(
          expectedIntStringBoolMkHListShrink
        )
      )
      .shrink

  lazy val expectedRecShrink =
    MkHListShrink
      .hcons[FieldType[Witness.`'i`.T, Int], Record.`'s -> String`.T](
        shrinkFieldType[Witness.`'i`.T, Int](Shrink.int),
        MkHListShrink.hcons[FieldType[Witness.`'s`.T, String], HNil](
          shrinkFieldType[Witness.`'s`.T, String](shrinkString),
          MkHListShrink.hnil
        )
      )
      .shrink

  lazy val expectedUnionShrink =
    MkCoproductShrink
      .ccons[FieldType[Witness.`'i`.T, Int], Union.`'s -> String`.T](
        shrinkFieldType[Witness.`'i`.T, Int](Shrink.int),
        MkCoproductShrink.ccons[FieldType[Witness.`'s`.T, String], CNil](
          shrinkFieldType[Witness.`'s`.T, String](shrinkString),
          MkCoproductShrink.cnil,
          Singletons[FieldType[Witness.`'s`.T, String]],
          Singletons[CNil]
        ),
        Singletons[FieldType[Witness.`'i`.T, Int]],
        Singletons[Union.`'s -> String`.T]
      )
      .shrink

  val listInt = {
    val shrink = Shrink[List[Int]]
    compareShrink(shrink, expectedListIntShrink)
  }

  val optionInt = {
    val shrink = Shrink[Option[Int]]
    compareShrink(shrink, expectedOptionIntShrink)
  }

  val simple = {
    val shrink = Shrink[Simple]
    compareShrink(shrink, expectedSimpleShrink)
  }

  val simpleHList = {
    val shrink = Shrink[Int :: String :: Boolean :: HNil]
    compareShrink(shrink, expectedIntStringBoolShrink)
  }

  val simpleCoproduct = {
    val shrink = Shrink[Int :+: String :+: Boolean :+: CNil]
    compareShrink(shrink, expectedIntStringBoolCoproductShrink)
  }

  val simpleRecord = {
    val shrink = Shrink[Rec]
    compareShrink(shrink, expectedRecShrink)
  }

  val simpleUnion = {
    val shrink = Shrink[Un]
    compareShrink(shrink, expectedUnionShrink)
  }
}
