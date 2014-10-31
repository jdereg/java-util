package com.cedarsoftware.ncube;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by ken on 8/22/2014.
 */
public class DataSourcePersistenceProxy extends AbstractJdbcPersistenceProxy {

    private DataSource _source;

    public DataSourcePersistenceProxy(DataSource source, Class service, Object adapter) {
        super(service, adapter);
        if (source == null) {
            throw new NullPointerException("DataSource cannot be null...");
        }
        _source = source;
    }

    public Connection getConnection() throws SQLException {
        return _source.getConnection();
    }
}
