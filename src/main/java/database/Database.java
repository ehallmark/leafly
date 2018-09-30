package database;

import com.google.gson.Gson;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    public static double[][] loadSimilarityMatrix(String table, String key, String valKey, List<String> labels) throws SQLException {
        Map<String,List<Object>> data = loadMap(table, key, valKey);
        double[][] matrix = new double[labels.size()][labels.size()];
        for(int i = 0; i < matrix.length; i++) {
            matrix[i][i] = 1d; // set diag
        }
        Map<String,Integer> labelIdxMap = IntStream.range(0, labels.size()).boxed()
                .collect(Collectors.toMap(i->(String) labels.get(i), i->i));
        double[] counts = new double[labels.size()];
        data.forEach((id, list)->{
            for(int i = 0; i < list.size(); i++) {
                Object xi = list.get(i);
                for(int j = 0; j < list.size(); j++) {
                    Object xj = list.get(j);
                    int idx1 = labelIdxMap.get((String)xi);
                    int idx2 = labelIdxMap.get((String)xj);
                    matrix[idx1][idx2] ++;
                    //matrix[idx2][idx1] ++;
                    counts[idx1] ++;
                    //counts[idx2] ++;
                }
            }
        });
        for(int i = 0; i < labels.size(); i++) {
            for(int j = 0; j < labels.size(); j++) {
                matrix[i][j] /= Math.max(1d, counts[i] + counts[j]);
            }
        }
        System.out.println("Matrix for "+table+": "+String.join(", ", labels));
        System.out.println(new Gson().toJson(matrix));
        return matrix;
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
