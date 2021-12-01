package ru.gb.storage.client.ui.controller;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lombok.AllArgsConstructor;
import lombok.Setter;
import ru.gb.storage.client.netty.Client;
import ru.gb.storage.commons.message.Message;
import ru.gb.storage.commons.message.Sign;
import ru.gb.storage.commons.message.SignMessage;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;

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
    private BlockingQueue<Message> messagesQueue;
    private ScreenController screenController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        final ChangeListener<String> changeFieldListener = (observableValue, oldValue, newValue) -> {
            infoLabel.setText("");
        };

        loginField.textProperty().addListener(changeFieldListener);
        passwordField.textProperty().addListener(changeFieldListener);
    }

    public void auth() {
        AuthService authService = new AuthService(messagesQueue);

        authService.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue instanceof SignMessage) {
                var message = (SignMessage) newValue;
                if (!message.isSuccess()) {
                    passwordField.clear();
                    infoLabel.setText("Incorrect username or password.");
                }
            }
        });

        authService.setOnSucceeded(handler -> {
            System.out.println("Success auth");
            infoLabel.setText("");
            screenController.activate("explorer");
        });

        authService.start();
    }

    public void sendAuth() {
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

    @AllArgsConstructor
    private static class AuthService extends Service<Message> {

        private final BlockingQueue<Message> messagesQueue;

        @Override
        protected Task<Message> createTask() {
            return new Task<>() {
                @Override
                protected Message call() throws Exception {
                    while (true) {
                        final Message msg = messagesQueue.take(); // wait incoming message
                        updateValue(msg);
                        if (msg instanceof SignMessage) {
                            var message = (SignMessage) msg;
                            if (message.isSuccess()) {
                                break;
                            }
                        }
                    }
                    return null;
                }
            };
        }
    }
}
