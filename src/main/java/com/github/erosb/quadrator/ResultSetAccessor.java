package com.github.erosb.quadrator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public interface ResultSetAccessor {

    static ResultSetAccessor of(Map<String, Object> attributesForReconst) {
        return new ResultSetAccessor() {
            @Override
            public Object getObject(String columnName) {
                return attributesForReconst.get(columnName);
            }

            @Override
            public Integer getInt(String columnName) {
                return (Integer) attributesForReconst.get(columnName);
            }

            @Override
            public String getString(String columnName) {
                return (String) attributesForReconst.get(columnName);
            }
        };
    }

    static ResultSetAccessor of(ResultSet rs) {
        return new ResultSetAccessor() {
            @Override
            public Object getObject(String columnName) {
                try {
                    return rs.getObject(columnName);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Integer getInt(String columnName) {
                try {
                    return rs.getInt(columnName);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String getString(String columnName) {
                try {
                    return rs.getString(columnName);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    Object getObject(String columnName);

    Integer getInt(String columnName);

    String getString(String columnName);
}
