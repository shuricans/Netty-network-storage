package ru.gb.storage.commons.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gb.storage.commons.io.File;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class NestedFilesRequestMessage extends Message {
    private List<File> files;
    private String path;
}
