package ru.gb.storage.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class User {
    private Long id;
    private String login;
    private String password;
}
