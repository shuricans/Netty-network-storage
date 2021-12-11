package ru.gb.storage.client.ui.controller;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Getter
@Setter
public class ScreenController {
    private final Map<String, Pane> screenMap = new HashMap<>();
    private final Scene main;
    private String active;

    public void add(String name, Pane pane) {
        screenMap.put(name, pane);
    }

    public void remove(String name) {
        screenMap.remove(name);
    }

    public void activate(String name) {
        active = name;
        main.setRoot(screenMap.get(name));
    }
}