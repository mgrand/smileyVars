package com.markgrand.smileyvars.spring;

import com.markgrand.smileyvars.SmileyVarsPreparedStatement;
import com.markgrand.smileyvars.util.SqlConsumer;
import com.mockrunner.mock.jdbc.MockDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SmileyVarsJdbcTemplateTest {
    private static final ResultSetExtractor<List<Inventory>> rse = rs -> {
        List<Inventory> inventoryList = new ArrayList<>();
        while (rs.next()) {
            inventoryList.add(new Inventory(rs.getInt("aisle"), rs.getInt("level"), rs.getInt("bin_number"), rs.getInt("quantity"), rs.getString("item_number")));
        }
        return inventoryList;
    };

    private static final RowMapper<Inventory> rowMapper
            = (rs, rowNum) -> new Inventory(rs.getInt("aisle"), rs.getInt("level"), rs.getInt("bin_number"), rs.getInt("quantity"), rs.getString("item_number"));

    private static Connection h2Connection;
    private MockDataSource mockDataSource;

    @BeforeEach
    void setUp() throws Exception {
        assertTrue(h2Connection == null || h2Connection.isClosed());
        h2Connection = getConnection();
        Statement stmt = h2Connection.createStatement();
        stmt.execute("CREATE TABLE inventory ("
                             + "aisle INT,"
                             + "level INT,"
                             + "bin_number INT,"
                             + "item_number VARCHAR(100),"
                             + "quantity INT,"
                             + "CONSTRAINT inventory_pk PRIMARY KEY (aisle, level, bin_number)"
                             + ")");
        stmt.execute("INSERT INTO inventory (aisle, level, bin_number, item_number, quantity) VALUES (4, 1, 7, 'M234', 22);");
        stmt.execute("INSERT INTO inventory (aisle, level, bin_number, item_number, quantity) VALUES (4, 1, 8, 'M8473', 31);");
        stmt.execute("INSERT INTO inventory (aisle, level, bin_number, item_number, quantity) VALUES (4, 1, 9, 'M8479', 18);");
        stmt.execute("INSERT INTO inventory (aisle, level, bin_number, item_number, quantity) VALUES (4, 2, 3, 'M255X', 27);");
        h2Connection.commit();
        stmt.close();
        mockDataSource = new MockDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return SmileyVarsJdbcTemplateTest.this.getConnection();
            }
        };
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:test", "sa", "");
    }

    @AfterEach
    void tearDown() throws Exception {
        h2Connection.createStatement().execute("drop table inventory");
        h2Connection.close();
    }

    @Test
    void simpleConstructor() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate();
        assertNull(svjt.getDataSource());
        svjt.setDataSource(mockDataSource);
        assertEquals(mockDataSource, svjt.getDataSource());
        assertTrue(svjt.isLazyInit());
    }

    @Test
    void constructorDataSource() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        assertEquals(mockDataSource, svjt.getDataSource());
        assertTrue(svjt.isLazyInit());
    }

    @Test
    void constructorDataSourceLazy() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource, true);
        assertEquals(mockDataSource, svjt.getDataSource());
        assertTrue(svjt.isLazyInit());
    }

    @Test
    void constructorDataSourceNotLazy() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource, false);
        assertEquals(mockDataSource, svjt.getDataSource());
        assertFalse(svjt.isLazyInit());
    }

    @Test
    void withSmileyVarsPreparedStatement() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        assertEquals(22, (int) svjt.executeSmileyVars("SELECT quantity FROM inventory WHERE aisle = :aisle AND level = :level AND bin_number = :bin_number",
                svps -> {
                    try (ResultSet rs = svps.setInt("aisle", 4).setInt("level", 1).setInt("bin_number", 7).executeQuery()) {
                        assertTrue(rs.next());
                        return rs.getInt("quantity");
                    }
                }));
    }

    @Test
    void withSmileyVarsPreparedStatementNoDatasource() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate();
        assertThrows(IllegalStateException.class,
                () -> svjt.executeSmileyVars("SELECT SUM(quantity) quantity FROM inventory WHERE item_number = :item_number", (SmileyVarsPreparedStatement svps) -> 9));
    }

    @Test
    void queryPreparedStatementWithExtractor() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        List<Inventory> inventoryList = svjt.querySmileyVars("SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)",
                svps -> svps.setInt("aisle", 4).setInt("level", 1),
                rse);
        assertEquals(3, inventoryList.size());
        inventoryList.forEach(inventory -> {
            assertEquals(1, inventory.getLevel());
            assertEquals(4, inventory.getAisle());
        });
    }

    @Test
    void queryTemplateArraysWithExtractor() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        String[] names = {"aisle", "level"};
        Integer[] values = {4, 1};
        String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        List<Inventory> inventoryList = svjt.querySmileyVars(sql, names, values, rse);
        assertEquals(3, inventoryList.size());
        inventoryList.forEach(inventory -> {
            assertEquals(1, inventory.getLevel());
            assertEquals(4, inventory.getAisle());
        });
    }

    @Test
    void queryTemplateMapWithExtractor() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("aisle", 4);
        valueMap.put("level", 1);
        String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        List<Inventory> inventoryList = svjt.querySmileyVars(sql, valueMap, rse);
        assertEquals(3, inventoryList.size());
        inventoryList.forEach(inventory -> {
            assertEquals(1, inventory.getLevel());
            assertEquals(4, inventory.getAisle());
        });
    }

    @Test
    void queryPreparedStatementWithRowCallbackHandler() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        int[] count = {0};
        String sql = "SELECT item_number, quantity, level, aisle FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        svjt.querySmileyVars(sql, svps -> svps.setInt("aisle", 4).setInt("level", 1), rs -> {
            count[0]++;
            assertEquals(1, rs.getInt("level"));
            assertEquals(4, rs.getInt("aisle"));
        });
        assertEquals(3, count[0]);
    }

    @Test
    void queryTemplateArrayWithRowCallbackHandler() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        String[] names = {"aisle", "level"};
        Integer[] values = {4, 1};
        int[] count = {0};
        String sql = "SELECT item_number, quantity, level, aisle FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        svjt.querySmileyVars(sql, names, values, rs -> {
            count[0]++;
            assertEquals(1, rs.getInt("level"));
            assertEquals(4, rs.getInt("aisle"));
        });
        assertEquals(3, count[0]);
    }

    @Test
    void queryTemplateMapWithRowCallbackHandler() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("aisle", 4);
        valueMap.put("level", 1);
        int[] count = {0};
        String sql = "SELECT item_number, quantity, level, aisle FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        svjt.querySmileyVars(sql, valueMap, rs -> {
            count[0]++;
            assertEquals(1, rs.getInt("level"));
            assertEquals(4, rs.getInt("aisle"));
        });
        assertEquals(3, count[0]);
    }

    @Test
    void queryPreparedStatementWithRowMapper() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        List<Inventory> inventoryList = svjt.querySmileyVars("SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)",
                svps -> svps.setInt("aisle", 4).setInt("level", 1),
                rowMapper);
        assertEquals(3, inventoryList.size());
        inventoryList.forEach(inventory -> {
            assertEquals(1, inventory.getLevel());
            assertEquals(4, inventory.getAisle());
        });
    }

    @Test
    void queryTemplateArraysWithRowMapper() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        String[] names = {"aisle", "level"};
        Integer[] values = {4, 1};
        String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        List<Inventory> inventoryList = svjt.querySmileyVars(sql, names, values, rowMapper);
        assertEquals(3, inventoryList.size());
        inventoryList.forEach(inventory -> {
            assertEquals(1, inventory.getLevel());
            assertEquals(4, inventory.getAisle());
        });
    }

    @Test
    void queryTemplateMapWithRowMapper() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("aisle", 4);
        valueMap.put("level", 1);
        String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        List<Inventory> inventoryList = svjt.querySmileyVars(sql, valueMap, rowMapper);
        assertEquals(3, inventoryList.size());
        inventoryList.forEach(inventory -> {
            assertEquals(1, inventory.getLevel());
            assertEquals(4, inventory.getAisle());
        });
    }

    @Test
    void queryPreparedStatementForObjectWithRowMapper() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        Inventory inventory = svjt.queryForObjectSmileyVars("SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)",
                svps -> svps.setInt("aisle", 4).setInt("level", 1).setInt("bin_number", 8),
                rowMapper);
        assertEquals(1, inventory.getLevel());
        assertEquals(4, inventory.getAisle());
        assertEquals(8, inventory.getBinNumber());
        assertEquals("M8473", inventory.getItemNumber());
        assertEquals(31, inventory.getQuantity());
    }

    @Test
    void queryTemplateArraysForObjectWithRowMapper() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        String[] names = {"aisle", "level", "bin_number"};
        Object[] values = {4, 1, 8};
        String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        Inventory inventory = svjt.queryForObjectSmileyVars(sql, names, values, rowMapper);
        assertEquals(1, inventory.getLevel());
        assertEquals(4, inventory.getAisle());
        assertEquals(8, inventory.getBinNumber());
        assertEquals("M8473", inventory.getItemNumber());
        assertEquals(31, inventory.getQuantity());
    }

    @Test
    void queryTemplateMapForObjectWithRowMapper() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("aisle", 4);
        valueMap.put("level", 1);
        valueMap.put("bin_number", 8);
        String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        Inventory inventory = svjt.queryForObjectSmileyVars(sql, valueMap, rowMapper);
        assertEquals(1, inventory.getLevel());
        assertEquals(4, inventory.getAisle());
        assertEquals(8, inventory.getBinNumber());
        assertEquals("M8473", inventory.getItemNumber());
        assertEquals(31, inventory.getQuantity());
    }

    @Test
    void queryPreparedStatementForObjectWithRequiredType() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        Integer quantity = svjt.queryForObjectSmileyVars("SELECT quantity FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)",
                svps -> svps.setInt("aisle", 4).setInt("level", 1).setInt("bin_number", 8),
                Integer.class);
        assertEquals(31, quantity);
    }

    @Test
    void queryTemplateArraysForObjectWithRequiredType() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        String[] names = {"aisle", "level", "bin_number"};
        Object[] values = {4, 1, 8};
        String sql = "SELECT quantity FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        Integer quantity = svjt.queryForObjectSmileyVars(sql, names, values, Integer.class);
        assertEquals(31, quantity);
    }

    @Test
    void queryTemplateMapForObjectWithRequiredType() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("aisle", 4);
        valueMap.put("level", 1);
        valueMap.put("bin_number", 8);
        String sql = "SELECT quantity FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        Integer quantity = svjt.queryForObjectSmileyVars(sql, valueMap, Integer.class);
        assertEquals(31, quantity);
    }

    @Test
    void queryPreparedStatementForMap() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        Map<String, Object> resultMap = svjt.queryForMapSmileyVars("SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)",
                svps -> svps.setInt("aisle", 4).setInt("level", 1).setInt("bin_number", 8));
        assertEquals(5, resultMap.size());
        assertEquals(31, resultMap.get("quantity"));
    }

    @Test
    void queryTemplateArraysForMap() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        String[] names = {"aisle", "level", "bin_number"};
        Object[] values = {4, 1, 8};
        String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        Map<String, Object> resultMap = svjt.queryForMapSmileyVars(sql, names, values);
        assertEquals(5, resultMap.size());
        assertEquals(31, resultMap.get("quantity"));
    }

    @Test
    void queryTemplateMapForMap() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("aisle", 4);
        valueMap.put("level", 1);
        valueMap.put("bin_number", 8);
        String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        Map<String, Object> resultMap = svjt.queryForMapSmileyVars(sql, valueMap);
        assertEquals(5, resultMap.size());
        assertEquals(31, resultMap.get("quantity"));
    }

    @Test
    void queryPreparedStatementForList() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        List<String> itemNumbers = svjt.queryForListSmileyVars("SELECT item_number FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)",
                svps -> svps.setInt("aisle", 4).setInt("level", 1),
                String.class);
        assertEquals(3, itemNumbers.size());
    }

    @Test
    void queryTemplateArraysForList() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        String[] names = {"aisle", "level"};
        Object[] values = {4, 1};
        String sql = "SELECT item_number FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        List<String> itemNumbers = svjt.queryForListSmileyVars(sql, names, values, String.class);
        assertEquals(3, itemNumbers.size());
    }

    @Test
    void queryTemplateListForMap() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("aisle", 4);
        valueMap.put("level", 1);
        String sql = "SELECT item_number FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        List<String> itemNumbers = svjt.queryForListSmileyVars(sql, valueMap, String.class);
        assertEquals(3, itemNumbers.size());
    }

    @Test
    void queryPreparedStatementForListMap() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        List<Map<String, Object>> resultList = svjt.queryForListSmileyVars("SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)",
                svps -> svps.setInt("aisle", 4).setInt("level", 1));
        assertEquals(3, resultList.size());
        assertEquals(5, resultList.get(0).size());
    }

    @Test
    void queryTemplateArraysForListMap() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        String[] names = {"aisle", "level"};
        Object[] values = {4, 1};
        String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        List<Map<String, Object>> resultList = svjt.queryForListSmileyVars(sql, names, values);
        assertEquals(3, resultList.size());
        assertEquals(5, resultList.get(0).size());
    }

    @Test
    void queryTemplateListForListMap() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("aisle", 4);
        valueMap.put("level", 1);
        String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        List<Map<String, Object>> resultList = svjt.queryForListSmileyVars(sql, valueMap);
        assertEquals(3, resultList.size());
        assertEquals(5, resultList.get(0).size());
    }

    @Test
    void queryPreparedStatementForRowSet() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        SqlRowSet rowSet = svjt.queryForRowSetSmileyVars("SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)",
                svps -> svps.setInt("aisle", 4).setInt("level", 1));
        assertTrue(rowSet.next());
        assertTrue(rowSet.next());
        assertTrue(rowSet.next());
        assertTrue(rowSet.isLast());
    }

    @Test
    void queryTemplateArraysForRowSet() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        String[] names = {"aisle", "level"};
        Object[] values = {4, 1};
        String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        SqlRowSet rowSet = svjt.queryForRowSetSmileyVars(sql, names, values);
        assertTrue(rowSet.next());
        assertTrue(rowSet.next());
        assertTrue(rowSet.next());
        assertTrue(rowSet.isLast());
    }

    @Test
    void queryTemplateListForRowSet() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("aisle", 4);
        valueMap.put("level", 1);
        String sql = "SELECT * FROM inventory WHERE aisle = :aisle AND level = :level (: AND bin_number = :bin_number :)";
        SqlRowSet rowSet = svjt.queryForRowSetSmileyVars(sql, valueMap);
        assertTrue(rowSet.next());
        assertTrue(rowSet.next());
        assertTrue(rowSet.next());
        assertTrue(rowSet.isLast());
    }

    @Test
    void updatePreparedStatement() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        String selectSql = "SELECT quantity FROM inventory WHERE aisle=:aisle AND level=:level AND bin_number=:bin_number";
        SqlConsumer<SmileyVarsPreparedStatement> setter
                = svps -> svps.setInt("aisle", 4).setInt("level", 2).setInt("bin_number", 3);
        Integer originalQuantity = svjt.queryForObjectSmileyVars(selectSql, setter, Integer.class);
        assertEquals(27, originalQuantity);
        String updateSql = "UPDATE inventory SET quantity = quantity + 1 WHERE aisle=:aisle AND level=:level AND bin_number=:bin_number";
        int updateCount = svjt.updateSmileyVars(updateSql, setter);
        assertEquals(1, updateCount);
        Integer newQuantity = svjt.queryForObjectSmileyVars(selectSql, setter, Integer.class);
        assertEquals(28, newQuantity);
    }

    @Test
    void updateTemplateArrays() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        String[] names = {"aisle", "level", "bin_number"};
        Object[] values = {4, 2, 3};
        String updateSql = "UPDATE inventory SET quantity = quantity + 1 WHERE aisle=:aisle AND level=:level AND bin_number=:bin_number";
        int updateCount = svjt.updateSmileyVars(updateSql, names, values);
        assertEquals(1, updateCount);
        String selectSql = "SELECT quantity FROM inventory WHERE aisle=:aisle AND level=:level AND bin_number=:bin_number";
        Integer newQuantity = svjt.queryForObjectSmileyVars(selectSql, names, values, Integer.class);
        assertEquals(28, newQuantity);
    }

    @Test
    void updateTemplateMap() {
        SmileyVarsJdbcTemplate svjt = new SmileyVarsJdbcTemplate(mockDataSource);
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("aisle", 4);
        valueMap.put("level", 2);
        valueMap.put("bin_number", 3);
        String updateSql = "UPDATE inventory SET quantity = quantity + 1 WHERE aisle=:aisle AND level=:level AND bin_number=:bin_number";
        int updateCount = svjt.updateSmileyVars(updateSql, valueMap);
        assertEquals(1, updateCount);
        String selectSql = "SELECT quantity FROM inventory WHERE aisle=:aisle AND level=:level AND bin_number=:bin_number";
        Integer newQuantity = svjt.queryForObjectSmileyVars(selectSql, valueMap, Integer.class);
        assertEquals(28, newQuantity);
    }

    private static class Inventory {
        private final Integer aisle;
        private final Integer level;
        private final Integer bin_number;
        private final Integer quantity;
        private final String itemNumber;

        public Inventory(Integer aisle, Integer level, Integer bin_number, Integer quantity, String itemNumber) {
            this.aisle = aisle;
            this.level = level;
            this.bin_number = bin_number;
            this.quantity = quantity;
            this.itemNumber = itemNumber;
        }

        public Integer getAisle() {
            return aisle;
        }

        public Integer getLevel() {
            return level;
        }

        public Integer getBinNumber() {
            return bin_number;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public String getItemNumber() {
            return itemNumber;
        }

        @Override
        public String toString() {
            return "Inventory{" +
                           "aisle=" + aisle +
                           ", level=" + level +
                           ", bin_number=" + bin_number +
                           ", quantity=" + quantity +
                           ", itemNumber='" + itemNumber + '\'' +
                           '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Inventory inventory = (Inventory) o;
            return Objects.equals(aisle, inventory.aisle) &&
                           Objects.equals(level, inventory.level) &&
                           Objects.equals(bin_number, inventory.bin_number) &&
                           Objects.equals(quantity, inventory.quantity) &&
                           Objects.equals(itemNumber, inventory.itemNumber);
        }

        @Override
        public int hashCode() {
            return Objects.hash(aisle, level, bin_number, quantity, itemNumber);
        }
    }
}