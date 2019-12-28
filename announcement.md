# SmileyVars: A template engine for SQL

SmileyVars is a lightweight java-based template engine for SQL. It helps you avoid having to write similar SQL many times because simple variations are needed.

SmileyVars is being initially developed with an integration to Springʼs JdbcTemplate. Other integrations are possible.

## Introduction by Example

Suppose we have a table that tracks the content of bins in a warehouse. Suppose that bins are identified by aisle, level and bin\_number. A query to get information about the contents of one bin might look like

    SELECT item_number, quantity FROM bin_tbl WHERE aisle=:aisle and level=:level and bin_number=:bin 

The first thing that you might notice about this example is that the value to be substituted into the SQL are indicated by a name prefixed by “:”. If we provide the values aisle=32, level=4 and bin=17 this will expand to

    SELECT item_number, quantity FROM bin_tbl WHERE aisle=32 and level=4 and bin_number=17 

Suppose that we would like to use the same SQL even for cases were we want to retrieve multiple rows. We could write

    SELECT item_number, quantity FROM bin_tbl WHERE aisle=:aisle (: and level=:level :) (: and bin_number=:bin :) 

What we have done is to bracket two parts of the query between (: and :). When a portion of SQL is bracketed this way, if the bracketed portion contains any :*variables* and values are not supplied for all of the :*variables*, then that portion of the SQL is not included in the expansion. If all of the values are supplied for the above example then it will expand to exactly the same SQL as the previous example. However, if we supply just the values aisle=32 and bin=17 with no value for bin, it expands to

    SELECT item_number, quantity FROM bin_tbl WHERE aisle=32 and bin_number=17 

If we supply just aisle=32, it expands to

    SELECT item_number, quantity FROM bin_tbl WHERE aisle=32 

What if we wanted to also have the flexibility of not specifying aisle? Just bracketing that part of the WHERE clause **does not work**:

    SELECT item_number, quantity FROM bin_tbl WHERE (: aisle=:aisle :) (: and level=:level :) (: and bin_number=:bin :) 

If the first bracketed portion of this query is not in the expansion, it is not valid SQL. There is a simple syntactic trick that we can use to avoid this issue. We can begin the WHERE  
 clause with 1=1 like this:

    SELECT item_number, quantity FROM bin_tbl WHERE 1=1 (: and aisle=:aisle :) (: and level=:level :) (: and bin_number=:bin :) 

This form of the SQL query allows us to supply all, some or none of the values and have it expand to a valid SQL query.

One thing to notice about this query is that the SELECT list does not include the aisle, level or bin\_number columns. Because of this, when we get the results of the query, we do not know which bin result rows are associated with.

A reasonable way to solve this problem is to just add those columns to the select list like this:

    SELECT item_number, quantity, aisle, level, bin_number FROM bin_tbl WHERE 1=1 (: and aisle=:aisle :) (: and level=:level :) (: and bin_number=:bin :) 

**Note:**  
If a template contains more than one :variable between (: brackets :), then the text between the brackets will be included in the templateʼs expansion only if values are supplied for all of the :variables.

SmileyVars is also useful for making UPDATEs more flexible. For example, if we want to update what is in a particular location, we could write

    UPDATE bin_tbl SET level=level (:, item_number=:item_number :)(:, quantity=:quantity :) WHERE aisle=:aisle AND level=:level AND bin_number=:bin_number 

This template requires that aisle, level and bin\_number  
 have values because they are not inside of (: :) brackets. This allows item or quantity to have values or not. If item or quantity does not have a value, it will not be updated.

The level=level is included in the UPDATE for the same reason we include 1=1 in WHERE  
 clauses. It does not change the effect of the command, but it does allow what follows it to be omitted by SmileyVars without causing any syntax errors.

For more information about SmileyVars see  [https://github.com/mgrand/smileyVars](https://github.com/mgrand/smileyVars) 