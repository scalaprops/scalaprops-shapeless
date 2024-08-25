# scalaprops-shapeless

Generation of arbitrary case classes / ADTs instances with [scalaprops](https://github.com/scalaprops/scalaprops) and [shapeless](https://github.com/milessabin/shapeless) ported from [alexarchambault/scalacheck-shapeless](https://github.com/alexarchambault/scalacheck-shapeless).

[![Maven Central](https://img.shields.io/maven-central/v/com.github.scalaprops/scalaprops-shapeless_3.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.scalaprops/scalaprops-shapeless_3)
[![scaladoc](https://javadoc.io/badge2/com.github.scalaprops/scalaprops-shapeless_3/javadoc.svg)](https://javadoc.io/doc/com.github.scalaprops/scalaprops-shapeless_3)

## Usage

Add to your `build.sbt`

```scala
libraryDependencies += "com.github.scalaprops" %% "scalaprops-shapeless" % "0.6.0"
```

Import the content of `scalaprops.ScalapropsShapeless` close to where you want
`scalaprops.Gen` type classes to be automatically available for case classes / sealed hierarchies,

```scala
import scalaprops.ScalapropsShapeless._

//  If you defined:

// case class Foo(i: Int, s: String, blah: Boolean)
// case class Bar(foo: Foo, other: String)

// sealed trait Base
// case class BaseIntString(i: Int, s: String) extends Base
// case class BaseDoubleBoolean(d: Double, b: Boolean) extends Base

//  then you can now do

Gen[Foo]
Gen[Bar]
Gen[Base]
```

and in particular, while writing property-based tests,

```scala
val `some property about Foo` = Property.forAll {
  (foo: Foo) =>
    // Ensure foo has the required property
}
```

without having to define yourself a `scalaprops.Gen` for `Foo`.

## Credits

scalaprops-shapeless ported from [alexarchambault/scalacheck-shapeless](https://github.com/alexarchambault/scalacheck-shapeless)

## License

Released under the Apache 2 license. See LICENSE file for more details.
