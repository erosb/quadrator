package com.github.erosb.quadrator;

import lombok.*;

import java.util.*;

import static java.util.stream.Collectors.*;

@RequiredArgsConstructor
class QueryConstructor {

    private final QuadratorConfig config;

    String constructBaseQuery(Class<?> entityClass) {
        var mappingConfig = config.mappingConfigOfType(entityClass);
        var aliasProvider = new AliasProvider(mappingConfig);
        String rootTableName = aliasProvider.aliasForRelation(mappingConfig.getRelationName());
        String selectClause = "SELECT " + mappingConfig.getAttributeNames().stream()
                        .map(attr -> "`" + rootTableName + "`.`" + attr + "` AS `" + aliasProvider.aliasFor(
                                        mappingConfig.getRelationName(), attr) + "`")
                        .collect(joining(", "));
        selectClause += mappingConfig.getAssociationMappings().stream().map(m -> {
            var manyTypeMapping = config.mappingConfigOfType(m.getManyType());
            return aliasProvider.inContext(m.getMemberName(), () -> manyTypeMapping.getAttributeNames().stream()
                            .map(attr -> "`" + manyTypeMapping.getRelationName() + "`.`" + attr
                                            + "` AS `" + aliasProvider.aliasFor(manyTypeMapping.getRelationName(),
                                            attr) + "`")
                            .collect(joining(", ", ", ", "")));
        }).collect(joining(", "));

        selectClause += mappingConfig.getToOneMappings().stream().map(m -> {
            var assocTypeMapping = config.mappingConfigOfType(m.getMemberType());
            return aliasProvider.inContext(m.getMemberName(), () ->
                            assocTypeMapping.getAttributeNames().stream()
                                            .map(attr -> "`" + aliasProvider.aliasForRelation(
                                                            assocTypeMapping.getRelationName()) + "`.`" + attr
                                                            + "` AS `" + aliasProvider.aliasFor(
                                                            assocTypeMapping.getRelationName(),
                                                            attr) + "`")
                                            .collect(joining(", ", ", ", ""))
            );
        }).collect(joining(", "));

        String fromClause = "FROM `" + rootTableName + "`";
        List<String> joinClauses = new ArrayList<>();
        for (ToManyMapping<?, ?> m : mappingConfig.getAssociationMappings()) {
            var manyTypeMapping = config.mappingConfigOfType(m.getManyType());
            joinClauses.add(aliasProvider.inContext(m.getMemberName(), () ->
                            " LEFT JOIN `" + manyTypeMapping.getRelationName() + "` AS `"
                            + aliasProvider.aliasForRelation(manyTypeMapping.getRelationName())
                            + "` ON `" + manyTypeMapping.getRelationName() + "`.`" + m.getForeignKeyAttributeName()
                            + "` = `" + rootTableName + "`.`" + mappingConfig.getPrimaryKeyMapping().getAttributeName()
                            + "`"
            ));
        }
        for (ToOneMapping<?, ?> m : mappingConfig.getToOneMappings()) {
            var oneTypeMapping = config.mappingConfigOfType(m.getMemberType());
            joinClauses.add(aliasProvider.inContext(m.getMemberName(), () ->
                            " LEFT JOIN `" + oneTypeMapping.getRelationName() + "` AS `"
                            + aliasProvider.aliasForRelation(oneTypeMapping.getRelationName())
                            + "` ON `" + aliasProvider.aliasForRelation(oneTypeMapping.getRelationName()) + "`.`"
                            + oneTypeMapping.getPrimaryKeyMapping().getAttributeName()
                            + "` = `" + rootTableName + "`.`" + m.getForeignKeyAttributeName() + "`"
            ));
        }
        return selectClause + "\n" + fromClause + "\n" + String.join("\n", joinClauses);
    }
}
