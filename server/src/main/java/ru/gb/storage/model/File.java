package ru.gb.storage.model;

import lombok.*;

import java.nio.file.Path;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class File {
    private Long id;
    private Path path;
    private Long parentId;
    private Long storageId;
}
