# Acolyte

Acolyte is a JDBC driver designed for cases like mockup, testing, or any case you would like to be able to handle JDBC query by hand (or maybe that's only Chmeee's son on the Ringworld).

[![Build Status](https://secure.travis-ci.org/cchantep/acolyte.png?branch=master)](http://travis-ci.org/cchantep/acolyte)

This documentation can be read [online](http://cchantep.github.io/acolyte/).

## Requirements

* Java 1.6+

## Usage

Acolyte driver behaves as any other JDBC driver, that's to say you can get a connection from, by using the well-known `java.sql.DriverManager.getConnection(jdbcUrl)` (and its variants).

JDBC URL should match `"jdbc:acolyte:anything-you-want?handler=id"` (see after for details about `handler` parameter).

### Java

Using Maven 2/3+, Acolyte dependency can be resolved as following from your POM:

```xml
<!-- ... -->

  <repositories>
    <!-- ... -->
    <repository>
      <id>applicius-snapshots</id>
      <name>Applicius Maven2 Snapshots Repository</name>
      <url>https://raw.github.com/applicius/mvn-repo/master/snapshots/</url>
    </repository>
  </repositories>

  <dependencies>
    <!-- ... -->
    <dependency>
      <groupId>acolyte</groupId>
      <artifactId>acolyte-core</artifactId>
      <version>VERSION</version>
    </dependency>
  </dependencies>

```

Then code could be:

```java
import java.util.List;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Date;

import acolyte.ConnectionHandler;
import acolyte.StatementHandler;
import acolyte.CompositeHandler;
import acolyte.RowList3;
import acolyte.Result;

import acolyte.StatementHandler.Parameter;

import static acolyte.RowLists.rowList3;
import static acolyte.Rows.row3;

// ...

// Configure in anyway JDBC with following url,
// declaring handler registered with 'my-unique-id' will be used.
final String jdbcUrl = "jdbc:acolyte:anything-you-want?handler=my-unique-id"

// Prepare handler
StatementHandler handler = new CompositeHandler().
  withQueryDetection("^SELECT "). // regex test from beginning
  withQueryDetection("EXEC that_proc"). // second detection regex
  withUpdateHandler(new CompositeHandler.UpdateHandler() {
    // Handle execution of update statement (not query)
    public int apply(String sql, List<Parameter> parameters) {
      // ...
      return count;
    }
  }).withQueryHandler(new CompositeHandler.QueryHandler () {
    public Result apply(String sql, List<Parameter> parameters) {
      // ...

      // Prepare list of 2 rows
      // with 3 columns of types String, Float, Date
      RowList3<String, Float, Date> rows = 
        rowList3(String.class, Float.class, Date.class).
        withLabel(1, "String").withLabel(3, "Date"). // Optional: set labels
        append(row3("str", 1.2f, new Date(1, 2, 3))).
        append(row3("val", 2.34f, new Date(4, 5, 6)));

      return rows.asResult();
    }
  });

// Register prepared handler with expected ID 'my-unique-id'
acolyte.Driver.register("my-unique-id", handler);

// then ...
Connection con = DriverManager.getConnection(jdbcUrl);

// ... Connection |con| is managed through |handler|
```

You can see detailed [use cases](https://github.com/cchantep/acolyte/blob/master/core/src/test/java/usecase/JavaUseCases.java) whose expectations are visible in [specifications](https://github.com/cchantep/acolyte/blob/master/core/src/test/scala/acolyte/AcolyteSpec.scala).

If you just need/want to directly get connection from `acolyte.Driver`, without using JDBC driver registry, you can use Acolyte direct connection:

```java
Connection con = new acolyte.Driver().connection(yourHandlerInstance);
```

#### Result creation

Acolyte provides `Row` and `RowList` classes (and their sub-classes) to allow easy and typesafe creation of result.

Row lists can be built as following:

```java
import acolyte.RowList1;
import acolyte.RowList3;

import static acolyte.RowLists.rowList1;
import static acolyte.RowLists.rowList3; 

// ...

RowList1<String> list1 = RowLists.rowList1(String.class);

RowList3<Integer, Float, Character> list2 = RowLists.
  rowList3(Integer.class, Float.class, Character.class)
```

In previous example, `list1` is a list of row with 1 column whose class is `String` (`VARCHAR` as for JDBC/SQL type).
Considering `list2`, it is a list of row with 3 columns, whose classes are `Integer`, `Float` and `Character`.

Once you have declared your row list, and before turning it as result set, you can either add rows to it, or leave it empty.

```java
import java.sql.ResultSet;

import static acolyte.Rows.row1;

// ...

// we have declared list1 and list2 (see previous example)

list1 = list1.append(row1("str"));

ResultSet rs1 = list1.resultSet();
ResultSet rs2 = list2.resultSet();
```

From previous example, result set `rs1` will contain 1 row, whereas `rs2` is empty.

Take care to `list1 = list1.append(row1("str"));`. As provider `RowList` classes are immutable, you should get updated instance from `append` to work on the list containing added row. This is more safe, and allow to rewrite previous example like:

```java
ResultSet rs1 = list1.append(row1("str")).resultSet();
ResultSet rs2 = list2.resultSet();
```

### Scala

Module `acolyte-scala` provide a Scala DSL to use more friendly Acolyte features.

Using SBT, Acolyte dependency can resolved as following:

```scala
resolvers += 
  "Applicius Snapshots" at "https://raw.github.com/applicius/mvn-repo/master/snapshots/"

libraryDependencies += "acolyte" %% "acolyte-scala" % "VERSION" % "test"
```

Then code could be:

```scala
import java.sql.{ Connection ⇒ SqlConnection, Date, DriverManager }
import acolyte.{ Driver ⇒ AcolyteDriver, Execution }
import acolyte.RowLists.{ rowList1, rowList3 }
import acolyte.Rows.row3
import Acolyte._ // import DSL

// ...

// Prepare handler
val handler: CompositeHandler = handleStatement.
  withQueryDetection("^SELECT "). // regex test from beginning
  withQueryDetection("EXEC that_proc"). // second detection regex
  withUpdateHandler({ e: Execution ⇒
    if (e.sql.startsWith("DELETE ")) {
      // Process deletion ...
      /* deleted = */ 2;
    } else {
      // ... Process ...
      /* count = */ 1;
    }
  }).withQueryHandler({ e: Execution ⇒
    if (e.sql.startsWith("SELECT ")) {
      // Empty resultset with 1 text column declared
      rowList1(String.class).asResult
    } else {
      // ... EXEC that_proc
      // (see previous withQueryDetection)

      // Prepare list of 2 rows
      // with 3 columns of types String, Float, Date
      rowList3(classOf[String], classOf[Float], classOf[Date]).
        withLabels( // Optional: set labels
          1 -> "String",
          3 -> "Date")
        :+ row3("str", 1.2f, new Date(1l))
        :+ row3("val", 2.34f, new Date(2l))).
        asResult

    }
  })

// Register prepared handler with expected ID 'my-handler-id'
AcolyteDriver.register("my-handler-id", handler)

// ... then connection is managed through |handler|
DriverManager.getConnection(jdbcUrl)
```

You can see detailed [use cases](https://github.com/cchantep/acolyte/blob/master/scala/src/test/scala/acolyte/ScalaUseCases.scala) whose expectations are visible in [specifications](https://github.com/cchantep/acolyte/blob/master/scala/src/test/scala/acolyte/AcolyteSpec.scala).

### Limitations

- Limited datatype conversions.
- Binary datatype are not currently supported.
- Callable statement are not (yet) implemented.
- `ResultSet.RETURN_GENERATED_KEYS` is not supported.
- Pseudo-support for transaction.
- Currency types.

## Build

Acolyte can be built from these sources using SBT (0.12.2+): `sbt publish`

## Documentation

Documentation is generated using Maven 3: `mvn -f site.xml site`