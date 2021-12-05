package ru.gb.storage.commons.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class SignMessage extends Message {
    private String login;
    private String password;
    private Sign type;
    private boolean success;
    private String info;
    private long storageId;
    private long rootDirId;
}
