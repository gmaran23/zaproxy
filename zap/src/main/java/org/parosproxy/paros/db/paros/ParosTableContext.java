/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2015 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.parosproxy.paros.db.paros;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.parosproxy.paros.db.DatabaseException;
import org.parosproxy.paros.db.DbUtils;
import org.parosproxy.paros.db.RecordContext;
import org.parosproxy.paros.db.TableContext;

public class ParosTableContext extends ParosAbstractTable implements TableContext {

    private static final String TABLE_NAME = "CONTEXT_DATA";

    private static final String DATAID = "DATAID";
    private static final String CONTEXTID = "CONTEXTID";
    private static final String TYPE = "TYPE";
    private static final String DATA = "DATA";

    private PreparedStatement psRead = null;
    private PreparedStatement psInsert = null;
    private CallableStatement psGetIdLastInsert = null;
    private PreparedStatement psGetAllData = null;
    private PreparedStatement psGetAllDataForContext = null;
    private PreparedStatement psGetAllDataForContextAndType = null;
    private PreparedStatement psDeleteData = null;
    private PreparedStatement psDeleteAllDataForContext = null;
    private PreparedStatement psDeleteAllDataForContextAndType = null;

    public ParosTableContext() {}

    @Override
    protected void reconnect(Connection conn) throws DatabaseException {
        try {
            if (!DbUtils.hasTable(conn, TABLE_NAME)) {
                // Need to create the table
                DbUtils.execute(
                        conn,
                        "CREATE cached TABLE CONTEXT_DATA (dataid bigint generated by default as identity (start with 1), contextId int not null, type int not null, data varchar(1048576) default '')");
            }

            psRead = conn.prepareStatement("SELECT * FROM CONTEXT_DATA WHERE " + DATAID + " = ?");
            psInsert =
                    conn.prepareStatement(
                            "INSERT INTO CONTEXT_DATA ("
                                    + CONTEXTID
                                    + ","
                                    + TYPE
                                    + ","
                                    + DATA
                                    + ") VALUES (?, ?, ?)");
            psGetIdLastInsert = conn.prepareCall("CALL IDENTITY();");

            psDeleteData =
                    conn.prepareStatement(
                            "DELETE FROM CONTEXT_DATA WHERE "
                                    + CONTEXTID
                                    + " = ? AND "
                                    + TYPE
                                    + " = ? AND "
                                    + DATA
                                    + " = ?");
            psDeleteAllDataForContext =
                    conn.prepareStatement("DELETE FROM CONTEXT_DATA WHERE " + CONTEXTID + " = ?");
            psDeleteAllDataForContextAndType =
                    conn.prepareStatement(
                            "DELETE FROM CONTEXT_DATA WHERE "
                                    + CONTEXTID
                                    + " = ? AND "
                                    + TYPE
                                    + " = ?");

            psGetAllData = conn.prepareStatement("SELECT * FROM CONTEXT_DATA");
            psGetAllDataForContext =
                    conn.prepareStatement("SELECT * FROM CONTEXT_DATA WHERE " + CONTEXTID + " = ?");
            psGetAllDataForContextAndType =
                    conn.prepareStatement(
                            "SELECT * FROM CONTEXT_DATA WHERE "
                                    + CONTEXTID
                                    + " = ? AND "
                                    + TYPE
                                    + " = ?");
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.parosproxy.paros.db.paros.TableContext#read(long)
     */
    @Override
    public synchronized RecordContext read(long dataId) throws DatabaseException {
        try {
            psRead.setLong(1, dataId);

            try (ResultSet rs = psRead.executeQuery()) {
                RecordContext result = build(rs);
                return result;
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.parosproxy.paros.db.paros.TableContext#insert(int, int, java.lang.String)
     */
    @Override
    public synchronized RecordContext insert(int contextId, int type, String url)
            throws DatabaseException {
        try {
            psInsert.setInt(1, contextId);
            psInsert.setInt(2, type);
            psInsert.setString(3, url);
            psInsert.executeUpdate();

            long id;
            try (ResultSet rs = psGetIdLastInsert.executeQuery()) {
                rs.next();
                id = rs.getLong(1);
            }
            return read(id);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.parosproxy.paros.db.paros.TableContext#delete(int, int, java.lang.String)
     */
    @Override
    public synchronized void delete(int contextId, int type, String data) throws DatabaseException {
        try {
            psDeleteData.setInt(1, contextId);
            psDeleteData.setInt(2, type);
            psDeleteData.setString(3, data);
            psDeleteData.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.parosproxy.paros.db.paros.TableContext#deleteAllDataForContextAndType(int, int)
     */
    @Override
    public synchronized void deleteAllDataForContextAndType(int contextId, int type)
            throws DatabaseException {
        try {
            psDeleteAllDataForContextAndType.setInt(1, contextId);
            psDeleteAllDataForContextAndType.setInt(2, type);
            psDeleteAllDataForContextAndType.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.parosproxy.paros.db.paros.TableContext#deleteAllDataForContext(int)
     */
    @Override
    public synchronized void deleteAllDataForContext(int contextId) throws DatabaseException {
        try {
            psDeleteAllDataForContext.setInt(1, contextId);
            psDeleteAllDataForContext.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.parosproxy.paros.db.paros.TableContext#getAllData()
     */
    @Override
    public List<RecordContext> getAllData() throws DatabaseException {
        try {
            List<RecordContext> result = new ArrayList<>();
            try (ResultSet rs = psGetAllData.executeQuery()) {
                while (rs.next()) {
                    result.add(
                            new RecordContext(
                                    rs.getLong(DATAID),
                                    rs.getInt(CONTEXTID),
                                    rs.getInt(TYPE),
                                    rs.getString(DATA)));
                }
            }

            return result;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.parosproxy.paros.db.paros.TableContext#getDataForContext(int)
     */
    @Override
    public synchronized List<RecordContext> getDataForContext(int contextId)
            throws DatabaseException {
        try {
            List<RecordContext> result = new ArrayList<>();
            psGetAllDataForContext.setInt(1, contextId);
            try (ResultSet rs = psGetAllDataForContext.executeQuery()) {
                while (rs.next()) {
                    result.add(
                            new RecordContext(
                                    rs.getLong(DATAID),
                                    rs.getInt(CONTEXTID),
                                    rs.getInt(TYPE),
                                    rs.getString(DATA)));
                }
            }

            return result;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.parosproxy.paros.db.paros.TableContext#getDataForContextAndType(int, int)
     */
    @Override
    public synchronized List<RecordContext> getDataForContextAndType(int contextId, int type)
            throws DatabaseException {
        try {
            List<RecordContext> result = new ArrayList<>();
            psGetAllDataForContextAndType.setInt(1, contextId);
            psGetAllDataForContextAndType.setInt(2, type);
            try (ResultSet rs = psGetAllDataForContextAndType.executeQuery()) {
                while (rs.next()) {
                    result.add(
                            new RecordContext(
                                    rs.getLong(DATAID),
                                    rs.getInt(CONTEXTID),
                                    rs.getInt(TYPE),
                                    rs.getString(DATA)));
                }
            }

            return result;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    private RecordContext build(ResultSet rs) throws DatabaseException {
        try {
            RecordContext rt = null;
            if (rs.next()) {
                rt =
                        new RecordContext(
                                rs.getLong(DATAID),
                                rs.getInt(CONTEXTID),
                                rs.getInt(TYPE),
                                rs.getString(DATA));
            }
            return rt;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.parosproxy.paros.db.paros.TableContext#setData(int, int, java.util.List)
     */
    @Override
    public void setData(int contextId, int type, List<String> dataList) throws DatabaseException {
        this.deleteAllDataForContextAndType(contextId, type);
        for (String data : dataList) {
            this.insert(contextId, type, data);
        }
    }
}
