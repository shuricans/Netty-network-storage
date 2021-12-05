package ru.gb.storage.commons.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gb.storage.commons.io.File;

import java.util.List;

@NoArgsConstructor
@Setter
@Getter
public class ListFilesMessage extends Message {
    private List<File> files;
}
