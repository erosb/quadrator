package com.github.erosb.quadrator;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Builder
public class QuadratorConfig {

    public static class QuadratorConfigBuilder {

        private Map<Class<?>, TypeMappingConfiguration> typeMappingConfig = new HashMap<>();

        QuadratorConfigBuilder typeMapping(TypeMappingConfiguration mappingConfig) {
            typeMappingConfig.put(mappingConfig.getType(), mappingConfig);
            return this;
        }

    }

    @Getter
    private final DataSource dataSource;

    private final Map<Class<?>, TypeMappingConfiguration> typeMappingConfig;

    TypeMappingConfiguration mappingConfigOfType(Class<?> type) {
        TypeMappingConfiguration mappingConfig = typeMappingConfig.get(type);
        if (mappingConfig == null) {
            throw new UnknownEntityTypeException(type);
        }
        return mappingConfig;
    }




}
