package com.github.erosb.quadrator;

import lombok.*;

import java.util.*;

import static java.util.stream.Collectors.*;

sealed interface Expr permits AliasedExpr, BinaryExpr, Identifier, QualifiedIdentifier {
    default AliasedExpr as(String alias) {
        return new AliasedExpr(this, alias);
    }
}

record BinaryExpr(Expr left, String operator, Expr right) implements Expr {
}

record Identifier(String ident) implements Expr {
    QualifiedIdentifier attr(String attr) {
        return new QualifiedIdentifier(this, new Identifier(attr));
    }

    AliasedExpr aliased(AliasProvider aliasProvider) {
        return new AliasedExpr(this, aliasProvider.aliasForRelation(ident));
    }
}

record AliasedExpr(Expr expr, String alias) implements Expr {
}

record QualifiedIdentifier(Identifier relation, Identifier attribute) implements Expr {
    AliasedExpr aliased(AliasProvider aliasProvider) {
        return new AliasedExpr(new Identifier(aliasProvider.aliasForRelation(relation.ident())),
                        aliasProvider.aliasFor(relation.ident(), attribute.ident()));
    }
}

record JoinClause(Expr relation, Expr condition) {
}

record SQLSelect(List<Expr> expressions, Expr from, List<JoinClause> joinConditions) {
}

class SQLSerializer {
    private final StringBuilder sb = new StringBuilder();

    String serialize(SQLSelect sql) {
        sb.append("SELECT ");
        sb.append(sql.expressions().stream().map(this::serialize).collect(joining(",\n\t\t")));
        sb.append("\n\tFROM ").append(serialize(sql.from()));
        sb.append(sql.joinConditions().stream()
                        .map(join -> "\n\tLEFT JOIN " + serialize(join.relation()) + " ON " + serialize(
                                        join.condition()))
                        .collect(joining()));
        return sb.toString();
    }

    private String serialize(Expr expr) {
        return switch (expr) {
            case AliasedExpr i -> serialize(i.expr()) + " AS " + esc(i.alias());
            case QualifiedIdentifier ident -> serialize(ident.relation()) + "." + serialize(ident.attribute());
            case Identifier identifier -> esc(identifier.ident());
            case BinaryExpr bin -> serialize(bin.left()) + " " + bin.operator() + " " + serialize(bin.right());
        };
    }

    private String esc(String ident) {
        return "`" + ident + "`";
    }
}

@RequiredArgsConstructor
class QueryConstructor {

    private static Identifier ident(String ident) {
        return new Identifier(ident);
    }

    private final QuadratorConfig config;

    String constructBaseQuery(Class<?> entityClass) {
        var mappingConfig = config.mappingConfigOfType(entityClass);
        var aliasProvider = new AliasProvider(mappingConfig);
        String rootTableName = aliasProvider.aliasForRelation(mappingConfig.getRelationName());
        List<Expr> selectExprs = new ArrayList<>();
        List<JoinClause> joins = new ArrayList<>();
        mappingConfig.getAttributeNames().stream()
                        .map(attr ->
                                        ident(rootTableName).attr(attr).as(aliasProvider.aliasFor(rootTableName, attr))
                        )
                        .forEach(selectExprs::add);
        Expr from = ident(rootTableName);

        for (var m : mappingConfig.getAssociationMappings()) {
            var manyTypeMapping = config.mappingConfigOfType(m.getManyType());
            selectExprs.addAll(aliasProvider.inContext(m.getMemberName(), () ->
                            manyTypeMapping.getAttributeNames().stream().map(attr ->
                                ident(aliasProvider.aliasForRelation(manyTypeMapping.getRelationName())).attr(attr)
                                            .as(aliasProvider.aliasFor(manyTypeMapping.getRelationName(), attr))
            ).toList()));
            String pkAttr = mappingConfig.getPrimaryKeyMapping().getAttributeName();
            joins.add(aliasProvider.inContext(m.getMemberName(), () ->
                            new JoinClause(ident(manyTypeMapping.getRelationName()).aliased(aliasProvider), // as(
//                                            aliasProvider.aliasForRelation(manyTypeMapping.getRelationName()))
                                            new BinaryExpr(
                                                            ident(aliasProvider.aliasForRelation(
                                                                            manyTypeMapping.getRelationName())
                                                            ).attr(m.getForeignKeyAttributeName()),
                                                            "=",
                                                            ident(rootTableName).attr(pkAttr)
                                            )
                            )));
        }

        for (var m : mappingConfig.getToOneMappings()) {
            selectExprs.addAll(aliasProvider.inContext(m.getMemberName(), () -> {
                var assocTypeMapping = config.mappingConfigOfType(m.getMemberType());
                return assocTypeMapping.getAttributeNames().stream().map(attr ->
                                ident(aliasProvider.aliasForRelation(assocTypeMapping.getRelationName())).attr(attr)
                                                .as(aliasProvider.aliasFor(assocTypeMapping.getRelationName(), attr))
                ).toList();
            }));

            var oneTypeMapping = config.mappingConfigOfType(m.getMemberType());
            joins.add(aliasProvider.inContext(m.getMemberName(), () -> {
                String joinedAlias = aliasProvider.aliasForRelation(oneTypeMapping.getRelationName());
                String foreignKeyAttr = oneTypeMapping.getPrimaryKeyMapping().getAttributeName();
                return new JoinClause(
                                ident(oneTypeMapping.getRelationName()).as(joinedAlias),
                                new BinaryExpr(
                                                ident(joinedAlias).attr(foreignKeyAttr),
                                                "=",
                                                ident(rootTableName).attr(m.getForeignKeyAttributeName())

                                )
                );
            }));
        }
        var sqlSelect = new SQLSelect(selectExprs, from, joins);
        return new SQLSerializer().serialize(sqlSelect);
    }
}
