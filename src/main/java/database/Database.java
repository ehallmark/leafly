package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static Connection conn;
    static {
        try {
            conn = DriverManager.getConnection("jdbc:postgresql://localhost/leaflydb?user=postgres&password=password&tcpKeepAlive=true");
        } catch(Exception e) {
            e.printStackTrace();
        }
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
