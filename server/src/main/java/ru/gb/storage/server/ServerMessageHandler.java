package ru.gb.storage.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import ru.gb.storage.commons.io.File;
import ru.gb.storage.commons.message.*;
import ru.gb.storage.io.FileManager;
import ru.gb.storage.model.Storage;
import ru.gb.storage.model.User;
import ru.gb.storage.service.AuthService;
import ru.gb.storage.service.FileService;
import ru.gb.storage.service.StorageService;
import ru.gb.storage.service.UserService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

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
                    signIn(message);
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

        if (msg instanceof FileRequestMessage) {
            var message = (FileRequestMessage) msg;
            switch (message.getType()) {
                case GET:
                    final ListFilesMessage listFilesMessage = new ListFilesMessage();

                    final File rootDir = fileService.getFileById(message.getFileId());
                    final List<File> files = fileService
                            .getFilesByDir(rootDir)
                            .stream()
                            .map(file -> new File(
                                    file.getId(),
                                    file.getName(),
                                    file.getPath(),
                                    file.getSize(),
                                    file.getIsDirectory(),
                                    file.getParentId(),
                                    file.getStorageId()
                            ))
                            .collect(Collectors.toList());

                    listFilesMessage.setFiles(files);
                    ctx.writeAndFlush(listFilesMessage);
                    break;
                case UPLOAD:
                    break;
                case DOWNLOAD:
                    break;
                default:
            }
        }
    }

    private void signIn(SignMessage message) {
        final User user = new User();
        user.setLogin(message.getLogin());
        user.setPassword(message.getPassword());
        final boolean authIsSuccess = authService.auth(user);
        message.setSuccess(authIsSuccess);
        if (authIsSuccess) {
            final User userFromDB = userService.getUserByLogin(user.getLogin());
            final Storage storage = storageService.getStorageByUser(userFromDB);
            final File rootDir = fileService.getFilesByStorage(storage)
                    .stream()
                    .filter(file -> file.getParentId() == 0)
                    .findFirst()
                    .orElse(null);
            if (rootDir != null) {
                String info = storage.getId() + ":" + rootDir.getId();
                message.setInfo(info);
            }
        }
    }

    private void signUp(SignMessage message) {

        final User userByLogin = userService.getUserByLogin(message.getLogin());
        if (userByLogin != null) {
            message.setSuccess(false);
            message.setInfo("This name [" + message.getLogin() + "] is taken, please try again.");
            return;
        }

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
        rootDir.setName(UUID.randomUUID().toString());
        rootDir.setPath(FileManager.generatePath(rootDir.getName()).toString());
        rootDir.setStorageId(newStorage.getId());
        rootDir.setSize(FileManager.DIRECTORY_SIZE);
        rootDir.setIsDirectory(true);
        rootDir.setParentId(null);
        final long rootDirId = fileService.addNewFile(rootDir);
        try {
            Files.createDirectories(Paths.get(rootDir.getPath()));
        } catch (IOException e) {
            e.printStackTrace();
            message.setSuccess(false);
            message.setInfo("Can't create remote directory, io error...");
            return;
        }
        message.setSuccess(newUserId > 0);
        String info = newStorage.getId() + ":" + rootDirId;
        message.setInfo(info);
    }

}
