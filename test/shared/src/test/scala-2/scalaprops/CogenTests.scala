package scalaprops

import scalaprops.derive._

import shapeless._
import shapeless.labelled.FieldType
import shapeless.record.Record
import shapeless.union.Union

import Util._

object CogenTests extends Scalaprops {
  import TestsDefinitions._
  import ScalapropsShapeless._

  private[this] implicit val genString: Gen[String] =
    Gen.asciiString

  lazy val expectedIntStringBoolCogen =
    expectedIntStringBoolMkHListCogen.cogen

  lazy val expectedIntStringBoolMkHListCogen =
    MkHListCogen.hcons(
      Cogen.cogenInt,
      MkHListCogen.hcons(
        Cogen.cogenString,
        MkHListCogen.hcons(
          Cogen.cogenBoolean,
          MkHListCogen.hnil
        )
      )
    )

  lazy val expectedIntStringBoolCoproductCogen =
    MkCoproductCogen
      .ccons(
        Cogen.cogenInt,
        MkCoproductCogen.ccons(
          Cogen.cogenString,
          MkCoproductCogen.ccons(
            Cogen.cogenBoolean,
            MkCoproductCogen.cnil
          )
        )
      )
      .cogen

  lazy val expectedSimpleCogen =
    MkCogen
      .genericProduct(
        Generic[Simple],
        expectedIntStringBoolMkHListCogen
      )
      .cogen

  lazy val expectedRecCogen =
    MkHListCogen
      .hcons[FieldType[Witness.`'i`.T, Int], Record.`'s -> String`.T](
        cogenFieldType[Witness.`'i`.T, Int](Cogen.cogenInt),
        MkHListCogen.hcons[FieldType[Witness.`'s`.T, String], HNil](
          cogenFieldType[Witness.`'s`.T, String](Cogen.cogenString),
          MkHListCogen.hnil
        )
      )
      .cogen

  lazy val expectedUnionCogen =
    MkCoproductCogen
      .ccons[FieldType[Witness.`'i`.T, Int], Union.`'s -> String`.T](
        cogenFieldType[Witness.`'i`.T, Int](Cogen.cogenInt),
        MkCoproductCogen.ccons[FieldType[Witness.`'s`.T, String], CNil](
          cogenFieldType[Witness.`'s`.T, String](Cogen.cogenString),
          MkCoproductCogen.cnil
        )
      )
      .cogen

  val compareSuccess = Property.forAll {
    val cogen = Cogen.cogenInt
    compareCogen(cogen, cogen)
  }

  val compareFailure = Property.forAll {
    val cogen = Cogen.cogenInt
    compareCogen(cogen, cogen.contramap[Int](_ + 1)) == false
  }

  val empty = Property.forAll {
    val expectedCogen =
      MkCogen
        .genericProduct(
          Generic[Empty.type],
          shapeless.Lazy(MkHListCogen.hnil)
        )
        .cogen

    val cogen = Cogen[Empty.type]
    compareCogen(expectedCogen, cogen)
  }

  val emptyAsSingleton = Property.forAll {
    val expectedCogen =
      cogenSingletonType[Empty.type]

    val cogen = Cogen[Empty.type]
    compareCogen(expectedCogen, cogen)
  }

  val emptyCC = Property.forAll {
    val expectedCogen =
      MkCogen
        .genericProduct(
          Generic[EmptyCC],
          shapeless.Lazy(MkHListCogen.hnil)
        )
        .cogen

    val cogen = Cogen[EmptyCC]
    compareCogen(expectedCogen, cogen)
  }

  val simple = Property.forAll {
    val cogen = Cogen[Simple]
    compareCogen(expectedSimpleCogen, cogen)
  }

  val simpleHList = Property.forAll {
    val cogen = Cogen[Int :: String :: Boolean :: HNil]
    compareCogen(expectedIntStringBoolCogen, cogen)
  }

  val simpleCoproduct = Property.forAll {
    val cogen = Cogen[Int :+: String :+: Boolean :+: CNil]
    compareCogen(expectedIntStringBoolCoproductCogen, cogen)
  }

  val simpleRec = Property.forAll {
    val cogen = Cogen[Rec]
    compareCogen(expectedRecCogen, cogen)
  }

  val simpleUnion = Property.forAll {
    val cogen = Cogen[Un]
    compareCogen(expectedUnionCogen, cogen)
  }
}
