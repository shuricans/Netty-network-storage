package ru.gb.storage.client.netty;

import javafx.application.Platform;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.gb.storage.client.ui.controller.DownloadsController;
import ru.gb.storage.client.ui.controller.ExplorerController;
import ru.gb.storage.client.ui.controller.LoginController;
import ru.gb.storage.client.ui.controller.ScreenController;
import ru.gb.storage.client.utils.ConfigProperties;
import ru.gb.storage.commons.message.Message;
import ru.gb.storage.commons.message.PingMessage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Getter
public class ClientService {

    private final String inetHost;
    private final int port;
    private final ExecutorService executorService;
    private final LoginController loginController;
    private final ExplorerController explorerController;
    private final DownloadsController downloadsController;
    private final ScreenController screenController;

    private Client client;
    private final long sendInterval = Long.parseLong(
            ConfigProperties.getPropertyValue("sendInterval")
                    .orElse("2000")
    );

    private final AtomicInteger counter = new AtomicInteger(0);

    public void connect() {
        client = new Client(
                this,
                port,
                inetHost,
                executorService,
                loginController,
                explorerController,
                downloadsController
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
                explorerController,
                downloadsController
        );
        executorService.execute(client);
    }

    public void sendMessage(Message msg) {
        client.sendMessage(msg);
    }

    protected void connectionResponseEvent(boolean success) {
        Platform.runLater(() -> loginController.connectionRequest(success));
        if (success) {
            counter.set(0);
            executorService.execute(new Sender(this, sendInterval));
        }
    }

    protected void pingResponseEvent() {
        counter.decrementAndGet();
    }

    private void badResponseTime() {
        switch (screenController.getActive()) {
            case "login":
                Platform.runLater(loginController::lostConnection);
                break;
            case "explorer":
                Platform.runLater(explorerController::lostConnection);
        }
    }

    private void sendPingMessage() {
        sendMessage(new PingMessage());
    }

    @AllArgsConstructor
    private static class Sender implements Runnable {

        private final ClientService clientService;
        private final long sendInterval;

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(sendInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (clientService.counter.get() > 2) {
                    clientService.badResponseTime();
                    break;
                }
                clientService.counter.incrementAndGet();
                clientService.sendPingMessage();
            }
        }
    }
}
