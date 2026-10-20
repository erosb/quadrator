package com.github.erosb.quadrator;

import java.util.*;

class AliasProvider {

    private final Map<String, Integer> occurrencesOfRelations = new HashMap<>();

    private final Stack<TypeMappingConfiguration<?>> contexts = new Stack<>();

    AliasProvider(TypeMappingConfiguration<?> rootContext) {
        this.contexts.add(rootContext);
    }

    String aliasFor(String attributeName) {
        return aliasForRelation() + "__" + attributeName;
    }

    public String aliasForRelation() {
        String relationName = contexts.peek().getRelationName();
        return relationName + "_" + occurrencesOfRelations.get(relationName);
    }

    public void enterContext(TypeMappingConfiguration<?> manyTypeMapping) {
        occurrencesOfRelations.compute(manyTypeMapping.getRelationName(), (k, v) -> v == null ? 0 : v + 1);
        contexts.push(manyTypeMapping);
    }

    public void exitContext() {
        contexts.pop();
    }
}
