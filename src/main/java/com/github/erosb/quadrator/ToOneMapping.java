package com.github.erosb.quadrator;

import lombok.*;

import java.util.function.*;

@Value
public class ToOneMapping<T, F> {

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ToOneMappingBuilderInitiator<F> {
        final @NonNull Class<F> memberType;
        String memberName;


        public <T> ToOneMappingBuilder<T, F> accessedUsing(@NonNull Function<T, F> getter, @NonNull String foreignKeyAttributeName) {
            return new ToOneMappingBuilder<>(
                    memberType,
                    memberName,
                    foreignKeyAttributeName,
                    getter
            );
        }

        public ToOneMappingBuilderInitiator<F> called(@NonNull String memberName) {
            this.memberName = memberName;
            return this;
        }

    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ToOneMappingBuilder<T, F> {
        final @NonNull Class<F> memberType;
        final @NonNull String memberName;
        final @NonNull String foreignKeyAttributeName;
        final @NonNull Function<T, F> getter;

        public ToOneMapping<T, F> build() {
            return new ToOneMapping<>(
                    memberType,
                    memberName,
                    foreignKeyAttributeName,
                    getter
            );
        }
    }


    public static <F> ToOneMappingBuilderInitiator<F> hasOne(Class<F> manyType) {
        return new ToOneMappingBuilderInitiator<>(manyType);
    }

    final @NonNull Class<F> memberType;
    final @NonNull String memberName;
    final @NonNull String foreignKeyAttributeName;
    final @NonNull Function<T, F> getter;

}
