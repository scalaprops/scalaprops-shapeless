package scalaprops

import scalaprops.derive._

import shapeless._
import shapeless.labelled.FieldType
import shapeless.record._
import shapeless.union._
import shapeless.test.illTyped

import Util._

object GenTests extends Scalaprops {
  import TestsDefinitions._
  import ScalapropsShapeless._

  private[this] implicit val genString: Gen[String] =
    Gen.asciiString

  lazy val expectedSimpleGen: Gen[Simple] =
    MkGen
      .genericProduct(
        Generic[Simple],
        MkHListGen.hcons(
          Gen[Int],
          MkHListGen.hcons(
            Strict(genString),
            MkHListGen.hcons(
              Gen.genBoolean,
              MkHListGen.hnil,
              ops.hlist.Length[HNil],
              ops.nat.ToInt[Nat._0]
            ),
            ops.hlist.Length[Boolean :: HNil],
            ops.nat.ToInt[Nat._1]
          ),
          ops.hlist.Length[String :: Boolean :: HNil],
          ops.nat.ToInt[Nat._2]
        )
      )
      .gen

  lazy val expectedIntStringBoolGen: Gen[Int :: String :: Boolean :: HNil] =
    MkHListGen
      .hcons(
        Gen[Int],
        MkHListGen.hcons(
          Strict(genString),
          MkHListGen.hcons(
            Gen.genBoolean,
            MkHListGen.hnil,
            ops.hlist.Length[HNil],
            ops.nat.ToInt[Nat._0]
          ),
          ops.hlist.Length[Boolean :: HNil],
          ops.nat.ToInt[Nat._1]
        ),
        ops.hlist.Length[String :: Boolean :: HNil],
        ops.nat.ToInt[Nat._2]
      )
      .gen

  lazy val expectedIntStringBoolCoproductGen: Gen[Int :+: String :+: Boolean :+: CNil] =
    MkCoproductGen
      .ccons(
        Gen[Int],
        MkCoproductGen.ccons(
          Strict(genString),
          MkCoproductGen.ccons(
            Gen.genBoolean,
            MkCoproductGen.cnil,
            ops.coproduct.Length[CNil],
            ops.nat.ToInt[Nat._0]
          ),
          ops.coproduct.Length[Boolean :+: CNil],
          ops.nat.ToInt[Nat._1]
        ),
        ops.coproduct.Length[String :+: Boolean :+: CNil],
        ops.nat.ToInt[Nat._2]
      )
      .gen

  lazy val expectedComposedGen: Gen[Composed] =
    MkGen
      .genericProduct(
        Generic[Composed],
        MkHListGen.hcons(
          expectedSimpleGen,
          MkHListGen.hcons(
            Strict(genString),
            MkHListGen.hnil,
            ops.hlist.Length[HNil],
            ops.nat.ToInt[Nat._0]
          ),
          ops.hlist.Length[String :: HNil],
          ops.nat.ToInt[Nat._1]
        )
      )
      .gen

  lazy val expectedTwiceComposedGen: Gen[TwiceComposed] =
    MkGen
      .genericProduct(
        Generic[TwiceComposed],
        MkHListGen.hcons(
          expectedSimpleGen,
          MkHListGen.hcons(
            expectedComposedGen,
            MkHListGen.hcons(
              Gen[Int],
              MkHListGen.hnil,
              ops.hlist.Length[HNil],
              ops.nat.ToInt[Nat._0]
            ),
            ops.hlist.Length[Int :: HNil],
            ops.nat.ToInt[Nat._1]
          ),
          ops.hlist.Length[Composed :: Int :: HNil],
          ops.nat.ToInt[Nat._2]
        )
      )
      .gen

  lazy val expectedComposedOptListGen: Gen[ComposedOptList] =
    MkGen
      .genericProduct(
        Generic[ComposedOptList],
        MkHListGen.hcons(
          Gen.option(expectedSimpleGen),
          MkHListGen.hcons(
            Strict(genString),
            MkHListGen.hcons(
              Gen.list[TwiceComposed](expectedTwiceComposedGen),
              MkHListGen.hnil,
              ops.hlist.Length[HNil],
              ops.nat.ToInt[Nat._0]
            ),
            ops.hlist.Length[List[TwiceComposed] :: HNil],
            ops.nat.ToInt[Nat._1]
          ),
          ops.hlist.Length[String :: List[TwiceComposed] :: HNil],
          ops.nat.ToInt[Nat._2]
        )
      )
      .gen

  lazy val expectedBaseGen: Gen[Base] =
    MkGen
      .genericNonRecursiveCoproduct(
        Generic[Base],
        MkCoproductGen.ccons(
          MkGen
            .genericProduct(
              Generic[BaseDB],
              MkHListGen.hcons(
                Gen[Byte],
                MkHListGen.hcons(
                  Gen.genBoolean,
                  MkHListGen.hnil,
                  ops.hlist.Length[HNil],
                  ops.nat.ToInt[Nat._0]
                ),
                ops.hlist.Length[Boolean :: HNil],
                ops.nat.ToInt[Nat._1]
              )
            )
            .gen,
          MkCoproductGen.ccons(
            MkGen
              .genericProduct(
                Generic[BaseIS],
                MkHListGen.hcons(
                  Gen[Int],
                  MkHListGen.hcons(
                    Strict(genString),
                    MkHListGen.hnil,
                    ops.hlist.Length[HNil],
                    ops.nat.ToInt[Nat._0]
                  ),
                  ops.hlist.Length[String :: HNil],
                  ops.nat.ToInt[Nat._1]
                )
              )
              .gen,
            MkCoproductGen.ccons(
              MkGen
                .genericProduct(
                  Generic[BaseLast],
                  MkHListGen.hcons(
                    expectedSimpleGen,
                    MkHListGen.hnil,
                    ops.hlist.Length[HNil],
                    ops.nat.ToInt[Nat._0]
                  )
                )
                .gen,
              MkCoproductGen.cnil,
              ops.coproduct.Length[CNil],
              ops.nat.ToInt[Nat._0]
            ),
            ops.coproduct.Length[BaseLast :+: CNil],
            ops.nat.ToInt[Nat._1]
          ),
          ops.coproduct.Length[BaseIS :+: BaseLast :+: CNil],
          ops.nat.ToInt[Nat._2]
        )
      )
      .gen

  lazy val expectedCCWithSingletonGen: Gen[CCWithSingleton] =
    MkGen
      .genericProduct(
        Generic[CCWithSingleton],
        MkHListGen.hcons(
          Gen[Int],
          MkHListGen.hcons(
            ScalapropsShapeless.genSingletonType[Witness.`"aa"`.T],
            MkHListGen.hnil,
            ops.hlist.Length[HNil],
            ops.nat.ToInt[Nat._0]
          ),
          ops.hlist.Length[Witness.`"aa"`.T :: HNil],
          ops.nat.ToInt[Nat._1]
        )
      )
      .gen

  lazy val expectedBaseWithSingletonMainGen: Gen[BaseWithSingleton.Main] =
    MkGen
      .genericProduct(
        Generic[BaseWithSingleton.Main],
        MkHListGen.hcons(
          ScalapropsShapeless.genSingletonType[Witness.`"aa"`.T],
          MkHListGen.hnil,
          ops.hlist.Length[HNil],
          ops.nat.ToInt[Nat._0]
        )
      )
      .gen

  lazy val expectedBaseWithSingletonDummyGen: Gen[BaseWithSingleton.Dummy] =
    MkGen
      .genericProduct(
        Generic[BaseWithSingleton.Dummy],
        MkHListGen.hcons(
          Gen[Int],
          MkHListGen.hnil,
          ops.hlist.Length[HNil],
          ops.nat.ToInt[Nat._0]
        )
      )
      .gen

  lazy val expectedBaseWithSingletonGen: Gen[BaseWithSingleton] =
    MkGen
      .genericNonRecursiveCoproduct(
        Generic[BaseWithSingleton],
        MkCoproductGen.ccons(
          expectedBaseWithSingletonDummyGen,
          MkCoproductGen.ccons(
            expectedBaseWithSingletonMainGen,
            MkCoproductGen.cnil,
            ops.coproduct.Length[CNil],
            ops.nat.ToInt[Nat._0]
          ),
          ops.coproduct.Length[BaseWithSingleton.Main :+: CNil],
          ops.nat.ToInt[Nat._1]
        )
      )
      .gen

  lazy val expectedT1TreeGen: Gen[T1.Tree] =
    MkGen
      .genericRecursiveCoproduct(
        T1.Tree.recursive,
        Generic[T1.Tree],
        MkRecursiveCoproductGen.ccons(
          MkGen
            .genericProduct(
              Generic[T1.Leaf.type],
              shapeless.Lazy(
                MkHListGen.hnil
              )
            )
            .gen,
          MkRecursiveCoproductGen.ccons(
            MkGen
              .genericProduct(
                Generic[T1.Node],
                MkHListGen.hcons(
                  expectedT1TreeGen,
                  MkHListGen.hcons(
                    expectedT1TreeGen,
                    MkHListGen.hcons(
                      Gen[Int],
                      MkHListGen.hnil,
                      ops.hlist.Length[HNil],
                      ops.nat.ToInt[Nat._0]
                    ),
                    ops.hlist.Length[Int :: HNil],
                    ops.nat.ToInt[Nat._1]
                  ),
                  ops.hlist.Length[T1.Tree :: Int :: HNil],
                  ops.nat.ToInt[Nat._2]
                )
              )
              .gen,
            MkRecursiveCoproductGen.cnil,
            ops.coproduct.Length[CNil],
            ops.nat.ToInt[Nat._0]
          ),
          ops.coproduct.Length[T1.Node :+: CNil],
          ops.nat.ToInt[Nat._1]
        )
      )
      .gen

  lazy val expectedT2TreeGen: Gen[T2.Tree] =
    MkGen
      .genericRecursiveCoproduct(
        T2.Tree.recursive,
        Generic[T2.Tree],
        MkRecursiveCoproductGen.ccons(
          MkGen
            .genericProduct(
              Generic[T2.Leaf.type],
              MkHListGen.hnil
            )
            .gen,
          MkRecursiveCoproductGen.ccons(
            MkGen
              .genericProduct(
                Generic[T2.Node],
                MkHListGen.hcons(
                  expectedT2TreeGen,
                  MkHListGen.hcons(
                    expectedT2TreeGen,
                    MkHListGen.hcons(
                      Gen[Int],
                      MkHListGen.hnil,
                      ops.hlist.Length[HNil],
                      ops.nat.ToInt[Nat._0]
                    ),
                    ops.hlist.Length[Int :: HNil],
                    ops.nat.ToInt[Nat._1]
                  ),
                  ops.hlist.Length[T2.Tree :: Int :: HNil],
                  ops.nat.ToInt[Nat._2]
                )
              )
              .gen,
            MkRecursiveCoproductGen.cnil,
            ops.coproduct.Length[CNil],
            ops.nat.ToInt[Nat._0]
          ),
          ops.coproduct.Length[T2.Node :+: CNil],
          ops.nat.ToInt[Nat._1]
        )
      )
      .gen

  lazy val expectedBazGen: Gen[Baz.type] =
    MkGen
      .genericProduct(
        Generic[Baz.type],
        MkHListGen.hnil
      )
      .gen

  lazy val expectedFooGen: Gen[Foo] =
    MkGen
      .genericNonRecursiveCoproduct(
        Generic[Foo],
        MkCoproductGen.ccons(
          expectedBazGen,
          MkCoproductGen.cnil,
          ops.coproduct.Length[CNil],
          ops.nat.ToInt[Nat._0]
        )
      )
      .gen

  lazy val expectedFGen: Gen[F.type] =
    MkGen
      .genericProduct(
        Generic[F.type],
        MkHListGen.hnil
      )
      .gen

  lazy val expectedEGen: Gen[E] =
    MkGen
      .genericProduct(
        Generic[E],
        MkHListGen.hcons(
          Gen[Byte],
          MkHListGen.hcons(
            Gen.option(Gen[Int]),
            MkHListGen.hnil,
            ops.hlist.Length[HNil],
            ops.nat.ToInt[Nat._0]
          ),
          ops.hlist.Length[Option[Int] :: HNil],
          ops.nat.ToInt[Nat._1]
        )
      )
      .gen

  lazy val expectedDGen: Gen[D] =
    MkGen
      .genericNonRecursiveCoproduct(
        Generic[D],
        MkCoproductGen.ccons(
          expectedBazGen,
          MkCoproductGen.ccons(
            expectedEGen,
            MkCoproductGen.ccons(
              expectedFGen,
              MkCoproductGen.cnil,
              ops.coproduct.Length[CNil],
              ops.nat.ToInt[Nat._0]
            ),
            ops.coproduct.Length[F.type :+: CNil],
            ops.nat.ToInt[Nat._1]
          ),
          ops.coproduct.Length[E :+: F.type :+: CNil],
          ops.nat.ToInt[Nat._2]
        )
      )
      .gen

  lazy val expectedCGen =
    MkGen
      .genericProduct(
        Generic[C.type],
        MkHListGen.hnil
      )
      .gen

  lazy val expectedBGen =
    MkGen
      .genericProduct(
        Generic[B],
        MkHListGen.hcons(
          Gen[Int],
          MkHListGen.hcons(
            Strict(genString),
            MkHListGen.hnil,
            ops.hlist.Length[HNil],
            ops.nat.ToInt[Nat._0]
          ),
          ops.hlist.Length[String :: HNil],
          ops.nat.ToInt[Nat._1]
        )
      )
      .gen

  lazy val expectedAGen: Gen[A] =
    MkGen
      .genericNonRecursiveCoproduct(
        Generic[A],
        MkCoproductGen.ccons(
          expectedBGen,
          MkCoproductGen.ccons(
            expectedBazGen,
            MkCoproductGen.ccons(
              expectedCGen,
              MkCoproductGen.ccons(
                expectedEGen,
                MkCoproductGen.ccons(
                  expectedFGen,
                  MkCoproductGen.cnil,
                  ops.coproduct.Length[CNil],
                  ops.nat.ToInt[Nat._0]
                ),
                ops.coproduct.Length[F.type :+: CNil],
                ops.nat.ToInt[Nat._1]
              ),
              ops.coproduct.Length[E :+: F.type :+: CNil],
              ops.nat.ToInt[Nat._2]
            ),
            ops.coproduct.Length[C.type :+: E :+: F.type :+: CNil],
            ops.nat.ToInt[Nat._3]
          ),
          ops.coproduct.Length[Baz.type :+: C.type :+: E :+: F.type :+: CNil],
          ops.nat.ToInt[Nat._4]
        )
      )
      .gen

  lazy val expectedLGen: Gen[L] =
    MkHListGen
      .hcons(
        Gen[Int],
        MkHListGen.hcons(
          Strict(Gen.asciiString),
          MkHListGen.hnil,
          ops.hlist.Length.hnilLength[HNil],
          ops.nat.ToInt[Nat._0]
        ),
        ops.hlist.Length.hlistLength[String, HNil, Nat._0],
        ops.nat.ToInt[Nat._1]
      )
      .gen

  lazy val expectedRecGen: Gen[Rec] =
    MkHListGen
      .hcons[FieldType[Witness.`'i`.T, Int], Record.`'s -> String`.T, Nat._1](
        genFieldType[Witness.`'i`.T, Int](Gen[Int]),
        MkHListGen.hcons[FieldType[Witness.`'s`.T, String], HNil, Nat._0](
          genFieldType[Witness.`'s`.T, String](genString),
          MkHListGen.hnil,
          ops.hlist.Length.hnilLength[HNil],
          ops.nat.ToInt[Nat._0]
        ),
        ops.hlist.Length.hlistLength[FieldType[Witness.`'s`.T, String], HNil, Nat._0],
        ops.nat.ToInt[Nat._1]
      )
      .gen

  lazy val expectedC0Gen: Gen[C0] =
    MkCoproductGen
      .ccons(
        Gen[Int],
        MkCoproductGen.ccons(
          Strict(genString),
          MkCoproductGen.cnil,
          ops.coproduct.Length.cnilLength,
          ops.nat.ToInt[Nat._0]
        ),
        ops.coproduct.Length.coproductLength[String, CNil, Nat._0],
        ops.nat.ToInt[Nat._1]
      )
      .gen

  lazy val expectedUnionGen: Gen[Un] =
    MkCoproductGen
      .ccons[FieldType[Witness.`'i`.T, Int], Union.`'s -> String`.T, Nat._1](
        genFieldType[Witness.`'i`.T, Int](Gen[Int]),
        MkCoproductGen.ccons[FieldType[Witness.`'s`.T, String], CNil, Nat._0](
          genFieldType[Witness.`'s`.T, String](genString),
          MkCoproductGen.cnil,
          ops.coproduct.Length.cnilLength,
          ops.nat.ToInt[Nat._0]
        ),
        ops.coproduct.Length.coproductLength[FieldType[Witness.`'s`.T, String], CNil, Nat._0],
        ops.nat.ToInt[Nat._1]
      )
      .gen

  val compareSuccess = Property.forAll {
    val g = Gen[Int]
    compareGen(g, g)
  }

  val compareFailure = Property.forAll {
    val gen = Gen[Int]
    compareGen(gen, gen.map(_ + 1)) == false
  }

  val empty = Property.forAll {
    val expectedGen =
      MkGen
        .genericProduct(
          Generic[Empty.type],
          shapeless.Lazy(MkHListGen.hnil)
        )
        .gen

    val gen = Gen[Empty.type]
    compareGen(expectedGen, gen)
  }

  val emptyCC = Property.forAll {
    val expectedGen =
      MkGen
        .genericProduct(
          Generic[EmptyCC],
          shapeless.Lazy(MkHListGen.hnil)
        )
        .gen

    val gen = Gen[EmptyCC]
    compareGen(expectedGen, gen)
  }

  val simple = Property.forAll {
    val gen = Gen[Simple]
    compareGen(expectedSimpleGen, gen)
  }

  val simpleHList = Property.forAll {
    val gen = Gen[Int :: String :: Boolean :: HNil]
    compareGen(expectedIntStringBoolGen, gen)
  }

  val simpleCoproduct = Property.forAll {
    val gen = Gen[Int :+: String :+: Boolean :+: CNil]
    compareGen(expectedIntStringBoolCoproductGen, gen)
  }

  val composed = Property.forAll {
    val gen = Gen[Composed]
    compareGen(expectedComposedGen, gen)
  }

  val twiceComposed = Property.forAll {
    val gen = Gen[TwiceComposed]
    compareGen(expectedTwiceComposedGen, gen)
  }

  val composedOptList = Property.forAll {
    val gen = Gen[ComposedOptList]
    compareGen(expectedComposedOptListGen, gen)
  }

  val base = Property.forAll {
    val gen = Gen[Base]
    compareGen(expectedBaseGen, gen)
  }

  val tree1 = Property.forAll {
    val gen = Gen[T1.Tree]
    compareGen(expectedT1TreeGen, gen)
  }

  val tree2 = Property.forAll {
    val gen = Gen[T2.Tree]
    compareGen(expectedT2TreeGen, gen)
  }

  val a = Property.forAll {
    val gen = Gen[A]
    compareGen(expectedAGen, gen)
  }

  val d = Property.forAll {
    val gen = Gen[D]
    compareGen(expectedDGen, gen)
  }

  val list = Property.forAll {
    val expected = Gen.list[Int](Gen[Int])
    val gen = Gen[List[Int]]
    compareGen(expected, gen)
  }

  val option = Property.forAll {
    val expected = Gen.option(Gen[Int])
    val gen = Gen[Option[Int]]
    compareGen(expected, gen)
  }

  val singleton = Properties.list(
    Property.forAll {
      val expected = Gen.value[Witness.`2`.T](2)
      val gen = Gen[Witness.`2`.T]
      compareGen(expected, gen)
    }.toProperties("simple"),
    Property.forAll {
      compareGen(expectedCCWithSingletonGen, Gen[CCWithSingleton])
    }.toProperties("case class"),
    Property.forAll {
      compareGen(expectedBaseWithSingletonGen, Gen[BaseWithSingleton])
    }.toProperties("ADT")
  )

  val testShapeless = Properties.list(
    Property.forAll {
      val gen = Gen[L]
      compareGen(expectedLGen, gen)
    }.toProperties("hlist"),
    Property.forAll {
      val gen = Gen[Rec]
      compareGen(expectedRecGen, gen)
    }.toProperties("record"),
    Property.forAll {
      val gen = Gen[C0]
      compareGen(expectedC0Gen, gen)
    }.toProperties("coproduct"),
    Property.forAll {
      val gen = Gen[Un]
      compareGen(expectedUnionGen, gen)
    }.toProperties("union")
  )

  val coproductNotBias = {
    sealed abstract class X
    case object A extends X
    case object B extends X
    case object C extends X

    Property.forAll { seed: Long =>
      val result = Gen[X]
        .samples(
          listSize = 12000,
          seed = seed
        )
        .groupBy(_.getClass)
        .map { case (k, v) => k -> v.size }
      result.foreach { x =>
        val n = x._2
        if (n < 3000 || 5000 < n) {
          sys.error(result.toString)
        }
      }
      true
    }
  }

  object NoTC {
    import NoTCDefinitions._

    illTyped("""
      Gen[NoGenType]
    """)

    illTyped("""
      Gen[ShouldHaveNoGen]
    """)

    illTyped("""
      Gen[ShouldHaveNoGenEither]
    """)

    illTyped("""
      Gen[BaseNoGen]
    """)
  }
}
