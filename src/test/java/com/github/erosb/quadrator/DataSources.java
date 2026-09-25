package com.github.erosb.quadrator;

import com.mysql.cj.jdbc.*;
import org.h2.jdbcx.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.*;
import org.testcontainers.utility.*;

import javax.sql.*;
import java.sql.*;

public class DataSources {

    @Container
    public static MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:latest"));

    static {
        mysql.start();
    }


    public static DataSource mem() {
        try {
            Class.forName("org.h2.Driver");
            Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
            insertFixtures(conn);

            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
            return dataSource;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void insertFixtures(Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        st.execute("drop table if exists `user`");
        st.execute("create table users (id int primary key auto_increment, user_name text)");
        st.executeUpdate("insert into users (user_name) values ('asdasd'), ('bsdbsd')");
    }

    public static DataSource mysql() {
        try {
            MysqlDataSource ds = new MysqlDataSource();
            mysql.getHost();
            ds.setServerName(mysql.getHost());
            ds.setDatabaseName("test");
            ds.setPort(mysql.getFirstMappedPort());
            ds.setUser("root");
            ds.setPassword("test");
            Statement st = ds.getConnection().createStatement();
            st.execute("drop table if exists `user`");
            st.execute("create table `user` (id int primary key auto_increment, name text)");
            st.executeUpdate("insert into `user` (name) values ('asdasd'), ('bsdbsd')");
            return ds;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
