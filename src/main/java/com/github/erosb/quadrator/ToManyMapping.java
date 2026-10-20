package com.github.erosb.quadrator;

import lombok.*;

import java.util.function.*;

@Value
public class ToManyMapping<T, F> {

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ToManyMappingBuilderInitiator<F> {
        final @NonNull Class<F> manyType;
        String memberName;

        public <T> ToManyMappingBuilder<T, F> accessedUsing(@NonNull Function<T, Iterable<F>> getter, @NonNull String foreignKeyAttributeName) {
            ToManyMappingBuilder<T, F> builder = new ToManyMappingBuilder<>();
            builder.getter = getter;
            builder.foreignKeyAttributeName = foreignKeyAttributeName;
            builder.manyType = manyType;
            builder.memberName = memberName;
            return builder;
        }

        public ToManyMappingBuilderInitiator<F> called(@NonNull String memberName) {
            this.memberName = memberName;
            return this;
        }
    }

    public static class ToManyMappingBuilder<T, F> {
        String foreignKeyAttributeName;
        Function<T, Iterable<F>> getter;
        Class<F> manyType;
        String memberName;

        public ToManyMapping<T, F> build() {
            return new ToManyMapping<>(foreignKeyAttributeName, getter, manyType, memberName);
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
    @NonNull
    String memberName;
}
