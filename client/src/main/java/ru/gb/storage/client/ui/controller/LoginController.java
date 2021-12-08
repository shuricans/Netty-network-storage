package ru.gb.storage.client.ui.controller;

import com.dustinredmond.fxalert.FXAlert;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.Setter;
import ru.gb.storage.client.netty.ClientService;
import ru.gb.storage.client.utils.Utils;
import ru.gb.storage.commons.message.Sign;
import ru.gb.storage.commons.message.SignMessage;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

@Setter
public class LoginController implements Initializable, LostConnection {

    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField loginField;
    @FXML
    private Label infoLabel;
    @FXML
    private Button buttonSignIn;
    @FXML
    private Button buttonSignUp;
    @FXML
    private Hyperlink hyperLinkReconnect;
    @FXML
    private Label labelInetHost;
    @FXML
    private Label labelPort;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Hyperlink hyperLinkHost;
    @FXML
    private Hyperlink hyperLinkPort;

    private ClientService clientService;
    private ScreenController screenController;
    private ExplorerController explorerController;
    private String inetHost;
    private int port;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        final ChangeListener<String> changeFieldListener = (observableValue, oldValue, newValue) -> {
            infoLabel.setText("");
        };

        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        loginField.textProperty().addListener(changeFieldListener);
        passwordField.textProperty().addListener(changeFieldListener);
    }

    public void auth(SignMessage message) {
        progressBar.setVisible(false);
        passwordField.clear();
        if (!message.isSuccess()) {
            if (message.getInfo() != null) {
                infoLabel.setText(message.getInfo());
            } else {
                infoLabel.setText("Incorrect username or password.");
            }
        } else {
            infoLabel.setText("");
            loginField.setText("");
            screenController.activate("explorer");
            explorerController.postInit(
                    loginField.getText().trim(),
                    message.getStorageId(),
                    message.getRootDirId()
            );
        }
    }

    public void sendSignIn() {
        final String login = loginField.getText().trim();
        final String password = passwordField.getText();

        if (login.isEmpty() || password.isEmpty()) {
            infoLabel.setText("Incorrect username or password.");
            passwordField.clear();
            return;
        }
        progressBar.setVisible(true);

        final SignMessage signInMessage = new SignMessage();
        signInMessage.setType(Sign.IN);
        signInMessage.setLogin(login);
        signInMessage.setPassword(password);
        clientService.sendMessage(signInMessage);
    }

    public void sendSignUp() {
        final String login = loginField.getText().trim();
        final String password = passwordField.getText();

        if (login.isEmpty() || password.isEmpty()) {
            infoLabel.setText("Please type username and password.");
            passwordField.clear();
            return;
        }
        progressBar.setVisible(true);

        final SignMessage signUpMessage = new SignMessage();
        signUpMessage.setType(Sign.UP);
        signUpMessage.setLogin(login);
        signUpMessage.setPassword(password);
        clientService.sendMessage(signUpMessage);
    }

    public void connect() {
        beforeConnect();
        clientService.connect(inetHost, port);
    }

    public void connectionRequest(boolean success) {
        afterConnect(success);
    }

    public void changeInetHost() {
        Optional<String> newInetHostValue = FXAlert.input().withText("Enter a new host:").showAndWaitString();
        newInetHostValue.ifPresent(newInetHost -> {
            if (Utils.isValidIpAddress(newInetHost)) {
                inetHost = newInetHost;
                labelInetHost.setText(inetHost);
            } else {
                FXAlert.showError("This is [" + newInetHost + "] not valid host.");
            }
        });
    }

    public void changePort() {
        Optional<String> newPortValue = FXAlert.input().withText("Enter an new port:").showAndWaitString();
        newPortValue.ifPresent(newPort -> {
            if (Utils.isValidPort(newPort)) {
                port = Integer.parseInt(newPort);
                labelPort.setText(newPort);
            } else {
                FXAlert.showError("This is [" + newPort + "] not valid port.");
            }
        });
    }

    private void beforeConnect() {
        labelInetHost.setText(inetHost);
        labelPort.setText(String.valueOf(port));
        infoLabel.setText("");
        hyperLinkHost.setDisable(true);
        hyperLinkPort.setDisable(true);
        hyperLinkReconnect.setVisible(false);
        progressBar.setVisible(true);
    }

    private void afterConnect(boolean success) {
        progressBar.setVisible(false);
        if (success) {
            infoLabel.setText("");
            loginField.setDisable(false);
            passwordField.setDisable(false);
            buttonSignIn.setDisable(false);
            buttonSignUp.setDisable(false);
            hyperLinkReconnect.setVisible(false);
        } else {
            hyperLinkHost.setDisable(false);
            hyperLinkPort.setDisable(false);
            infoLabel.setText("Connection to server failed ...\nCheck host and port in bottom bar.");
            hyperLinkReconnect.setVisible(true);
        }
    }

    @Override
    public void lostConnection() {
        FXAlert.showError("lost connection...");
        afterConnect(false);
    }
}
