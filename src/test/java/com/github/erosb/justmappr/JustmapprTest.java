package com.github.erosb.justmappr;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.mysql.cj.jdbc.MysqlDataSourceFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainerProvider;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.erosb.justmappr.TypeMappingConfiguration.trivialMapping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
public class JustmapprTest {

    @Container
    public MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:latest"));

//    new GenericContainer(DockerImageName.parse("mysql:8.0"))
//            .withExposedPorts(3306);

    @BeforeEach @SneakyThrows
    void insertFixtures() {
        mysql.start();
        Class.forName("org.h2.Driver");
        Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();

        MysqlDataSource ds = new MysqlDataSource();
        mysql.getHost();
        ds.setServerName(mysql.getHost());
        ds.setDatabaseName("test");
        ds.setPort(mysql.getFirstMappedPort());
        ds.setUser("root");
        ds.setPassword("test");
        ds.getConnection();


        Connection conn = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        Statement st = conn.createStatement();
        st.execute("create table `user` (id int primary key auto_increment, name text)");
        st.executeUpdate("insert into `user` (name) values ('asdasd'), ('bsdbsd')");
    }

    private Justmappr buildJustmappr() {
        MysqlDataSource ds = new MysqlDataSource();
        mysql.getHost();
        ds.setServerName(mysql.getHost());
        ds.setDatabaseName("test");
        ds.setPort(mysql.getFirstMappedPort());
        ds.setUser("root");
        ds.setPassword("test");
        return Justmappr.create(Justmappr.config()
                .dataSource(ds)
                .typeMapping(trivialMapping(User.class, "id"))
                .build());
    }

    @AfterEach
    void tearDown() throws SQLException {
        Connection conn = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        Statement st = conn.createStatement();
        st.execute("drop table `user`");
    }

    @Test
    public void requireByPK_success() {
        var justmappr = buildJustmappr();

        User u = justmappr.requireByPK(User.class, 1);

        assertEquals("asdasd", u.getName());
        assertEquals(1, u.getId());
    }

    @Test
    public void requireByPK_notFound() {
        assertThrows(EntityNotFoundException.class, () ->
                buildJustmappr().requireByPK(User.class, 10)
        );
    }

    @Test
    public void requireByPK_unhandledEntity() {
        assertThrows(UnknownEntityTypeException.class, () ->
                buildJustmappr().requireByPK(ConcurrentHashMap.class, 10));
    }

    @Test
    public void saveSuccess() {
        var justmappr = buildJustmappr();

        User u = new User(3, "John D");
        justmappr.save(u);

        var actual = justmappr.requireByPK(User.class, 3);
        assertEquals(u, actual);
    }

    @Test
    public void saveWithoutId() {
        var justmappr = buildJustmappr();

        User u = new User(null, "John D");
        u = justmappr.save(u);

        var actual = justmappr.requireByPK(User.class, 3);
        assertEquals(u, actual);
    }

}
