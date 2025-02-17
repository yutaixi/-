package com.im.db.mysql; 
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry; 
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
public class MySQLPool {
    private static volatile MySQLPool pool;
    private MysqlDataSource ds;
    private Map<Connection, Boolean> map;
  
    private String url = "jdbc:mysql://localhost:3306/weixin";
    private String username = "weixin";
    private String password = "1937294381";
    private int initPoolSize = 10;
    private int maxPoolSize = 200;
    private int waitTime = 100;
     
    private MySQLPool() {
        init();
    }
     
    public static MySQLPool getInstance() {
        if (pool == null) {
            synchronized (MySQLPool.class) {
                if(pool == null) {
                    pool = new MySQLPool();
                }
            }
        }
        return pool;
    }
     
    private void init() {
        try {
            ds = new MysqlDataSource();
            ds.setUrl(url);
            ds.setUser(username);
            ds.setPassword(password);
            ds.setCacheCallableStmts(true);
            ds.setConnectTimeout(1000);
            ds.setLoginTimeout(2000);
            ds.setUseUnicode(true);
            ds.setEncoding("UTF-8");
            ds.setZeroDateTimeBehavior("convertToNull");
            ds.setMaxReconnects(5);
            ds.setAutoReconnect(true);
            map = new HashMap<Connection, Boolean>();
            for (int i = 0; i < initPoolSize; i++) {
                map.put(getNewConnection(), true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
     
    public Connection getNewConnection() {
        try {
            return ds.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
     
    public synchronized Connection getConnection() {
        Connection conn = null;
        try {
            for (Entry<Connection, Boolean> entry : map.entrySet()) {
                if (entry.getValue()) {
                    conn = entry.getKey();
                    map.put(conn, false);
                    break;
                }
            }
            if (conn == null) {
                if (map.size() < maxPoolSize) {
                    conn = getNewConnection();
                    map.put(conn, false);
                } else {
                    wait(waitTime);
                    conn = getConnection();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }
     
    public void releaseConnection(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            if(map.containsKey(conn)) {
                if (conn.isClosed()) {
                    map.remove(conn);
                } else {
                    if(!conn.getAutoCommit()) {
                        conn.setAutoCommit(true);
                    }
                    map.put(conn, true);
                }
            } else {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}