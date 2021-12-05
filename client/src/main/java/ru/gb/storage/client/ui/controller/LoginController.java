package ru.gb.storage.client.ui.controller;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lombok.Setter;
import ru.gb.storage.client.netty.Client;
import ru.gb.storage.commons.message.Sign;
import ru.gb.storage.commons.message.SignMessage;

import java.net.URL;
import java.util.ResourceBundle;

@Setter
public class LoginController implements Initializable {

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

    private Client client;
    private ScreenController screenController;
    private ExplorerController explorerController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        final ChangeListener<String> changeFieldListener = (observableValue, oldValue, newValue) -> {
            infoLabel.setText("");
        };

        loginField.textProperty().addListener(changeFieldListener);
        passwordField.textProperty().addListener(changeFieldListener);
    }

    public void auth(SignMessage message) {
        if (!message.isSuccess()) {
            passwordField.clear();
            if (message.getInfo() != null) {
                infoLabel.setText(message.getInfo());
            } else {
                infoLabel.setText("Incorrect username or password.");
            }
        } else {
            infoLabel.setText("");
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

        final SignMessage signInMessage = new SignMessage();
        signInMessage.setType(Sign.IN);
        signInMessage.setLogin(login);
        signInMessage.setPassword(password);
        client.sendMessage(signInMessage);
    }

    public void sendSignUp() {
        final String login = loginField.getText().trim();
        final String password = passwordField.getText();

        if (login.isEmpty() || password.isEmpty()) {
            infoLabel.setText("Please type username and password.");
            passwordField.clear();
            return;
        }

        final SignMessage signUpMessage = new SignMessage();
        signUpMessage.setType(Sign.UP);
        signUpMessage.setLogin(login);
        signUpMessage.setPassword(password);
        client.sendMessage(signUpMessage);
    }
}
