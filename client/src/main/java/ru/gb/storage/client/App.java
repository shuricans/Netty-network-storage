package ru.gb.storage.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import ru.gb.storage.client.netty.Client;
import ru.gb.storage.client.ui.controller.ExplorerController;
import ru.gb.storage.client.ui.controller.LoginController;
import ru.gb.storage.client.ui.controller.ScreenController;
import ru.gb.storage.commons.message.Message;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class App extends Application {

    private ExecutorService executorService;
    private Client client;
    private ScreenController screenController;
    private LoginController loginController;
    private ExplorerController explorerController;
    private final LinkedBlockingQueue<Message> messagesQueue = new LinkedBlockingQueue<>();

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
        Object object = loader.load();
        Scene scene = new Scene((Parent) object, 800, 600);
        loginController = loader.getController();

        screenController = new ScreenController(scene);
        screenController.add("login", (Pane) object);

        loginController.setClient(client);
        loginController.setMessagesQueue(messagesQueue);
        loginController.setScreenController(screenController);

        loader = new FXMLLoader(getClass().getResource("/explorer.fxml"));
        screenController.add("explorer", loader.load());
        explorerController = loader.getController();

        loginController.setExplorerController(explorerController);

        explorerController.setClient(client);
        explorerController.setMessagesQueue(messagesQueue);
        explorerController.setScreenController(screenController);
        explorerController.setStage(stage);

        stage.setTitle("Netty-network-storage");
        stage.setScene(scene);
        stage.show();

        loginController.auth();
    }


    @Override
    public void init() throws Exception {
        client = new Client("localhost", 9000, messagesQueue);
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(client);
    }

    @Override
    public void stop() throws Exception {
        executorService.shutdownNow();
    }
}
