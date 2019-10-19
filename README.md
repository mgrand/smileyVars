# smileyVars
*A template engine for SQL*

SmileyVars is a lightweight template engine for SQL. It helps you avoid having to write similar SQL many times because small variations are needed.

SmileyVars is being initially developed with an integration to Spring&#x2bc;s JdbcTemplate. Other integrations are possible.

## Introduction by Example

Suppose we have a table that tracks the content of bins in a warehouse. Suppose that bins are identified by `aisle`, `level` and `bin_number`. A query to get information about the contents of one bin might look like

```SQL
SELECT item_number, quantity FROM bin_tbl
WHERE aisle=:aisle and level=:level and bin_number=:bin
```

The first thing that you might notice about this example is that the value to be substituted into the SQL are indicated by a name prefixed by ":". If we provide the values `aisle=32`, `level=4` and `bin=17` this will expand to

```SQL
SELECT item_number, quantity FROM bin_tbl
WHERE aisle=32 and level=4 and bin_number=17
```

Suppose that we would like to use the same SQL even for cases were we want to retrieve multiple rows. We could write

```SQL
SELECT item_number, quantity FROM bin_tbl
WHERE aisle=:aisle (: and level=:level :) (: and bin_number=:bin :)
```

What we have done is to bracket two parts of the query between `(:` and `:)`. When a portion of SQL is bracketed this way, if the bracketed portion contains any :*variables* and values are not supplied for all of the :*variables*, then that portion of the SQL is not included in the expansion. If all of the values are supplied for the above example then it will expand to exactly the same SQL as the previous example. However, if we supply just the values`aisle=32` and `bin=17`, it expands to 

```SQL
SELECT item_number, quantity FROM bin_tbl
WHERE aisle=32 and bin_number=17
```

if we supply just `aisle=32`, it expands to

```SQL
SELECT item_number, quantity FROM bin_tbl
WHERE aisle=32
```

What if we wanted to also have the flexibility of not specifying `aisle`? Just bracketing that part of the WHERE does
not work:

```SQL
SELECT item_number, quantity FROM bin_tbl
WHERE (: aisle=:aisle :) (: and level=:level :) (: and bin_number=:bin :)
```

If the first bracketed portion of this query is not in the expansion, it is not syntactically valid SQL. There is a 
simple syntactic trick that we can use to avoid this issue. We can begin the `WHERE` clause with `1=1` like this:
 
```SQL
SELECT item_number, quantity FROM bin_tbl
WHERE 1=1 (: and aisle=:aisle :) (: and level=:level :) (: and bin_number=:bin :)
```

This form of the SQL query allows us to supply all, some or none of the values and have it expand to a valid SQL query.

One thing to notice about this query is that the `SELECT` list does not include the `aisle`, `level` or `bin_number` 
columns. Because of this, when we get the results of the query, we do not know which bin result rows are associated
with.

A reasonable way to solve this problem is to just add those columns to the select list like this:

```SQL
SELECT item_number, quantity, aisle, level, bin_number FROM bin_tbl
WHERE 1=1 (: and aisle=:aisle :) (: and level=:level :) (: and bin_number=:bin :)
```

<b>Note:</b><br>
If a template contains more than one `:variable` between `(:` brackets 
`:)`, then the text between the brackets will be included in the template&#x2bc;s expansion only if 

<!--
In some cases there may be a concern about amount of data being transported from the database. In such cases you may 
prefer to only include columns in the `SELECT` list that are not constrained to a single value in the `WHERE` clause.s
-->

### Data Types
When a smileyVar template is expanded, the variables are replaced with SQL 
literals such as `123`, `'abc'` or `DATE '2020-04-28'`. Which type of
literal a variable is replaced with may depend on just the type of value
that is provided for the variable:
* Values that are instances of `Number` are formatted as SQL numeric 
literals.
* Values that are `String` objects are fomatted as SQL string literals.
* Values that are `Calendar` objects are formatted as SQL timestamp 
literals.

For example, if `rate` is the `Integer` value `31`, `dept` is the
`String` value `"nonce"` and `day` is the `Calendar` value 
`18FEB2020 13:43:56EST` then
```SQL
SELECT * FROM data WHERE 1=1 (: and rate=:rate:)(: and dept=:dept:)(: and day=:day:)
```
expands to
```SQL
SELECT * FROM data WHERE 1=1 and rate=31 and dept='nonce' and day=TIMESTAMP '2020-2-18 13:43:56-5:0'
```
There are some cases where you want to explicitly specify what kind of
literal a value should be formatted as. For example you may want a 
`Calendar` value to be formatted as a date literal (with no time 
component) rather than a timestamp. You can specify the formatting you
want for a variable by following it with a colon (:) and the name of a
format like this:
```SQL
SELECT * FROM data WHERE 1=1 (:and day=:day:date:)
```
If `day` is the `Calendar` value `18FEB2020 13:43:56EST` then the above 
example expands to
```SQL
SELECT * FROM data WHERE 1=1 and day=DATE '2020-2-18'
```

These are the currently supported formats:

| Format name | Default Mapping | Applies to Java Types                 | Produces          | Included in Template Type |
| ----------- |:---------------:| ------------------------------------- | ----------------- | ------------------------- |
| number      | yes             | `Number`                              | numeric literal   | all                       |
| string      | yes             | `String`                              | string literal    | all                       |
| timestamp   | yes             | `Date`, `Calendar`, `TemporalAccessor`| TIMESTAMP literal | all                       |
| date        | no              | `Date`, `Calendar`, `TemporalAccessor`| DATE literal      | all                       |

**Format name** is the name to use when explicitly specifying the format.

**Default Mapping** is yes if the format will automatically be used 
based on the type of value when there is no formatter specified.

**Applies to Java Types** shows the Java types that the formatter can be
used with. Note that some of thes types are abstract classes of 
interfaces that are extended or implemented by many concret classes. For
example, `Number` is extended by `BigDecimal`, `Double`, `Integer` and 
other classes that represent numeric values. `TemporalAccessor` is 
implemented by `Instant`, `LocalDateTime`, `Year` and other classes that
represent points in time.

**Produces** is the type of SQL literal that the formatter produces.

**Included in Template Type** has to do with a feature of SmilelyVars we
have not discussed yet. When you create a SmilelyVars template, it is
created for a particular dialect of SQL such as PostgreSQL, Oracle of
Transact-SQL (Sql Server). Some formatters are included in all template
types. Other formatters are for use in just one type of template.

<sub>**Note**: No dialect-specifed formats have been implemented for this version of SmileyVars.</sub>

## Using smileyVars
You can use smileyVars as a stand-alone pre-processor for SQL. However, 
more convenient integrations with other libraries are planned. In this 
section, we show you how to use smileyVars as a stand-alone preprocessor.

The first step is adding the smileyVars jar file to your project. The
recommended way to get the library is to allow maven or another 
dependency management tool to automatically download it. The maven 
dependency information is:
```
<dependency>
    <groupId>com.markgrand.smileyVars</groupId>
    <artifactId>smiley-vars</artifactId>
    <version>0.2.1-RELEASE</version>
</dependency>
```
Alternatively, you can build it yourself. Download the source from https://github.com/mgrand/smileyVars. You can use Maven to build it by
using the command
```
mvn clean install
```

Using SmilelyVars in your Java code is very simple. There are just two steps: 
* Create a template.  
* Apply values to the template.

This is exemplified by the following code sample:
```java
import com.markgrand.smileyVars.SmileyVarsTemplate;
...
public class SimpleAnsiExample {
    private static final SmileyVarsTemplate selectTemplate 
        = SmileyVarsTemplate.ansiTemplate("SELECT item, quant FROM bin_tbl WHERE 1=1(: and aisle=:aisle:)(: and bin_number=:bin :)");

    public StorageLocation getLocation(Connection conn, String aisle, Integer bin) throws SQLException {
        Statement stmt = conn.createStatement();
        Map<String, Object> map = new HashMap<>();
        map.put("aisle", aisle);
        map.put("bin", bin);
        ResultSet rs = stmt.executeQuery(selectTemplate.apply(map));
        ...
    }
    ...
}
```

A call to the static method `SmileyVarsTemplate.ansiTemplate` creates a 
template with the given body. The `ansiTemplate` method creates 
templates that support features that are common to most relational 
databases. There are other methods that create templates that are 
specialized for a specific type of relational database:

| Method                                 | Database   |
| -------------------------------------- | ---------- |
| `SimpleAnsiExample.postgresqlTemplate` | PostgreSql |
| `SimpleAnsiExample.oracleTemplate`     | Oracle     |
| `SimpleAnsiExample.sqlServerTemplate`  | SQL Server |

To apply values to a template, you need to put variable names and their 
values in a map. Then pass the map to the template&#x2bc;s `apply` 
method. The apply method returns the expanded template body.

### Logging

SmileyVars uses slf4j for its logging.  Slf4j integrates with all of the popular logging libraries (Logback, log4j, &hellip;). You can find documentation for
slf4j at <https://www.slf4j.org/manual.html>

## Roadmap
This is a list of planned future features:
* Availability of a pre-compiled jar in the Maven Central repository.
* Syntax for specifying explicit formatters/datatypes of simleyVars.
* Built-in support for additional datatypes:
    * BitSet
    * TimeDuration
    * Money
    * unique identifier/GUID 
    * boolean
* Integration with Spring JdbcTemplate
* Support for parsing national character set string literals.
* Support for parsing unicode string literals.

## Appendix: smileyVars Syntax

The EBNF grammar below describes the syntax of smileyVars. You can also 
view it as a [syntax/railroad diagram](file:documentation/sv-grammar.xhtml)
<small>(created using <https://www.bottlecaps.de/rr/ui>)</small>.


```EBNF
/*
 * smileyVars Grammar 
 */

template_body ::= (sql_text | bracketed_text)*

sql_text ::= (other_char | quoted_string | quoted_identifier | comment | '(' [^:] )*

quoted_string ::= ansi_quoted_string | postgresql_escape_string | postgresql_dollar_string
                | oracle_delimited_string

ansi_quoted_string ::= "'" ( [^'] | "'" "'" )* "'"

postgresql_escape_string ::= [eE] "'" ( [^'] | "''" | "\\" | "\'" )* "'"

/* The dollar_tag on each end of this must be the same */
postgresql_dollar_string ::= dollar_tag [^#x0]* dollar_tag

dollar_tag ::= '$' [^$]* '$'

oracle_delimited_string ::= [Qq] "'" ( "(" ([^)] | ")" [^'])* ")"
                                      | "[" ([^#x5D] | "]" [^'])* "]"
                                      | "{" ([^}] | "}" [^'])* "}"
                                      | "<" ([^>] | ">" [^'])* ">"
                                      | delimiter_char [^#x0]* delimiter_char) "'"
                /* Both occurrences of delimiter_char must be the same character */

quoted_identifier ::= '"' ( [^"] | '"' '"' )* '"'

other_char ::= [^'"(]

comment ::= line_comment | block_comment

line_comment ::= "--" [^#x0a#x0d]* [#x0a#x0d]

/* These should be able to nest as supported for PostgreSQL, SQLServer and DB2 */
block_comment ::= "/*" ([^*] | '*' [^/])* "*/"

bracketed_text ::= "(:" (bracketed_char | quoted_string | quoted_identifier
                         | comment | (":" var (":" type)?) ) ":)"

bracketed_char ::= [^'":]

var ::= [A-Za-z] [A-Za-z0-9_]*

type ::= [$A-Za-z] [$A-Za-z0-9_]*

<small>Copyright &copy; Mark Grand 2019</small>
