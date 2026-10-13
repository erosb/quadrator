package com.github.erosb.quadrator;

import java.sql.*;
import java.util.*;

public interface ResultSetAccessor {

    static ResultSetAccessor of(Map<String, Object> attributesForReconst, Map<String, List<Object>> members) {
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


            @Override
            public <E> List<E> getMany(String memberName) {
                return (List<E>) members.get(memberName);
            }
        };
    }

    static ResultSetAccessor of(ResultSet rs, Map<String, List<Object>> members) {
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

            @Override
            public <E> List<E> getMany(String memberName) {
                return (List<E>) members.get(memberName);
            }
        };
    }

    Object getObject(String columnName);

    Integer getInt(String columnName);

    String getString(String columnName);

    <E> List<E> getMany(String memberName);
}
