package com.github.erosb.quadrator;

import lombok.*;

import java.util.*;

import static java.util.stream.Collectors.*;

@RequiredArgsConstructor
class QueryConstructor {

    private final QuadratorConfig config;

    String constructBaseQuery(Class<?> entityClass) {
        var mappingConfig = config.mappingConfigOfType(entityClass);
        String selectClause = "SELECT " + mappingConfig.getAttributeNames().stream()
                .map(attr -> "`" + mappingConfig.getRelationName() + "`.`" + attr + "`")
                .collect(joining(", "));
        selectClause += mappingConfig.getAssociationMappings().stream().map(m -> {
            var manyTypeMapping = config.mappingConfigOfType(m.getManyType());
            return manyTypeMapping.getAttributeNames().stream()
                    .map(attr -> "`" + manyTypeMapping.getRelationName() + "`.`" + attr + "`")
                    .collect(joining(", ", ", ", ""));
        }).collect(joining(", "));
        String fromClause = "FROM `" + mappingConfig.getRelationName() + "`";
        List<String> joinClauses = new ArrayList<>();
        for (ToManyMapping<?, ?> m : mappingConfig.getAssociationMappings()) {
            var manyTypeMapping = config.mappingConfigOfType(m.getManyType());
            joinClauses.add(" JOIN `" + manyTypeMapping.getRelationName()
                    + "` ON `" + manyTypeMapping.getRelationName() + "`.`" + m.getForeignKeyAttributeName()
                    + "` = `" + mappingConfig.getRelationName() + "`.`" + mappingConfig.getPrimaryKeyMapping().getAttributeName() + "`");
        }

        return selectClause + " " + fromClause + String.join(" ", joinClauses);
    }
}
