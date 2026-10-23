package com.github.erosb.quadrator;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.*;


@FunctionalInterface
interface SQLThrowingSupplier<T> {
    T get() throws SQLException;
}

class AliasProvider {

    private final Map<String, Integer> occurrencesOfRelations = new HashMap<>();

    private final Stack<String> contexts = new Stack<>();

    // context -> (relationName -> alias)
    private final Map<String, Map<String, String>> aliasesPerContext = new HashMap<>();

    AliasProvider(TypeMappingConfiguration<?> rootContext) {
        this.contexts.add("");
    }

    String aliasFor(String relationName, String attributeName) {
        return aliasForRelation(relationName) + "__" + attributeName;
    }

    String aliasForRelation(String relationName) {
        var aliasesInCurrentContext = aliasesPerContext.computeIfAbsent(contextAsString(), k -> new HashMap<>(3));
        var knownAlias = aliasesInCurrentContext.get(relationName);
        if (knownAlias != null) {
            return knownAlias;
        }
        var occurrenceCount = occurrencesOfRelations.compute(relationName, (k, v) -> v == null ? 0 : v + 1);
        String alias = occurrenceCount == 0 ? relationName : relationName + "__" + occurrenceCount;
        aliasesInCurrentContext.put(relationName, alias);
        occurrencesOfRelations.put(relationName, occurrenceCount + 1);
        return alias;
    }

    String contextAsString() {
        return contexts.stream().collect(Collectors.joining("."));
    }

    private void enterContext(String context) {
        contexts.push(context);
    }

    private void exitContext() {
        contexts.pop();
    }

    <R> R inContext(String context, SQLThrowingSupplier<R> supplier) {
        enterContext(context);
        try {
            return supplier.get();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            exitContext();
        }
    }
}
