package com.github.erosb.quadrator;

import lombok.*;
import org.junit.jupiter.api.*;

import static com.github.erosb.quadrator.DataSources.*;
import static org.junit.jupiter.api.Assertions.*;

public class ReconstitutionFactoryTest {

    @SneakyThrows
    private Quadrator buildQuadrator() {
        return Quadrator.create(Quadrator.config()
                .dataSource(mem())
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
