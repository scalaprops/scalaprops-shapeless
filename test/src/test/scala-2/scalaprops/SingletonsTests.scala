package scalaprops

import shapeless.*
import scalaprops.Property.forAll

import Util.validateSingletons

object SingletonsTests extends Scalaprops {
  import SingletonsTestsDefinitions.*

  property("hnil") = forAll {
    validateSingletons[HNil](HNil)
  }

  property("caseObject") = forAll {
    validateSingletons[CaseObj.type](CaseObj)
  }

  property("emptyCaseClass") = forAll {
    validateSingletons[Empty](Empty())
  }

  property("adt") = forAll {
    validateSingletons[Base](BaseEmpty(), BaseObj)
  }

  property("adtNotAllSingletons") = forAll {
    validateSingletons[BaseMore](BaseMoreEmpty(), BaseMoreObj)
  }

  property("nonSingletonCaseClass") = forAll {
    validateSingletons[NonSingleton]()
  }
}
