/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartloli.kafka.eagle.plugin.mysql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartloli.kafka.eagle.common.util.SystemConfigUtils;
import org.smartloli.kafka.eagle.plugin.util.JConstants;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Initializing database scripts.
 *
 * @author smartloli.
 * <p>
 * Created by Aug 7, 2017
 * <p>
 * Update by smartloli Sep 12, 2021
 * Settings prefixed with 'kafka.eagle.' will be deprecated, use 'efak.' instead.
 */
public class MySqlRecordSchema {

    public static final Logger LOG = LoggerFactory.getLogger(MySqlRecordSchema.class);

    /**
     * Load database schema script.
     */
    public static void schema() {
        String url = SystemConfigUtils.getProperty("efak.url");
        String username = SystemConfigUtils.getProperty("efak.username");
        String password = SystemConfigUtils.getProperty("efak.password");
        String host = url.split("//")[1].split("/")[0].split(":")[0];
        String port = url.split("//")[1].split("/")[0].split(":")[1];
        String db = url.split("//")[1].split("/")[1].split("\\?")[0];

        if (database(username, password, host, port, db)) {
            tables(username, password, host, port, db);
        }
    }

    private static void tables(String username, String password, String host, String port, String db) {
        Connection connection = MySqlStoragePlugin.getInstance(host + ":" + port + "/" + db, username, password);
        ResultSet rs = null;
        Statement stmt = null;
        List<String> tbls = new ArrayList<>();
        try {
            rs = connection.createStatement().executeQuery(JConstants.SHOW_TABLES);
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    tbls.add(rs.getString(i));
                }
            }

            for (String tbl : JConstants.TBLS) {
                if (tbls.contains(tbl)) {
                    LOG.info("The [" + tbl + "] table already exists. Do not need to create it.");
                } else {
                    String key = "CREATE_TABLE_" + tbl.toUpperCase();
                    stmt = connection.createStatement();
                    if (JConstants.KEYS.containsKey(key)) {
                        stmt.addBatch(JConstants.KEYS.get(key).toString());
                    }
                    if (JConstants.KEYS.containsKey(key + "_INSERT")) {
                        stmt.addBatch(JConstants.KEYS.get(key + "_INSERT").toString());
                    }
                    // add index
                    if (JConstants.KEYS.containsKey(key + "_INDEX")) {
                        stmt.addBatch(JConstants.KEYS.get(key + "_INDEX").toString());
                    }
                    if (JConstants.KEYS.containsKey(key + "_INDEX_TM")) {
                        stmt.addBatch(JConstants.KEYS.get(key + "_INDEX_TM").toString());
                    }
                    if (JConstants.KEYS.containsKey(key + "_INDEX_TM_CLUSTER")) {
                        stmt.addBatch(JConstants.KEYS.get(key + "_INDEX_TM_CLUSTER").toString());
                    }
                    int[] code = stmt.executeBatch();
                    if (code.length > 0) {
                        LOG.info("Create [" + tbl + "] has successed.");
                    } else {
                        LOG.error("Create [" + tbl + "] has failed.");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }

                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        MySqlStoragePlugin.close(connection);
    }

    private static boolean database(String username, String password, String host, String port, String db) {
        Connection connection = MySqlStoragePlugin.getInstance(host + ":" + port, username, password);
        ResultSet rs = null;
        List<String> dbs = new ArrayList<>();
        try {
            rs = connection.createStatement().executeQuery(JConstants.SHOW_DATABASES);
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    dbs.add(rs.getString(i));
                }
            }
            if (dbs.contains(db)) {
                LOG.info("The [" + db + "] database already exists. Do not need to create it.");
            } else {
                boolean status = connection.createStatement().execute(String.format(JConstants.CREATE_DB_SQL, db));
                if (!status) {
                    LOG.info("SQL statement affect the 0 records. Create [" + db + "] has successed.");
                } else {
                    LOG.error("Create [" + db + "] has failed.");
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        MySqlStoragePlugin.close(connection);
        return false;
    }
}
