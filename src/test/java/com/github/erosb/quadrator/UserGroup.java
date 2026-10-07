package com.github.erosb.quadrator;

import lombok.*;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGroup {
    String name;
    List<User> members;
}
