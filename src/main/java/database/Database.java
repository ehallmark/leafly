package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {
    private static Connection conn;
    static {
        try {
            conn = DriverManager.getConnection("jdbc:postgresql://localhost/leaflydb?user=postgres&password=password&tcpKeepAlive=true");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Map<String,Object>> loadData(String table, String... fields) throws SQLException {
        List<Map<String,Object>> data = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement("select "+String.join(",",fields)+" from "+table);
        ResultSet rs = ps.executeQuery();
        while(rs.next()) {
            Map<String,Object> row = new HashMap<>();
            for(int i = 0; i < fields.length; i++) {
                row.put(fields[i], rs.getObject(i+1));
            }
            data.add(row);
        }
        return data;
    }

    public static Map<String,List<Object>> loadMap(String table, String idField, String valueField) throws SQLException {
        Map<String,List<Object>> data = new HashMap<>();
        PreparedStatement ps = conn.prepareStatement("select "+idField+","+valueField+" from "+table);
        ResultSet rs = ps.executeQuery();
        while(rs.next()) {
            String id = rs.getString(1);
            data.putIfAbsent(id, new ArrayList<>());
            data.get(id).add(rs.getObject(2));
        }
        return data;
    }

    public static Map<String,Map<String,Double>> loadMapWithValue(String table, String idField, String valueField, String numericField) throws SQLException {
        Map<String,Map<String,Double>> data = new HashMap<>();
        PreparedStatement ps = conn.prepareStatement("select "+idField+","+valueField+","+numericField+" from "+table);
        ResultSet rs = ps.executeQuery();
        while(rs.next()) {
            String id = rs.getString(1);
            data.putIfAbsent(id, new HashMap<>());
            data.get(id).put(rs.getString(2), rs.getDouble(3));
        }
        return data;
    }

    public static List<String> loadEffects() throws SQLException {
        return loadSingleColumn("effect", "effects");
    }

    public static List<String> loadProfiles() throws SQLException {
        return loadSingleColumn("profile", "profiles");
    }

    public static List<String> loadFlavors() throws SQLException {
        return loadSingleColumn("flavor", "flavors");
    }

    public static List<String> loadParentStrains() throws SQLException {
        return loadSingleColumn("strain_id", "parent_strains");
    }

    public static List<String> loadStrains() throws SQLException {
        return loadSingleColumn("id", "strains");
    }

    private static List<String> loadSingleColumn(String columnName, String tableName) throws SQLException {
        List<String> data = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement("select "+columnName+" from "+tableName);
        ResultSet rs = ps.executeQuery();
        while(rs.next()) {
            data.add(rs.getString(1));
        }
        return data;
    }

}
