package scalaprops

object SizeTests0 {
  import scalaprops.Shapeless._

  import SizeTestsDefinitions._

  val genTree = Gen[Tree]
}

object SizeTests extends Scalaprops {
  import SizeTestsDefinitions._

  assert(Leaf.depth == 0)
  assert(Branch(Leaf, Leaf).depth == 1)
  assert(Branch(Branch(Leaf, Leaf), Leaf).depth == 2)

  // manually calculated, grows approx. like log(size)
  val maxDepths = Seq(
    10 -> 5,
    100 -> 8,
    300 -> 10
  )

  val tree = Property.forAll {
    val seed = System.currentTimeMillis()
    val inspect = 10000

    for ((size, expectedMaxDepth) <- maxDepths) {
      val maxDepth = SizeTests0.genTree.infiniteStream(size = size, seed = seed).map(_.depth).take(inspect).max

      assert(maxDepth == expectedMaxDepth)
    }
    true
  }
}
