package com.github.erosb.quadrator;

import java.util.*;

public record UserGroup(
        String name,
        List<User> members
) {
}
