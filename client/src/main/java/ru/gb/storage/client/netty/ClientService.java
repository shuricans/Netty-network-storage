package ru.gb.storage.client.netty;

import javafx.application.Platform;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.gb.storage.client.ui.controller.ExplorerController;
import ru.gb.storage.client.ui.controller.LoginController;
import ru.gb.storage.commons.message.Message;

import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
@Getter
public class ClientService {

    private final String inetHost;
    private final int port;
    private final ExecutorService executorService;
    private final LoginController loginController;
    private final ExplorerController explorerController;

    private Client client;

    public void connect() {
        client = new Client(
                this,
                port,
                inetHost,
                executorService,
                loginController,
                explorerController
        );
        executorService.execute(client);
    }

    public void connect(String inetHost, int port) {
        client = new Client(
                this,
                port,
                inetHost,
                executorService,
                loginController,
                explorerController
        );
        executorService.execute(client);
    }

    public void sendMessage(Message msg) {
        client.sendMessage(msg);
    }

    protected void connection(boolean success) {
        Platform.runLater(() -> {
            loginController.connectionRequest(success);
        });
    }
}
