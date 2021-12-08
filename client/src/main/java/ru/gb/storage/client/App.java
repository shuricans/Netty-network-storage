package ru.gb.storage.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import ru.gb.storage.client.netty.ClientService;
import ru.gb.storage.client.ui.controller.ExplorerController;
import ru.gb.storage.client.ui.controller.LoginController;
import ru.gb.storage.client.ui.controller.ScreenController;
import ru.gb.storage.client.utils.ConfigProperties;
import ru.gb.storage.client.utils.Utils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {

    private ExecutorService executorService;
    private String defaultInetHost;
    private int defaultPort;

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        final Parameters parameters = getParameters();
        final List<String> args = parameters.getRaw();

        String inetHost = defaultInetHost;
        int port = defaultPort;

        if (!args.isEmpty()) {
            String inetHostFromArgs = args.get(0);
            final boolean isValidAddress = Utils.isValidIpAddress(inetHostFromArgs);
            if (isValidAddress) {
                inetHost = inetHostFromArgs;
            }
            if (args.size() == 2) {
                final String portFromArgs = args.get(1);
                final boolean isValidPort = Utils.isValidPort(portFromArgs);
                if (isValidPort) {
                    port = Integer.parseInt(portFromArgs);
                }
            }
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Object object = loader.load();
        Scene scene = new Scene((Parent) object, 800, 600);
        LoginController loginController = loader.getController();

        ScreenController screenController = new ScreenController(scene);
        screenController.add("login", (Pane) object);
        loginController.setScreenController(screenController);
        screenController.setActive("login");

        loader = new FXMLLoader(getClass().getResource("/fxml/explorer.fxml"));
        screenController.add("explorer", loader.load());
        ExplorerController explorerController = loader.getController();

        loginController.setExplorerController(explorerController);

        explorerController.setScreenController(screenController);
        explorerController.setLoginController(loginController);
        explorerController.setStage(stage);


        loginController.setInetHost(inetHost);
        loginController.setPort(port);

        ClientService clientService = new ClientService(
                inetHost,
                port,
                executorService,
                loginController,
                explorerController,
                screenController
        );

        loginController.setClientService(clientService);
        explorerController.setClientService(clientService);

        final Optional<String> title = ConfigProperties.getPropertyValue("title");
        title.ifPresent(stage::setTitle);
        stage.setScene(scene);
        stage.show();

        loginController.connect();
    }


    @Override
    public void init() {
        final String nThreads = ConfigProperties.getPropertyValue("nThreads").orElse("3");
        executorService = Executors.newFixedThreadPool(Integer.parseInt(nThreads));
        defaultInetHost = ConfigProperties.getPropertyValue("inetHost").orElse("localhost");
        defaultPort = Integer.parseInt(ConfigProperties.getPropertyValue("port").orElse("9000"));
    }

    @Override
    public void stop() {
        executorService.shutdownNow();
    }
}
