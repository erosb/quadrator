package com.github.erosb.quadrator;

import lombok.*;

import javax.sql.*;
import java.util.*;

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
    private final @NonNull DataSource dataSource;

    private final Map<Class<?>, TypeMappingConfiguration> typeMappingConfig;

    TypeMappingConfiguration mappingConfigOfType(Class<?> type) {
        TypeMappingConfiguration mappingConfig = typeMappingConfig.get(type);
        if (mappingConfig == null) {
            throw new UnknownEntityTypeException(type);
        }
        return mappingConfig;
    }


}
