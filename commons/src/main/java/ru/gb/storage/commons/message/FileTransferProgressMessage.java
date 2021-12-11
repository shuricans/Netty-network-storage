package ru.gb.storage.commons.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FileTransferProgressMessage extends Message {
    private long fileId;
    private int progress;
    private String destPath;
    private boolean isDone;
}
