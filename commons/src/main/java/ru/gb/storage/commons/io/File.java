package ru.gb.storage.commons.io;

import lombok.*;

import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class File {
    private Long id;
    private String name;
    private String path;
    private Long size;
    private Boolean isDirectory;
    private Boolean isReady;
    private Long parentId;
    private Long storageId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        File file = (File) o;
        return Objects.equals(name, file.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
