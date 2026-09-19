package com.github.erosb.quadrator;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private Quadrator buildQuadrator() {
        return Quadrator.create(Quadrator.config()
//                .da
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
