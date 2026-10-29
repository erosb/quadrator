package com.github.erosb.quadrator;

import lombok.*;

import java.util.*;

import static java.util.stream.Collectors.*;

sealed interface Expr permits AliasedExpr, QualifiedIdentifier, Identifier {

}

record Identifier(String ident) implements Expr {
}

record AliasedExpr(Expr expr, String alias) implements Expr {
}

record QualifiedIdentifier(Identifier relation, Identifier attribute) implements Expr {
}

record SQLSelect(List<Expr> expressions, Expr from) {
}

class SQLSerializer {
    private final StringBuilder sb = new StringBuilder();

    String serialize(SQLSelect sql) {
        sb.append("SELECT ");
        sb.append(sql.expressions().stream().map(this::serialize).collect(joining(", ")));
        sb.append(" FROM ").append(serialize(sql.from()));
        return sb.toString();
    }

    private String serialize(Expr expr) {
        return switch (expr) {
            case AliasedExpr i -> serialize(i.expr()) + " AS " + esc(i.alias());
            case QualifiedIdentifier qualifiedIdentifier -> throw new UnsupportedOperationException();
            case Identifier identifier -> throw new UnsupportedOperationException();
        };
    }

    private String esc(String ident) {
        return "`" + ident + "`";
    }
}

@RequiredArgsConstructor
class QueryConstructor {

    private final QuadratorConfig config;

    String constructBaseQuery(Class<?> entityClass) {
        var mappingConfig = config.mappingConfigOfType(entityClass);
        var aliasProvider = new AliasProvider(mappingConfig);
        String rootTableName = aliasProvider.aliasForRelation(mappingConfig.getRelationName());
        List<Expr> selectExprs = new ArrayList<>();
        mappingConfig.getAttributeNames().stream()
                .map(attr -> new AliasedExpr(
                        new QualifiedIdentifier(new Identifier(rootTableName), new Identifier(attr)),
                        aliasProvider.aliasFor(rootTableName, attr))
                )
                .forEach(selectExprs::add);
        Expr from = new Identifier(rootTableName);
        var sqlSelect = new SQLSelect(selectExprs, from);
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
        return new SQLSerializer().serialize(sqlSelect) + "\n" + String.join("\n", joinClauses);
    }
}
