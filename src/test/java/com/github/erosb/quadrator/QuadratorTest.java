package com.github.erosb.quadrator;

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

import static com.github.erosb.quadrator.TypeMappingConfiguration.trivialMapping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
public class QuadratorTest {

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

    private Quadrator buildQuadrator() {
        MysqlDataSource ds = new MysqlDataSource();
        mysql.getHost();
        ds.setServerName(mysql.getHost());
        ds.setDatabaseName("test");
        ds.setPort(mysql.getFirstMappedPort());
        ds.setUser("root");
        ds.setPassword("test");
        return Quadrator.create(Quadrator.config()
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
        var quadrator = buildQuadrator();

        User u = quadrator.requireByPK(User.class, 1);

        assertEquals("asdasd", u.getName());
        assertEquals(1, u.getId());
    }

    @Test
    public void requireByPK_notFound() {
        assertThrows(EntityNotFoundException.class, () ->
                buildQuadrator().requireByPK(User.class, 10)
        );
    }

    @Test
    public void requireByPK_unhandledEntity() {
        assertThrows(UnknownEntityTypeException.class, () ->
                buildQuadrator().requireByPK(ConcurrentHashMap.class, 10));
    }

    @Test
    public void saveSuccess() {
        var quadrator = buildQuadrator();

        User u = new User(3, "John D");
        quadrator.save(u);

        var actual = quadrator.requireByPK(User.class, 3);
        assertEquals(u, actual);
    }

    @Test
    public void saveWithoutId() {
        var quadrator = buildQuadrator();

        User u = new User(null, "John D");
        u = quadrator.save(u);

        var actual = quadrator.requireByPK(User.class, 3);
        assertEquals(u, actual);
    }

}
