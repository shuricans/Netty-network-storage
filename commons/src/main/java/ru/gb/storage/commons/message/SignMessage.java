package ru.gb.storage.commons.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignMessage extends Message {
    private String login;
    private String password;
    private Sign type;
    private boolean success;
}
