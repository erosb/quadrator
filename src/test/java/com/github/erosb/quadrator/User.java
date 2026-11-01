package com.github.erosb.quadrator;

import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class User {
    private Integer id;
    private String name;

    public User(Integer id, String name, List<AccessToken> accessTokens) {
        this.id = id;
        this.name = name;
    }
//    private List<AccessToken> accessTokens;
}
