package ru.gb.storage.commons.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class RefreshMessage extends Message {
    private Type type;
    public enum Type {LOCAL, REMOTE};
}
