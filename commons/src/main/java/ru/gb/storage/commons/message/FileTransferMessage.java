package ru.gb.storage.commons.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class FileTransferMessage extends Message {
    private long fileId;
    private byte[] content;
    private long startPosition;
    private int progress;
    private boolean isDone;
    private String destPath;
    private String realPath;
}
