# Scala

Module `jdbc-scala` provides a Scala DSL to use more friendily Acolyte features.

Using SBT, Acolyte JDBC dependency can resolved as following:

```scala
libraryDependencies += "org.eu.acolyte" %% "jdbc-scala" % "VERSION" % "test"
```

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.eu.acolyte/jdbc-scala_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.eu.acolyte/jdbc-scala_2.11/)

Then code could be:

```scala
import java.sql.{ Date, DriverManager }
import acolyte.jdbc.{ Driver ⇒ AcolyteDriver, QueryExecution, UpdateExecution }
import acolyte.jdbc.RowLists.{ rowList1, rowList3 }
import acolyte.jdbc.AcolyteDSL
import acolyte.jdbc.Implicits._

// ...

// Prepare handler
val handler: CompositeHandler = AcolyteDSL.handleStatement.
  withQueryDetection(
    "^SELECT ", // regex test from beginning
    "EXEC that_proc"). // second detection regex
  withUpdateHandler { e: UpdateExecution ⇒
    if (e.sql.startsWith("DELETE ")) {
      // Process deletion ...
      /* deleted = */ 2;
    } else {
      // ... Process ...
      /* count = */ 1;
    }
  } withQueryHandler { e: QueryExecution ⇒
    if (e.sql.startsWith("SELECT ")) {
      // Empty resultset with 1 text column declared
      rowList1(classOf[String]).asResult
    } else {
      // ... EXEC that_proc
      // (see previous withQueryDetection)

      // Prepare list of 2 rows
      // with 3 columns of types String, Float, Date
      rowList3(classOf[String], classOf[Float], classOf[Date]).
        withLabels( // Optional: set labels
          1 -> "String",
          3 -> "Date")
        :+ ("str", 1.2f, new Date(1l)) // tuple as row 
        :+ ("val", 2.34f, null)).
        asResult

    }
  }

// Register prepared handler with expected ID 'my-handler-id'
AcolyteDriver.register("my-handler-id", handler)

// ... then connection is managed through |handler|
DriverManager.getConnection(jdbcUrl)
```

You can see detailed [use cases](https://github.com/cchantep/acolyte/blob/master/jdbc-scala/src/test/jdbc-scala/acolyte/ScalaUseCases.scala) whose expectations are visible in [specifications](https://github.com/cchantep/acolyte/blob/master/jdbc-scala/src/test/jdbc-scala/acolyte/AcolyteSpec.scala).

*See online [API documentation](http://http://acolyte.eu.org/jdbc-scaladoc)*.

## Connection 

As soon as you register Acolyte handler with a unique ID, corresponding connection can be resolved using JDBC URL including this ID as parameter.

```scala
// Register prepared handler with expected ID 'my-unique-id'
// handler: acolyte.jdbc.ConnectionHandler or acolyte.jdbc.StatementHandler instance
acolyte.jdbc.Driver.register("my-unique-id", handler)

// then ...
// ... later as handler has registered with 'my-unique-id'
val jdbcUrl = "jdbc:acolyte:anything-you-want?handler=my-unique-id"

import java.sql.{ Connection, DriverManager }

val con: Connection = DriverManager.getConnection(jdbcUrl)
// ... Connection |con| is managed through |handler|
```

It's also possible to get directly get an Acolyte connection, without using JDBC driver registry:

```scala
import acolyte.jdbc.AcolyteDSL

val con = AcolyteDSL.connection(handler)
```

### Connection properties

JDBC allows to pass properties to driver to customize connection creation:

```java
import acolyte.jdbc.AcolyteDSL

val con1 = DriverManager.getConnection(jdbcUrl, someJavaUtilProps)
val con2 = AcolyteDSL.connection(handler, "prop" -> "value"/* ... */)
```

(See [properties](./java.html#Connection_properties) meaningful for Acolyte)

## Query handler

In Scala query handler, pattern matching can be use to easily describe result case:

```scala
import acolyte.jdbc.{ QueryExecution, DefinedParameter, ExecutedParameter }

handleStatement.withQueryDetection("^SELECT").
  withQueryHandler { e: QueryExecution ⇒ 
    // ...

    e match {
      case QueryExecution(sql, DefinedParameter("str", _) :: Nil)
        if sql.startsWith("SELECT") ⇒
        // result when sql starts with SELECT
        // and there is only 1 parameter with "str" value

      case QueryExecution(_, 
        ExecutedParameter(_) :: ExecutedParameter(2) :: _) ⇒
        // result when there is at least 2 parameters for any sql
        // with the second having integer value 2
    }
  }
```

Partial function can also be used to describe handled cases:

```scala
/* ... */ withUpdateHandler {
  case UpdateExecution("SELECT 1", Nil) => /* case 1 */
  case UpdateExecution("SELECT 2", p1 :: Nil) => /* case 2 */
  /* ... */
}
```

Using [scalac plugin](./scalac-plugin.html), extractor `ExecutedStatement(regex, params)` can be used with [rich pattern matching](https://github.com/cchantep/acolyte/blob/master/jdbc-driver/src/test/jdbc-scala/acolyte/ExecutionSpec.scala):

```scala
e/*: Execution match {
  case ~(ExecutedStatement("^SELECT"), // if sql starts with SELECT
    (matchingSql, ExecutedParameter("strVal") :: Nil)) => /* ... */
}
```

If you plan only to handle query (not update) statements, `handleQuery` can be used:

```scala
handleQuery withQueryHandler { e ⇒ … }
```

When you only need connection for a single result case, `withQueryResult` is useful:

```scala
import acolyte.jdbc.AcolyteDSL

// res: acolyte.jdbc.QueryResult
val str: String = AcolyteDSL.withQueryResult(res) { connection ⇒ … }
```

### Result creation

Row lists can be built in the following way:

```scala
import acolyte.jdbc.{ RowList1, RowList3 }
import acolyte.jdbc.RowLists.{ rowList1, rowList3 }

// ...

val list1 = RowLists.rowList1(classOf[String])
val list2 = RowLists.rowList3(classOf[Int], classOf[Float], classOf[Char])
```

Column names/labels can also be setup (column first index is 1):

```scala
// ...

val list1up = list1.withLabel(1 -> "first label")
val list2up = list2.withLabel(2 -> "first label").withLabel(3 -> "third name")
```

Both column classes and names can be declared in bulk way:

```scala
import acolyte.jdbc.{ RowLists, RowList1, RowList3 }

// ...

val list1 = RowLists.rowList1(
  classOf[String] -> "first label")

val list2 = RowLists.rowList3(
  classOf[Int] -> "1st",
  classOf[Float] -> "2nd",
  classOf[Char] -> "3rd")
```

[RowLists factory](http://acolyte.eu.org/jdbc-driver-javadoc/acolyte/jdbc/RowLists.html) also provide convinience constructor for single column row list:

```scala
// Instead of RowLists.rowList1(classOf[String]) :+ stringRow) ...
RowLists.stringList() :+ stringRow

// Instead of RowLists.rowList1(Boolean.TYPE) :+ boolRow) ...
RowLists.booleanList() :+ boolRow

// Instead of RowLists.rowList1(Byte.TYPE) :+ byteRow) ...
RowLists.byteList() :+ byteRow

// Instead of RowLists.rowList1(Short.TYPE) :+ shortRow) ...
RowLists.shortList() :+ shortRow

// Instead of RowLists.rowList1(Integer.TYPE) :+ intRow) ...
RowLists.intList() :+ intRow

// Instead of RowLists.rowList1(Long.TYPE) :+ longRow) ...
RowLists.longList() :+ longRow

// Instead of RowLists.rowList1(Float.TYPE) :+ floatRow) ...
RowLists.floatList() :+ floatRow

// Instead of RowLists.rowList1(Double.TYPE) :+ doubleRow) ...
RowLists.doubleList() :+ doubleRow

// Instead of RowLists.rowList1(classOf[BigDecimal]) :+ bdRow) ...
RowLists.bigDecimalList() :+ bdRow

// Instead of RowLists.rowList1(classOf[Date]) :+ dateRow) ...
RowLists.dateList() :+ dateRow

// Instead of RowLists.rowList1(classOf[Time]) :+ timeRow) ...
RowLists.timeList() :+ timeRow

// Instead of RowLists.rowList1(classOf[Timestamp]) :+ tsRow) ...
RowLists.timestampList() :+ tsRow
```

Once you have declared your row list, and before turning it as result set, you can either add rows to it, or leave it empty.

```scala
import java.sql.ResultSet

// ...

val rs1: ResultSet = list1.append("str").resultSet()
val rs2: ResultSet = list2.resultSet()
```

## Generated keys

Update case not only returning update count but also generated keys can be represented with `UpdateResult`:

```java
import acolyte.jdbc.{ AcolyteDSL, RowLists }

// Result with update count == 1 and a generated key 2L
AcolyteDSL.updateResult(1, RowLists.longList.append(2L))
```

Keys specified on result will be given to JDBC statement `.getGeneratedKeys`.

## Implicits

To ease use of AcolyteDSL DSL, implicit conversions are provided for `QueryResult`, query handler `QueryExecution => QueryResult` can be defined with following alternatives.

- `QueryExecution => RowList`: query result as given row lists.
- `QueryExecution => T`: one row with one column of type `T` as query result.

In same way, implicit conversions are provided for `UpdateResult` allowing update handler to defined as following.

- `UpdateExecution => Int`: update count as update result.

```scala
import acolyte.jdbc.{ QueryResult, RowLists, UpdateResult }
import acolyte.jdbc.Implicits._

// Alternative definitions for query handler
val qh1: QueryExecution => QueryResult = 
  // Defined from QueryExecution => RowList
  { ex: QueryExecution =>
    RowLists.rowList2(classOf[String], classOf[Int]) :+ ("str", 2)
  }

val qh2: QueryExecution => QueryResult = // Defined from QueryExecution => T
  { ex: QueryExecution => 
    val ur: UpdateResult = "str" // as RowList1[String] with only one row
    ur
  }

// Alternative definition for update handler
val uh1: UpdateExecution => UpdateResult =
  // Defined from UpdateExecution => Int
  { ex: UpdateExecution => /* update count = */ 2 }
```

## Debug utility

Acolyte can be use to create [scope of debuging](http://acolyte.eu.org/jdbc-scaladoc/#acolyte.jdbc.AcolyteDSL$@debuging[A]%28printer:acolyte.jdbc.QueryExecution=%3EUnit%29%28f:java.sql.Connection=%3EA%29:Unit), to check what is executed in the JDBC layer.

```scala
import acolyte.jdbc.AcolyteDSL

AcolyteDSL.debuging() { con =>
  val stmt = con.prepareStatement("SELECT * FROM Test WHERE id = ?")
  stmt.setString(1, "foo")
  stmt.executeQuery()
}
```

The previous example will print `Executed query: QueryExecution(SELECT * FROM Test WHERE id = ?,List(Param(foo, VARCHAR)))`  on the stdout.

The default printer (using stdout) can be replaced by any function `acolyte.jdbc.QueryExecution => Unit` (see [`QueryExecution`](http://acolyte.eu.org/jdbc-scaladoc/#acolyte.jdbc.QueryExecution) API).

```scala
import acolyte.jdbc.AcolyteDSL

AcolyteDSL.debuging({ exec => myLogger.debug(s"Executed: $exec") }) { con =>
  ???
}
```

> Any function using JDBC which is called within the scope, will be passed to the debug printer.
