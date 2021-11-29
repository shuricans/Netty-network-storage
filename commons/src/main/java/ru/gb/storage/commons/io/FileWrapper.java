package ru.gb.storage.commons.io;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FileWrapper {
    private Long id;
    private String name;
    private String path;
    private Long size; //kB
    private boolean isDirectory;
    private Path p;
}
