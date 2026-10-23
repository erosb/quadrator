package com.github.erosb.quadrator;

import lombok.*;
import lombok.extern.slf4j.*;

import java.sql.*;
import java.util.*;

import static java.util.stream.Collectors.*;

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
@Slf4j
class DefaultQuadrator
                implements Quadrator {

    private final QuadratorConfig config;

    @Override
    public <E> E requireByPK(Class<E> clazz, Object primaryKey) {
        try {
            var conn = getConnection();
            TypeMappingConfiguration typeMappingConfiguration = config.mappingConfigOfType(clazz);
            String sql = new QueryConstructor(config).constructBaseQuery(clazz)
                            + " WHERE "
                            + typeMappingConfiguration.getRelationName() + "."
                            + typeMappingConfiguration.getPrimaryKeyMapping().getAttributeName() + " = ?";
            log.debug("SQL: {}", sql);
            var stmt = conn.prepareStatement(sql);
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
        TypeMappingConfiguration<E> mappingConfig = config.mappingConfigOfType((Class<E>) entity.getClass());
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

                if (!generated.next())
                    throw new IllegalStateException();
                attributesForReconst.put(primaryKeyAttr, generated.getObject(1));

                return mappingConfig.getReconstitutionFactory()
                                .reconstitute(ResultSetAccessor.of(attributesForReconst, Map.of()));

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            var allFieldMappings = mappingConfig.allFieldMappings();
            var sql = "INSERT INTO `" + mappingConfig.getRelationName() + "` (" +
                            allFieldMappings.stream()
                                            .map(FieldMapping::getAttributeName)
                                            .collect(joining(", ")) + ") VALUES (" +

                            String.join(",", Collections.nCopies(allFieldMappings.size(), "?")) + ")";
            log.debug("SQL: {}", sql);
            try {
                var stmt = getConnection().prepareStatement(sql);
                for (int i = 0; i < allFieldMappings.size(); i++) {
                    Object value = allFieldMappings.get(i).getGetter().apply(entity);
                    stmt.setObject(i + 1, value == null ? null : value.toString());
                }
                stmt.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return entity;
    }

    private <E> E instantiate(Class<E> clazz, ResultSet rs) {
        var mappingConfig = config.mappingConfigOfType(clazz);
        var aliasProvider = new AliasProvider(mappingConfig);
        try {

            Map<String, List<Object>> manys = new HashMap<>();
            E firstEntity = null;
            do {
                for (var assocMapping : mappingConfig.getAssociationMappings()) {
                    var manyType = assocMapping.getManyType();
                    List<Object> entities = new ArrayList<>();
                    TypeMappingConfiguration<?> manyTypeConfig = config.mappingConfigOfType(manyType);
                    var assocEntity = aliasProvider.inContext(assocMapping.getMemberName(), () -> {
                        if (entityExistsInRow(rs, aliasProvider, manyTypeConfig)) {
                            var resultSetAccessor = ResultSetAccessor.of(rs, manys,
                                            manyTypeConfig.getRelationName(),
                                            aliasProvider);
                            var associatedEntity = manyTypeConfig.getReconstitutionFactory()
                                            .reconstitute(resultSetAccessor);
                            return associatedEntity;
                        }
                        return null;
                    });
                    if (assocEntity != null) {
                        entities.add(assocEntity);
                        manys.computeIfAbsent(assocMapping.getMemberName(), k -> new ArrayList<>())
                                        .add(assocEntity);
                    }
                }
                if (rs.isLast()) {
                    for (var toOneMapping : mappingConfig.getToOneMappings()) {
                        var associatedType = toOneMapping.getMemberType();
                        var associatedTypeConfig = config.mappingConfigOfType(associatedType);
                        var resultSetAccessor = ResultSetAccessor.of(rs, Map.of(),
                                        associatedTypeConfig.getRelationName(), aliasProvider);
                        var assocObj = associatedTypeConfig.getReconstitutionFactory().reconstitute(resultSetAccessor);
                        manys.put(toOneMapping.getMemberName(), List.of(assocObj));
                    }
                    var resultSetAccessor = ResultSetAccessor.of(rs, manys, mappingConfig.getRelationName(),
                                    aliasProvider);
                    firstEntity = mappingConfig.getReconstitutionFactory().reconstitute(resultSetAccessor);
                }
            } while (rs.next());
            return firstEntity;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean entityExistsInRow(ResultSet rs, AliasProvider aliasProvider,
                    TypeMappingConfiguration<?> manyTypeConfig) throws SQLException {
        var pkMapping = manyTypeConfig.getPrimaryKeyMapping();
        var columnName = aliasProvider.aliasFor(manyTypeConfig.getRelationName(), pkMapping.getAttributeName());
        return rs.getObject(columnName) != null;
    }

}
