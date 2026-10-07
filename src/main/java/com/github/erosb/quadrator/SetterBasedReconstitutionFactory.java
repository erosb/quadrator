package com.github.erosb.quadrator;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

import static com.github.erosb.quadrator.TypeMappingConfiguration.*;

@RequiredArgsConstructor
@Slf4j
class SetterBasedReconstitutionFactory<T>
        implements ReconstitutionFactory<T> {

    static String toJavaName(String attributeName) {
        StringBuilder rval = new StringBuilder();
        boolean nextUppercase = false;
        for (int i = 0; i < attributeName.length(); i++) {
            char c = attributeName.charAt(i);
            if (c == '_') {
                nextUppercase = true;
            } else {
                if (nextUppercase) {
                    rval.append((c + "").toUpperCase());
                    nextUppercase = false;
                } else rval.append(c);
            }
        }
        return rval.toString();
    }

    private final Class<T> javaType;

    private final List<FieldMapping<T, ?>> fieldMappings;

    @Override
    public T reconstitute(ResultSetAccessor rs)
            throws SQLException {
        try {
            var c = Arrays.stream(javaType.getConstructors())
                    .filter(ctor -> ctor.getParameters().length == 0)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No default constructor found for " + javaType.getName()));
            T instance = (T) c.newInstance();
            for (FieldMapping fieldMapping : fieldMappings) {
                Object fieldValue = rs.getObject(fieldMapping.getAttributeName());
                log.debug("Setting {} to {}", fieldMapping.getAttributeName(), fieldValue);
                setterFor(javaType, toJavaName(fieldMapping.getAttributeName()))
                        .apply(instance, fieldValue);
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
