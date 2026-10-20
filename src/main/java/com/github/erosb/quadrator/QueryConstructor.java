package com.github.erosb.quadrator;

import lombok.*;

import java.util.*;

import static java.util.stream.Collectors.*;

record SQLQuery() {
}


@RequiredArgsConstructor
class QueryConstructor {

    private final QuadratorConfig config;


    String constructBaseQuery(Class<?> entityClass) {
        var mappingConfig = config.mappingConfigOfType(entityClass);
        var aliasProvider = new AliasProvider(mappingConfig);
        aliasProvider.enterContext(mappingConfig);
        String rootTableName = aliasProvider.aliasForRelation();
        aliasProvider.exitContext();
        String selectClause = "SELECT " + mappingConfig.getAttributeNames().stream()
                .map(attr -> "`" + rootTableName + "`.`" + attr + "` AS `" + aliasProvider.aliasFor(attr) + "`")
                .collect(joining(", "));
        selectClause += mappingConfig.getAssociationMappings().stream().map(m -> {
            var manyTypeMapping = config.mappingConfigOfType(m.getManyType());
            aliasProvider.enterContext(manyTypeMapping);
            String attributes = manyTypeMapping.getAttributeNames().stream()
                    .map(attr -> "`" + manyTypeMapping.getRelationName() + "`.`" + attr
                            + "` AS `" + aliasProvider.aliasFor(attr) + "`")
                    .collect(joining(", ", ", ", ""));
            aliasProvider.exitContext();
            return attributes;
        }).collect(joining(", "));
        String fromClause = "FROM `" + rootTableName + "`";
        List<String> joinClauses = new ArrayList<>();
        for (ToManyMapping<?, ?> m : mappingConfig.getAssociationMappings()) {
            var manyTypeMapping = config.mappingConfigOfType(m.getManyType());
            aliasProvider.enterContext(manyTypeMapping);
            joinClauses.add(" LEFT JOIN `" + manyTypeMapping.getRelationName()
                    + "` ON `" + manyTypeMapping.getRelationName() + "`.`" + m.getForeignKeyAttributeName()
                    + "` = `" + rootTableName + "`.`" + mappingConfig.getPrimaryKeyMapping().getAttributeName() + "`");
            aliasProvider.exitContext();
        }
        for (ToOneMapping<?, ?> m : mappingConfig.getToOneMappings()) {
            var oneTypeMapping = config.mappingConfigOfType(m.getMemberType());
            aliasProvider.enterContext(oneTypeMapping);
            joinClauses.add(" LEFT JOIN `" + oneTypeMapping.getRelationName()
                    + "` ON `" + aliasProvider.aliasFor(oneTypeMapping.getRelationName()) + "`.`" + oneTypeMapping.getPrimaryKeyMapping().getAttributeName()
                    + "` = `" + rootTableName + "`.`" + m.getForeignKeyAttributeName() + "`");
            aliasProvider.exitContext();
        }
        return selectClause + "\n" + fromClause + "\n" + String.join("\n", joinClauses);
    }
}
