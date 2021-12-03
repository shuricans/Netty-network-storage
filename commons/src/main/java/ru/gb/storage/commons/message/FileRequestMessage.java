package ru.gb.storage.commons.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gb.storage.commons.io.File;

@NoArgsConstructor
@Setter
@Getter
public class FileRequestMessage extends Message {
    private File file;
    private FileRequestType type;
    private Long parentDirId;
    private Long storageId;
    private String path;
}

