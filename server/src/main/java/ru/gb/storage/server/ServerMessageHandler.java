package ru.gb.storage.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
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
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static ru.gb.storage.commons.Constant.BUFFER_SIZE;

@AllArgsConstructor
public class ServerMessageHandler extends SimpleChannelInboundHandler<Message> {

    private final Executor executor;
    private final AuthService authService;
    private final UserService userService;
    private final StorageService storageService;
    private final FileService fileService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {

        if (msg instanceof PingMessage) {
            ctx.writeAndFlush(msg);
        }

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
                    getEventHandler(ctx, message);
                    break;
                case UPLOAD:
                    uploadEventHandler(ctx, message);
                    break;
                case DOWNLOAD:
                    downloadEventHandler(ctx, message);
                    break;
                case DELETE:
                    deleteEventHandler(ctx, message);
                default:
            }
        }

        if (msg instanceof FileTransferMessage) {
            var message = (FileTransferMessage) msg;
            try (RandomAccessFile raf = new RandomAccessFile(message.getRealPath(), "rw")) {
                raf.seek(message.getStartPosition());
                raf.write(message.getContent());
                if (message.getProgress() >= 0) {
                    ctx.writeAndFlush(
                            new FileTransferProgressMessage(
                                    message.getFileId(),
                                    message.getProgress(),
                                    message.getDestPath(),
                                    message.isDone()
                            )
                    );
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getEventHandler(ChannelHandlerContext ctx, FileRequestMessage message) {
        final ListFilesMessage listFilesMessage = new ListFilesMessage();

        final File rootDir = fileService.getFileById(message.getParentDirId());
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
    }

    // client wants upload his file or directory on server
    private void uploadEventHandler(ChannelHandlerContext ctx, FileRequestMessage message) {
        final File fileFromClient = message.getFile();

        // is directory?
        if (fileFromClient.getIsDirectory()) {
            // create virtual directory in db
            final File virtualDir = new File();
            virtualDir.setName(fileFromClient.getName());
            virtualDir.setPath("");
            virtualDir.setSize(0L);
            virtualDir.setIsDirectory(true);
            virtualDir.setParentId(message.getParentDirId());
            virtualDir.setStorageId(message.getStorageId());

            final long virtualDirId = fileService.addNewFile(virtualDir);
            // now we have our virtual directory id
            // and we send specific message to client
            // give all files in this directory
            message.setType(FileRequestMessage.Type.GET);
            message.getFile().setId(virtualDirId);
        } else { // if regular file
            final File futureFileOnServer = new File();
            final String newFileName = UUID.randomUUID().toString();
            final FileManager.Pathway pathway = FileManager.generatePath(newFileName);
            futureFileOnServer.setName(fileFromClient.getName());
            futureFileOnServer.setPath(pathway.getFullPath());
            futureFileOnServer.setSize(fileFromClient.getSize());
            futureFileOnServer.setIsDirectory(fileFromClient.getIsDirectory());
            futureFileOnServer.setParentId(message.getParentDirId());
            futureFileOnServer.setStorageId(message.getStorageId());

            try {
                Files.createDirectories(Paths.get(pathway.getDirectories()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            final long id = fileService.addNewFile(futureFileOnServer);

            message.getFile().setId(id);
            message.setRealPath(futureFileOnServer.getPath());
        }
        ctx.writeAndFlush(message);
    }

    // client wants download this file from server
    private void downloadEventHandler(ChannelHandlerContext ctx, FileRequestMessage message) {
        final File file = message.getFile();
        if (file.getIsDirectory()) {
            final File dir = fileService.getFileById(file.getId());
            final List<File> files = fileService.getFilesByDir(dir);
            final NestedFilesRequestMessage nestedFilesRequestMessage =
                    new NestedFilesRequestMessage(files, message.getDestPath());
            ctx.writeAndFlush(nestedFilesRequestMessage);
            return;
        }
        executor.execute(new FileUploader(ctx, message));
//        executor.execute(() -> {
//            try (final RandomAccessFile raf = new RandomAccessFile(file.getPath(), "r")) {
//                final long fileLength = raf.length();
//                boolean isDone = false;
//                int progress = -1;
//
//                do {
//                    final long filePointer = raf.getFilePointer();
//                    final long availableBytes = fileLength - filePointer;
//
//                    byte[] buffer;
//
//                    final FileTransferMessage fileTransferMessage = new FileTransferMessage();
//                    int currentProgress = (int) ((filePointer / (fileLength * 1f)) * 100);
//
//                    if (availableBytes >= BUFFER_SIZE) {
//                        buffer = new byte[BUFFER_SIZE];
//                        if (currentProgress % 5 == 0 && currentProgress > progress) {
//                            progress = currentProgress;
//                            fileTransferMessage.setProgress(progress);
//                        } else {
//                            fileTransferMessage.setProgress(-1);
//                        }
//                    } else {
//                        buffer = new byte[(int) availableBytes];
//                        isDone = true;
//                    }
//
//                    raf.read(buffer);
//
//
//                    fileTransferMessage.setContent(buffer);
//                    fileTransferMessage.setStartPosition(filePointer);
//                    fileTransferMessage.setDone(isDone);
//                    fileTransferMessage.setDestPath(message.getRealPath());
//
//                    ctx.writeAndFlush(fileTransferMessage).sync();
//                } while (raf.getFilePointer() < fileLength);
//            } catch (IOException | InterruptedException e) {
//                e.printStackTrace();
//            }
//        });
    }

    private void deleteEventHandler(ChannelHandlerContext ctx, FileRequestMessage message) {

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
            final File rootDir = fileService.getRootDir(storage);
            if (rootDir != null) {
                message.setStorageId(storage.getId());
                message.setRootDirId(rootDir.getId());
            } else {
                message.setSuccess(false);
                message.setInfo("Fail on database layer...");
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

        if (newUserId < 0) {
            message.setSuccess(false);
            message.setInfo("Failed to create new user");
            return;
        }
        newUser.setId(newUserId);

        // create new Storage for new User
        final Storage newStorage = storageService.addNewStorage(newUser);

        // create root directory for new User
        final File rootDir = new File();
        rootDir.setName("rootPackage_" + newUserId);
        rootDir.setPath("");
        rootDir.setStorageId(newStorage.getId());
        rootDir.setSize(0L);
        rootDir.setIsDirectory(true);
        rootDir.setParentId(null);
        final long rootDirId = fileService.addNewFile(rootDir);
        message.setSuccess(true);
        message.setStorageId(newStorage.getId());
        message.setRootDirId(rootDirId);
    }

    @RequiredArgsConstructor
    private static class FileUploader implements Runnable {

        private final ChannelHandlerContext ctx;
        private final FileRequestMessage message;
        private int progress = -1;

        @Override
        public void run() {
            try (final RandomAccessFile raf = new RandomAccessFile(message.getFile().getPath(), "r")) {
                final long fileLength = raf.length();
                boolean isDone = false;
                do {
                    final long filePointer = raf.getFilePointer();
                    final long availableBytes = fileLength - filePointer;

                    byte[] buffer;

                    final FileTransferMessage fileTransferMessage = new FileTransferMessage();
                    int currentProgress = (int) ((filePointer / (fileLength * 1f)) * 100);

                    if (availableBytes >= BUFFER_SIZE) {
                        buffer = new byte[BUFFER_SIZE];
                        if (currentProgress % 5 == 0 && currentProgress > progress) {
                            progress = currentProgress;
                            fileTransferMessage.setProgress(progress);
                        } else {
                            fileTransferMessage.setProgress(-1);
                        }
                    } else {
                        buffer = new byte[(int) availableBytes];
                        isDone = true;
                        fileTransferMessage.setProgress(100);
                    }

                    raf.read(buffer);

                    fileTransferMessage.setFileId(message.getFile().getId());
                    fileTransferMessage.setContent(buffer);
                    fileTransferMessage.setStartPosition(filePointer);
                    fileTransferMessage.setDone(isDone);
                    fileTransferMessage.setDestPath(message.getDestPath());
                    fileTransferMessage.setRealPath(message.getRealPath());

                    ctx.writeAndFlush(fileTransferMessage).sync();
                } while (raf.getFilePointer() < fileLength);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
