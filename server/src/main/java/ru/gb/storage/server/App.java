package ru.gb.storage.server;

import ru.gb.storage.service.ConfigProperties;

public final class App {

    public static void main(String[] args) {
        final String portString = ConfigProperties.getPropertyValue("port").orElse("8080");
        try {
            int port = Integer.parseInt(portString);
            new Server(port).start();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
}
