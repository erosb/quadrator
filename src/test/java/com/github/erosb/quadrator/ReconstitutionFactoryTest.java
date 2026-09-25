package com.github.erosb.quadrator;

import lombok.*;
import org.h2.jdbcx.*;
import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

public class ReconstitutionFactoryTest {


    @BeforeEach
    @SneakyThrows
    void insertFixtures() {
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
        Statement st = conn.createStatement();
        st.execute("create table users (id int primary key auto_increment, user_name text)");
        st.executeUpdate("insert into users (user_name) values ('asdasd'), ('bsdbsd')");
    }

    @SneakyThrows
    private Quadrator buildQuadrator() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        return Quadrator.create(Quadrator.config()
                .dataSource(dataSource)
                .typeMapping(TypeMappingConfiguration.builderFor(User.class)
                        .relationName("users")
                        .primaryKeyMapping(User::getId, "id")
                        .fieldMapping(User::getName, "user_name")
                        .reconstitutionFactory(rs -> new User(rs.getInt("id"), rs.getString("user_name")))
                        .build()
                )
                .build());
    }

    @Test
    void reconstitutionFactoryTest() {
        var quadrator = buildQuadrator();

        var u = quadrator.requireByPK(User.class, 1);

        assertEquals(1, u.getId());
        assertEquals("asdasd", u.getName());
    }
}
