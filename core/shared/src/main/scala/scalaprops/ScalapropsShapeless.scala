package scalaprops

import derive._

sealed abstract class ScalapropsShapeless
    extends SingletonInstances
    with HListInstances
    with CoproductInstances
    with DerivedInstances
    with FieldTypeInstances

object ScalapropsShapeless extends ScalapropsShapeless

@deprecated(message = "use ScalapropsShapeless instead", since = "0.4.3")
object Shapeless extends ScalapropsShapeless
