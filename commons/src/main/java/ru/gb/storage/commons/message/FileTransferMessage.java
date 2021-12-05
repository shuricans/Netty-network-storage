package ru.gb.storage.commons.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class FileTransferMessage extends Message {
    private byte[] content;
    private Long startPosition;
    private Long fileId;
    private Boolean isDone;
}
