# scalaprops-shapeless

Generation of arbitrary case classes / ADTs instances with [scalaprops](https://github.com/scalaprops/scalaprops) and [shapeless](https://github.com/milessabin/shapeless) ported from [alexarchambault/scalacheck-shapeless](https://github.com/alexarchambault/scalacheck-shapeless).

[![Build Status](https://travis-ci.org/scalaprops/scalaprops-shapeless.svg)](https://travis-ci.org/scalaprops/scalaprops-shapeless)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.scalaprops/scalaprops-shapeless_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.scalaprops/scalaprops-shapeless_2.12)
[![scaladoc](http://javadoc-badge.appspot.com/com.github.scalaprops/scalaprops-shapeless_2.12.svg?label=scaladoc)](http://javadoc-badge.appspot.com/com.github.scalaprops/scalaprops-shapeless_2.12/scalaprops/index.html)
[![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.14.svg)](https://www.scala-js.org)

## Usage

Add to your `build.sbt`
```scala
libraryDependencies += "com.github.scalaprops" %% "scalaprops-shapeless" % "0.2.2"
```

scalaprops-shapeless depends on shapeless 2.3. It is built against scala 2.11, and 2.12.

Import the content of `org.scalaprops.Shapeless` close to where you want
`scalaprops.Gen` type classes to be automatically available for case classes / sealed hierarchies,

```scala
import org.scalaprops.Shapeless._

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
  foo: Foo =>
    // Ensure foo has the required property
}
```

without having to define yourself a `scalaprops.Gen` for `Foo`.

## Credits

scalaprops-shapeless ported from [alexarchambault/scalacheck-shapeless](https://github.com/alexarchambault/scalacheck-shapeless)

## License

Released under the Apache 2 license. See LICENSE file for more details.
