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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
        Object object = loader.load();
        Scene scene = new Scene((Parent) object, 800, 600);
        LoginController loginController = loader.getController();

        ScreenController screenController = new ScreenController(scene);
        screenController.add("login", (Pane) object);
        loginController.setScreenController(screenController);

        loader = new FXMLLoader(getClass().getResource("/explorer.fxml"));
        screenController.add("explorer", loader.load());
        ExplorerController explorerController = loader.getController();

        loginController.setExplorerController(explorerController);

        explorerController.setScreenController(screenController);
        explorerController.setStage(stage);

        Client client = new Client(
                9000,
                "localhost",
                executorService,
                loginController,
                explorerController
        );

        executorService.execute(client);

        stage.setTitle("Netty-network-storage");
        stage.setScene(scene);
        stage.show();
    }


    @Override
    public void init() throws Exception {

    }

    @Override
    public void stop() throws Exception {
        executorService.shutdownNow();
    }
}
