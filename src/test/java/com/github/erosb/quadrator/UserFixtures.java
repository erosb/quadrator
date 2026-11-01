package com.github.erosb.quadrator;

import java.util.*;

public class UserFixtures {

    public static User alice() {
        return new User(1, "Alice", List.of());
    }

    public static User bob() {
        return new User(2, "Bob", List.of());
    }

    public static User charlie() {
        return new User(3, "Charlie", List.of());
    }

    public static User daniel() {
        return new User(4, "Daniel", List.of());
    }

    // he is not in the DB
    public static User ethan() {
        return new User(5, "Ethan", List.of());
    }

}
