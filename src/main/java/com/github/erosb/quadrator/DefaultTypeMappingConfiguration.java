package com.github.erosb.quadrator;

import lombok.*;

import java.util.*;

@RequiredArgsConstructor
class DefaultTypeMappingConfiguration<T>
        implements TypeMappingConfiguration<T> {

    private final String relationName;
    private final Class<T> javaType;
    private final List<FieldMapping<T, ?>> fieldMappings;
    private final List<ToManyMapping<T, ?>> toManyMappings;
    @Getter
    private final List<ToOneMapping<T, ?>> toOneMappings;
    private final FieldMapping<T, ?> primaryKeyMapping;
    private final ReconstitutionFactory<T> reconstitutionFactory;

    @Override
    public String getRelationName() {
        return relationName;
    }

    @Override
    public ReconstitutionFactory<T> getReconstitutionFactory() {
        return reconstitutionFactory;
    }

    @Override
    public Class<T> getType() {
        return javaType;
    }

    @Override
    public List<String> getAttributeNames() {
        ArrayList<String> attrNames = new ArrayList<>();
        attrNames.add(primaryKeyMapping.getAttributeName());
        attrNames.addAll(fieldMappings.stream().map(FieldMapping::getAttributeName).toList());
        return attrNames;
    }

    @Override
    public FieldMapping<T, ?> getPrimaryKeyMapping() {
        return primaryKeyMapping;
    }

    @Override
    public List<FieldMapping<T, ?>> getFieldMappings() {
        return fieldMappings;
    }

    @Override
    public List<ToManyMapping<T, ?>> getAssociationMappings() {
        return toManyMappings;
    }
}
