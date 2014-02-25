# Acolyte Scalac plugin

Scala compiler plugin

## Match component

Scala pattern matching involves use of either case class or extractor.

When extractor needs parameters to be created, it should be declared as stable identifier before `match` block. e.g. With regular expression matching:

```scala
val Letter = "[a-zA-Z]+".r

"String" match {
  case Letter ⇒ true
}
```

Match component included in this plugin provides syntax `~(extractorFactory[, bindings])` for rich pattern matching.

Consider following extractor, instantiated with one parameter:

```scala
case class Regex(e: String) {
  lazy val re = e.r
  def unapplySeq(target: Any): Option[List[String]] = re.unapplySeq(target)
}
```

Then provided rich syntax can be used as following:

```scala
str match {
  case ~(Regex("^a.*"))                      ⇒ 1 // no binding

  case ~(Regex("# ([A-Z]+).*"), a)           ⇒ 2 
  // if str == "# BCD123", then a = "BCD"

  case ~(Regex("([0-9]+);([a-z]+)"), (a, b)) ⇒ 3
  // if str == "234;xyz", then a = "234" and b = "xyz"

  case _                                     ⇒ 4
}
```

It will be refactored by plugin, so that required stable identifiers will be available for matching:

```scala
val Xtr1 = Regex("^a.*")
// ...
str match {
  case Xtr1 ⇒ 1 // no binding
  // ...
}
```

## Usage

Snapshot `1.0.15-SNAPSHOT1` is published on https://raw.github.com/applicius/mvn-repo/master/releases/ .

> If you have another `~` symbol, it will have to be renamed at `import pkg.{ ~ ⇒ renamed }`.

### SBT usage

Scalac plugin can be used with SBT project, using its [compiler plugins support](http://www.scala-sbt.org/0.12.3/docs/Detailed-Topics/Compiler-Plugins.html):

```scala
autoCompilerPlugins := true

addCompilerPlugin("org.eu.acolyte" %% "scalac-plugin" % "VERSION")

scalacOptions += "-P:acolyte:debug" // Optional
```

### Maven usage

Maven scala plugin [supports compiler plugin](http://scala-tools.org/mvnsites/maven-scala-plugin/example_scalac_plugins.html), so you can do:

```xml
<project>
  <!-- ... -->
  <build>
    <!-- ... -->
    <plugings>
      <plugin>
        <groupId>org.scala-tools</groupId>
        <artifactId>maven-scala-plugin</artifactId>
        <!-- ... version -->
        <configuration>
          <!-- ... -->
          <args><!-- Optional: enable debug -->
            <arg>-P:acolyte:debug</arg>
          </args>

          <compilerPlugins>
            <compilerPlugin>
              <groupId>org.eu.acolyte</groupId>
              <artifactId>scalac-plugin_2.10</artifactId>
              <version>VERSION</version>
            </compilerPlugin>
          </compilerPlugins>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

## Plugin options

There is few option for this plugin.

- `-P:acolyte:debug`: Display debug while compiling with (e.g. refactored match code).

## Compilation errors

As match component refactors `match` AST when `~(…, …)` is used, then if there is compilation error around that location will be mentioned as `/path/to/file.scala#refactored-match-M` (with `M` informational index of refactored match).

If there is an error with given extractor factory, you will get something like:

```
[error] /path/to/file.scala#refactored-match-M:1: Compilation error.
[error] Error details.
[error] val Xtr1 = B() // generated from ln L, col C
```

Comment `// generated from ln L, col C` indicates location in original code, before it gets refactored.

If there result from given extractor factory is not a valid extract, it will raise:

```
[error] /path/to/file.scala#refactored-match-M:1: value Xtr0 is not a case class constructor, nor does it have an unapply/unapplySeq method
[error] case Xtr1((a @ _)) => Nil // generated from ln L, col C
```