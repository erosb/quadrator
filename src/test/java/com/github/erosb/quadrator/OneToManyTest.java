package com.github.erosb.quadrator;

import org.junit.jupiter.api.*;

import java.util.*;

import static com.github.erosb.quadrator.DataSources.*;
import static com.github.erosb.quadrator.ToManyMapping.*;
import static com.github.erosb.quadrator.ToOneMapping.*;
import static com.github.erosb.quadrator.TypeMappingConfiguration.*;
import static org.junit.jupiter.api.Assertions.*;

public class OneToManyTest {

    private Quadrator buildQuadrator() {
        return Quadrator.create(Quadrator.config()
                .dataSource(mem(true))
                .typeMapping(trivialMapping(User.class, "id"))
                .typeMapping(TypeMappingConfiguration.builderFor(UserGroup.class)
                        .relationName("user_groups")
                        .primaryKeyMapping(UserGroup::getName, "name", false)
                        .associationMapping(hasMany(User.class)
                                .called("members")
                                .accessedUsing(UserGroup::getMembers, "group_name")
                                .build())
                        .associationMapping(hasOne(User.class)
                                .called("creator")
                                .accessedUsing(UserGroup::getCreator, "creator_id")
                                .build()
                        )
                        .build()
                ).build()
        );
    }

    @Test
    void testQueryOneGroup() {
        Quadrator quadrator = buildQuadrator();

        var actual = quadrator.requireByPK(UserGroup.class, "group1");

        var expected = new UserGroup("group1", new User(2, "Bob"), List.of(
                new User(2, "Bob"),
                new User(3, "Charlie")
        ));

        assertEquals(expected, actual);
    }

    @Test
    void zeroOnManySide() {
        Quadrator quadrator = buildQuadrator();

        var actual = quadrator.requireByPK(UserGroup.class, "empty_group");

        var expected = new UserGroup("empty_group", new User(3, "Charlie"), null);

        assertEquals(expected, actual);
    }

    @Test
    @Disabled
    void queryManyGroups() {

    }

    @Test
    @Disabled
    void persistUserGroup() {

    }

    @Test
    @Disabled
    void multipleOccurrencesOfRelation() {

    }

}
