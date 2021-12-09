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
    private Type type;
    private long parentDirId;
    private long storageId;
    private String realPath;
    private String destPath;
    public enum Type {GET, DOWNLOAD, UPLOAD, DELETE}
}

