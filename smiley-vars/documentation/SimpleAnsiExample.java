import com.markgrand.smileyvars.SmileyVarsTemplate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class SimpleAnsiExample {
    private static final String AISLE_BIN_SELECT
            = "SELECT item_number, quantity FROM bin_tbl WHERE 1=1(: and aisle=:aisle:)(: and bin_number=:bin :)";
    private static final SmileyVarsTemplate selectTemplate = SmileyVarsTemplate.ansiTemplate(AISLE_BIN_SELECT);

    public StorageLocation getLocation(Connection conn, String aisle, Integer bin) throws SQLException {
        Statement stmt = conn.createStatement();
        Map<String, Object> map = new HashMap<>();
        map.put("aisle", aisle);
        map.put("bin", bin);
        ResultSet rs = stmt.executeQuery(selectTemplate.apply(map));
        return new StorageLocation(rs);
    }

    private static class StorageLocation {
        StorageLocation(ResultSet rs){}
    }
}