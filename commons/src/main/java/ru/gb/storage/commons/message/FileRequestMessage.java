package ru.gb.storage.commons.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class FileRequestMessage extends Message {
    private FileRequestType type;
    private Long fileId;
    private Long storageId;
}

