package com.github.erosb.quadrator;

import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.*;

import java.util.concurrent.*;

import static com.github.erosb.quadrator.DataSources.*;
import static com.github.erosb.quadrator.TypeMappingConfiguration.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class QuadratorTest {

    private Quadrator buildQuadrator() {
        return Quadrator.create(Quadrator.config()
                .dataSource(mysql())
                .typeMapping(trivialMapping(User.class, "id"))
                .build());
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
