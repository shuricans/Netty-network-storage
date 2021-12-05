package ru.gb.storage.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Storage {
    private Long id;
    private Long capacity;
    private Long userId;
}
