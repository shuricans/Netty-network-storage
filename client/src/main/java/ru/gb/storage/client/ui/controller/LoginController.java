package ru.gb.storage.client.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ru.gb.storage.client.App;
import ru.gb.storage.client.netty.Client;
import ru.gb.storage.commons.message.Sign;
import ru.gb.storage.commons.message.SignMessage;

import java.net.URL;
import java.util.ResourceBundle;

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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void sendAuth(ActionEvent actionEvent) {
        final SignMessage signInMessage = new SignMessage();
        signInMessage.setType(Sign.IN);
        signInMessage.setLogin("shuricans");
        signInMessage.setPassword("password123");
        client.sendMessage(signInMessage);
    }

    public void setClient(Client client) {
        this.client = client;
    }


}
