package com.github.erosb.quadrator;

import lombok.RequiredArgsConstructor;

import java.sql.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public interface Quadrator {

    static Quadrator create(QuadratorConfig config) {
        return new DefaultQuadrator(config);
    }

    static QuadratorConfig.QuadratorConfigBuilder config() {
        return QuadratorConfig.builder();
    }

    <E> E requireByPK(Class<E> clazz, Object primaryKey);

    <T> T save(T entity);
}

@RequiredArgsConstructor
class DefaultQuadrator
        implements Quadrator {

    private final QuadratorConfig config;

    @Override
    public <E> E requireByPK(Class<E> clazz, Object primaryKey) {
        try {
            var conn = getConnection();
            TypeMappingConfiguration typeMappingConfiguration = config.mappingConfigOfType(clazz);
            var stmt = conn.prepareStatement(baseQuery(clazz) + " WHERE " + typeMappingConfiguration.getPrimaryKeyMapping().getAttributeName() + " = ?");
            stmt.setString(1, primaryKey.toString());
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return instantiate(clazz, rs);
            }
            throw new EntityNotFoundException(clazz.getSimpleName() + " not found by primary key " + primaryKey);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConnection()
            throws SQLException {
        return config.getDataSource().getConnection(); //DriverManager.getConnection(config.getConnection());
    }

    @Override
    public <E> E save(E entity) {
        TypeMappingConfiguration<E> mappingConfig = config.mappingConfigOfType(entity.getClass());
        List<FieldMapping<E, ?>> fieldMappings = mappingConfig.getFieldMappings();
        if (mappingConfig.getPrimaryKeyMapping().isDbGenerated()) {
            String primaryKeyAttr = mappingConfig.getPrimaryKeyMapping().getAttributeName();
            List<FieldMapping<E, ?>> insertedFields = fieldMappings.stream()
                    .filter(fm -> !fm.getAttributeName().equals(primaryKeyAttr))
                    .toList();
            var sql = "INSERT INTO `" + mappingConfig.getRelationName() + "` (" +
                    insertedFields.stream()
                            .map(FieldMapping::getAttributeName)
                            .collect(joining(", ")) + ") VALUES (" +

                    String.join(",", Collections.nCopies(insertedFields.size(), "?")) + ")";
            System.out.println(sql);
            try {
                var stmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                Map<String, Object> attributesForReconst = new HashMap<>();
                for (int i = 0; i < insertedFields.size(); i++) {
                    FieldMapping<E, ?> insertedFieldMapping = insertedFields.get(i);
                    Object fieldValue = insertedFieldMapping.getGetter().apply(entity);
                    stmt.setObject(i + 1, fieldValue);
                    attributesForReconst.put(insertedFieldMapping.getAttributeName(), fieldValue);
                }
                stmt.executeUpdate();
                ResultSet generated = stmt.getGeneratedKeys();

                if (!generated.next()) throw new IllegalStateException();
                attributesForReconst.put(primaryKeyAttr, generated.getObject(1));

                return mappingConfig.getReconstitutionFactory().reconstitute(new MapBackedResultSet(attributesForReconst));

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            var sql = "INSERT INTO `" + mappingConfig.getRelationName() + "` (" +
                    fieldMappings.stream()
                            .map(f -> (FieldMapping<E, ?>) f)
                            .map(FieldMapping::getAttributeName)
                            .collect(joining(", ")) + ") VALUES (" +

                    String.join(",", Collections.nCopies(fieldMappings.size(), "?")) + ")";

            try {
                var stmt = getConnection().prepareStatement(sql);
                for (int i = 0; i < fieldMappings.size(); i++) {
                    stmt.setObject(i + 1, fieldMappings.get(i).getGetter().apply(entity));
                }
                stmt.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return entity;
    }

    private <E> String baseQuery(Class<E> clazz) {
        var mappingConfig = config.mappingConfigOfType(clazz);
        String selectClause = "SELECT " + mappingConfig.getAttributeNames().stream().collect(joining(", "));
        String fromClause = "FROM `" + mappingConfig.getRelationName() + "`";
        return selectClause + " " + fromClause;
    }

    private <E> E instantiate(Class<E> clazz, ResultSet rs) {
        var constr = clazz.getConstructors()[0];
        constr.setAccessible(true);
        var mappingConfig = config.mappingConfigOfType(clazz);
        try {
            return (E) mappingConfig.getReconstitutionFactory().reconstitute(rs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
