package com.github.erosb.quadrator;

import org.junit.jupiter.api.*;

import static com.github.erosb.quadrator.DataSources.*;
import static com.github.erosb.quadrator.TypeMappingConfiguration.*;
import static org.junit.jupiter.api.Assertions.*;

public class OneToManyTest {


    private Quadrator buildQuadrator() {
        return Quadrator.create(Quadrator.config()
                .dataSource(mysql(true))
                .typeMapping(trivialMapping(User.class, "id"))
                .typeMapping(TypeMappingConfiguration.builderFor(UserGroup.class)
                        .relationName("user_groups")
                        .primaryKeyMapping(UserGroup::name, "name", false)
                        .fieldMapping(UserGroup::members, "members")
                        .build()
                ).build()
        );
    }

    @Test
    void testQuery() {
        Quadrator quadrator = buildQuadrator();

        var actual = quadrator.requireByPK(UserGroup.class, "group1");

        var expected = new UserGroup("group1", java.util.List.of(
                new User(2, "Bob"),
                new User(3, "Charlie")
        ));

        assertEquals(expected, actual);
    }


}
