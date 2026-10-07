package com.github.erosb.quadrator;

import lombok.*;
import lombok.extern.slf4j.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import static com.github.erosb.quadrator.TypeMappingConfiguration.*;

@RequiredArgsConstructor
@Slf4j
public class TypeMappingConfigurationBuilder<T> {

    private final Class<T> type;
    private String relationName;
    private FieldMapping<T, ?> primaryKeyMapping;
    private final List<FieldMapping<T, ?>> fieldMappings = new ArrayList<>();
    private final List<ToManyMapping<T, ?>> associationMappings = new ArrayList<>();
    private ReconstitutionFactory<T> reconstitutionFactory;

    public TypeMappingConfigurationBuilder<T> relationName(String relationName) {
        this.relationName = relationName;
        return this;
    }

    public <PK> TypeMappingConfigurationBuilder<T> primaryKeyMapping(Function<T, PK> getter, String attributeName) {
        return primaryKeyMapping(getter, attributeName, true);
    }

    public <PK> TypeMappingConfigurationBuilder<T> primaryKeyMapping(Function<T, PK> getter, String attributeName, boolean dbGenerated) {
        this.primaryKeyMapping = new FieldMapping<>(attributeName, getter, dbGenerated);
        return this;
    }

    public <F> TypeMappingConfigurationBuilder<T> fieldMapping(Function<T, F> getter, String attributeName) {
        this.fieldMappings.add(new FieldMapping<>(attributeName, getter, false));
        return this;
    }

    public TypeMappingConfigurationBuilder<T> reconstitutionFactory(ReconstitutionFactory<T> reconstitutionFactory) {
        this.reconstitutionFactory = reconstitutionFactory;
        return this;
    }

    public TypeMappingConfiguration build() {
        if (reconstitutionFactory == null) {
            reconstitutionFactory = new SetterBasedReconstitutionFactory<>(type, concatFieldMappings(primaryKeyMapping, fieldMappings));
        }
        log.debug("fieldMappings = {}", fieldMappings);
        return new DefaultTypeMappingConfiguration<T>(relationName, type, fieldMappings, primaryKeyMapping,
                reconstitutionFactory);
    }

    public <F> TypeMappingConfigurationBuilder<T> associationMapping(Function<T, F> getter, String name) {
        boolean isToMany = singleAbstractMethod(getter).getReturnType().isAssignableFrom(Iterable.class);
        if (!isToMany) {
            throw new IllegalArgumentException("Association mapping is not to-many (not supported)");
        }
        var mapping = new ToManyMapping<>(this.relationName + "_" + primaryKeyMapping.getAttributeName(), getter);
        this.associationMappings.add(mapping);
        return this;
    }

    private static <T, F> Method singleAbstractMethod(Function<T, F> members) {
        List<Method> abstractMethods = Arrays.stream(members.getClass().getInterfaces())
                .flatMap(iface -> Arrays.stream(iface.getMethods()))
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .distinct()
                .toList();
        if (abstractMethods.size() != 1) {
            throw new IllegalStateException("Expected exactly one abstract method, found " + abstractMethods.size());
        }
        return abstractMethods.getFirst();
    }
}
