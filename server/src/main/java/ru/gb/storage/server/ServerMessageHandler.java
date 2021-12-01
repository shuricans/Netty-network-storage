package ru.gb.storage.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import ru.gb.storage.commons.message.*;
import ru.gb.storage.io.FileManager;
import ru.gb.storage.model.File;
import ru.gb.storage.model.Storage;
import ru.gb.storage.model.User;
import ru.gb.storage.service.AuthService;
import ru.gb.storage.service.FileService;
import ru.gb.storage.service.StorageService;
import ru.gb.storage.service.UserService;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.Executor;

@AllArgsConstructor
public class ServerMessageHandler extends SimpleChannelInboundHandler<Message> {

    private final Executor executor;
    private final AuthService authService;
    private final UserService userService;
    private final StorageService storageService;
    private final FileService fileService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {

        if (msg instanceof SignMessage) {
            var message = (SignMessage) msg;
            switch (message.getType()) {
                case IN:
                    final User user = new User();
                    user.setLogin(message.getLogin());
                    user.setPassword(message.getPassword());
                    final boolean authIsSuccess = authService.auth(user);
                    message.setSuccess(authIsSuccess);
                    break;
                case OUT:
                    message.setSuccess(true);
                    break;
                case UP:
                    signUp(message);
                    break;
                default:
            }
            message.setPassword(""); // clear raw password before response
            ctx.writeAndFlush(message);
        }
    }

    private void signUp(SignMessage message) {
        // create new User
        final User newUser = new User();
        newUser.setLogin(message.getLogin());
        newUser.setPassword(message.getPassword());
        final long newUserId = userService.addNewUser(newUser);
        newUser.setId(newUserId);

        // create new Storage for new User
        final Storage newStorage = storageService.addNewStorage(newUser);

        // create root directory for new User
        final File rootDir = new File();
        rootDir.setStorageId(newStorage.getId());
        String rootDirName = UUID.randomUUID().toString();
        rootDir.setPath(FileManager.generatePath(rootDirName));
        fileService.addNewFile(rootDir);

        try {
            Files.createDirectories(rootDir.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        message.setSuccess(newUserId > 0);
    }

}
