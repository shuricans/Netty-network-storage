package ru.gb.storage.client.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import lombok.AllArgsConstructor;
import ru.gb.storage.client.io.LocalFileManager;
import ru.gb.storage.client.ui.controller.ExplorerController;
import ru.gb.storage.client.ui.controller.LoginController;
import ru.gb.storage.commons.io.File;
import ru.gb.storage.commons.message.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static ru.gb.storage.commons.Constant.BUFFER_SIZE;

@AllArgsConstructor
public class ClientMessageHandler extends SimpleChannelInboundHandler<Message> {

    private final ClientService clientService;
    private final ExecutorService executorService;
    private final LoginController loginController;
    private final ExplorerController explorerController;
    private final LocalFileManager localFileManager = new LocalFileManager();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        if (msg instanceof FileTransferMessage) {
            var message = (FileTransferMessage) msg;
            try(RandomAccessFile raf = new RandomAccessFile(message.getPath(), "rw")) {
                raf.seek(message.getStartPosition());
                raf.write(message.getContent());
                if (message.getIsDone()) {
                    Platform.runLater(explorerController::refreshLocal);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (msg instanceof PingMessage) {
            clientService.pingResponseEvent();
        }

        if (msg instanceof NestedFilesRequestMessage) {
            var message = (NestedFilesRequestMessage) msg;
            localFileManager.createDir(Paths.get(message.getPath()));
            final List<File> remoteNestedFiles = message.getFiles();
            for (File remoteFile : remoteNestedFiles) {
                final FileRequestMessage fileRequestMessage = new FileRequestMessage();
                fileRequestMessage.setType(FileRequestType.DOWNLOAD);
                fileRequestMessage.setStorageId(remoteFile.getStorageId());
                fileRequestMessage.setFile(remoteFile);
                String path = Paths.get(message.getPath(), remoteFile.getName()).toString();
                fileRequestMessage.setPath(path);
                ctx.writeAndFlush(fileRequestMessage);
            }

        }


        if (msg instanceof ListFilesMessage) {
            var message = (ListFilesMessage) msg;
            Platform.runLater(() -> {
                explorerController.updateRemoteFiles(message.getFiles());
            });
        }

        if (msg instanceof FileRequestMessage) {
            var message = (FileRequestMessage) msg;
            switch (message.getType()) {
                case UPLOAD: // server ready to accept this regular file
                    executorService.execute(
                            new FileUploader(ctx, message)
                    );
                    break;
                case GET: // server want to get all children of this directory
                    final File directory = message.getFile();
                    final ObservableList<File> localFiles = localFileManager.getLocalFiles(directory.getPath());
                    for (File localFile : localFiles) {
                        final FileRequestMessage fileRequestMessage = new FileRequestMessage();
                        fileRequestMessage.setType(FileRequestType.UPLOAD);
                        fileRequestMessage.setFile(localFile);
                        fileRequestMessage.setStorageId(message.getStorageId());
                        fileRequestMessage.setParentDirId(directory.getId());
                        ctx.writeAndFlush(fileRequestMessage);
                    }
            }
        }

        if (msg instanceof SignMessage) {
            var message = (SignMessage) msg;
            Platform.runLater(() -> {
                loginController.auth(message);
            });
        }
    }

    @AllArgsConstructor
    private static class FileUploader implements Runnable {

        private final ChannelHandlerContext ctx;
        private final FileRequestMessage message;

        @Override
        public void run() {
            try (final RandomAccessFile raf = new RandomAccessFile(message.getFile().getPath(), "r")) {
                final long fileLength = raf.length();
                boolean isDone = false;
                do {
                    final long filePointer = raf.getFilePointer();
                    final long availableBytes = fileLength - filePointer;

                    byte[] buffer;

                    if (availableBytes >= BUFFER_SIZE) {
                        buffer = new byte[BUFFER_SIZE];
                    } else {
                        buffer = new byte[(int) availableBytes];
                        isDone = true;
                    }

                    raf.read(buffer);

                    final FileTransferMessage fileTransferMessage = new FileTransferMessage();

                    fileTransferMessage.setContent(buffer);
                    fileTransferMessage.setStartPosition(filePointer);
                    fileTransferMessage.setIsDone(isDone);
                    fileTransferMessage.setPath(message.getPath());

                    ctx.writeAndFlush(fileTransferMessage).sync();
                } while (raf.getFilePointer() < fileLength);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}