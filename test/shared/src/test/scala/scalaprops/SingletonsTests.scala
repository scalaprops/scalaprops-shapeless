package scalaprops

import shapeless._
import scalaprops.Property.forAll

import Util.validateSingletons

object SingletonsTests extends Scalaprops {

  import SingletonsTestsDefinitions._

  val hnil = forAll {
    validateSingletons[HNil](HNil)
  }

  val caseObject = forAll {
    validateSingletons[CaseObj.type](CaseObj)
  }

  val emptyCaseClass = forAll {
    validateSingletons[Empty](Empty())
  }

  val adt = forAll {
    validateSingletons[Base](BaseEmpty(), BaseObj)
  }

  val adtNotAllSingletons = forAll {
    validateSingletons[BaseMore](BaseMoreEmpty(), BaseMoreObj)
  }

  val nonSingletonCaseClass = forAll {
    validateSingletons[NonSingleton]()
  }
}
