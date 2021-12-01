package ru.gb.storage.commons.io;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class File {
    private Long id;
    private String name;
    private String path;
    private Long size;
    private Boolean isDirectory;
    private Long parentId;
    private Long storageId;
}
