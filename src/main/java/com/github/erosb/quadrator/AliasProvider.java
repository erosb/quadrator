package com.github.erosb.quadrator;

import java.util.*;

class AliasProvider {

    private final Stack<TypeMappingConfiguration<?>> contexts = new Stack<>();

    AliasProvider(TypeMappingConfiguration<?> rootContext) {
        this.contexts.add(rootContext);
    }

    String aliasFor(String attributeName) {
        return aliasForRelation() + "__" + attributeName;
    }

    public String aliasForRelation() {
        return contexts.peek().getRelationName();
    }

    public void enterContext(TypeMappingConfiguration<?> manyTypeMapping) {
        contexts.push(manyTypeMapping);
    }

    public void exitContext() {
        contexts.pop();
    }
}
