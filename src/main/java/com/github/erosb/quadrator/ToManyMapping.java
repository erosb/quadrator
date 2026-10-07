package com.github.erosb.quadrator;

import lombok.*;

import java.util.function.*;

@Value
class ToManyMapping<T, F> {
    @NonNull
    String foreignKeyAttributeName;
    @NonNull
    Function<T, F> getter;
}
