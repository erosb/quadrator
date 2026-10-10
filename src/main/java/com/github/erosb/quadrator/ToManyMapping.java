package com.github.erosb.quadrator;

import lombok.*;

import java.util.function.*;

@Value
public class ToManyMapping<T, F> {

    public static class ToManyMappingBuilderInitiator<F> {
        Class<F> manyType;

        private ToManyMappingBuilderInitiator(@NonNull Class<F> manyType) {
            this.manyType = manyType;
        }

        public <T> ToManyMappingBuilder<T, F> accessedUsing(Function<T, Iterable<F>> getter, String foreignKeyAttributeName) {
            ToManyMappingBuilder<T, F> builder = new ToManyMappingBuilder<>();
            builder.getter = getter;
            builder.foreignKeyAttributeName = foreignKeyAttributeName;
            builder.manyType = manyType;
            return builder;
        }
    }

    public static class ToManyMappingBuilder<T, F> {
        String foreignKeyAttributeName;
        Function<T, Iterable<F>> getter;
        Class<F> manyType;

        public ToManyMapping<T, F> build() {
            return new ToManyMapping<>(foreignKeyAttributeName, getter, manyType);
        }
    }

    public static <F> ToManyMappingBuilderInitiator<F> hasMany(Class<F> manyType) {
        return new ToManyMapping.ToManyMappingBuilderInitiator<>(manyType);
    }

    @NonNull
    String foreignKeyAttributeName;
    @NonNull
    Function<T, Iterable<F>> getter;
    @NonNull
    Class<F> manyType;
}
