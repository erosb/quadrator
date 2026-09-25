package com.github.erosb.quadrator;

import lombok.*;
import org.junit.jupiter.api.*;

import javax.sql.*;
import java.sql.*;

import static com.github.erosb.quadrator.DataSources.*;
import static org.junit.jupiter.api.Assertions.*;

public class ReconstitutionFactoryTest {

    @SneakyThrows
    private Quadrator buildQuadrator() {
        DataSource ds = mem(false);
        try (Statement st = ds.getConnection().createStatement()) {
            st.execute("drop table if exists `user`");
            st.execute("create table users (id int primary key auto_increment, user_name text)");
            st.executeUpdate("insert into users (user_name) values ('asdasd'), ('bsdbsd')");
        }
        return Quadrator.create(Quadrator.config()
                .dataSource(ds)
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
