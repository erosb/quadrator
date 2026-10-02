package com.github.erosb.quadrator;

import com.mysql.cj.jdbc.*;
import org.h2.jdbcx.*;
import org.testcontainers.mysql.*;
import org.testcontainers.utility.*;

import javax.sql.*;
import java.sql.*;

public class DataSources {

    private static MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:latest"));

    static {
        mysql.start();
    }


    public static DataSource mem(boolean insertFixtreus) {
        try {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:test");
            if (insertFixtreus) {
                insertFixtures(dataSource.getConnection());
            }
            return dataSource;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void insertFixtures(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("drop table if exists `user`");
            st.execute("create table `user` (id int primary key auto_increment, name text)");
            st.executeUpdate("insert into `user` (name) values ('asdasd'), ('bsdbsd')");
        }
        try (Statement st = conn.createStatement()) {
            st.execute("drop table if exists `events`");
            st.execute("create table `events` (id varchar(36) primary key, description text)");
            st.executeUpdate("insert into `events` (id, description) values (uuid(), 'Test event 1'), (uuid(), 'Test event 2')");
        }
    }

    public static DataSource mysql(boolean insertFixtures) {
        try {
            MysqlDataSource ds = new MysqlDataSource();
            mysql.getHost();
            ds.setServerName(mysql.getHost());
            ds.setDatabaseName("test");
            ds.setPort(mysql.getFirstMappedPort());
            ds.setUser("root");
            ds.setPassword("test");
            if (insertFixtures) {
                insertFixtures(ds.getConnection());
            }
            return ds;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
