# smileyVars
*A template engine for SQL*


SmileyVars is a lightweight template engine for SQL. It helps you avoid having to write similar SQL many times because small variations are needed.

SmileyVars is being initially developed with an integration to Spring's JdbcTemplate. Other integrations are possible.

## Introduction by Example

Suppose we have a table that tracks the content of bins in a warehouse. Suppose that bins are identified by `aisle`, `level` and `bin_number`. A query to get information about the contents of one bin might look like

```
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

In some cases there may be a concern about amount of data being transportd from the database. In such cases you may 
prefer to only include columns in the `SELECT` list that are not constrained to a single value in the `WHERE` clause.s

## Using smileyVars
