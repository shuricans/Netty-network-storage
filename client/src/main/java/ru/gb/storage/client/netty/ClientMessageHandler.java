package ru.gb.storage.client.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;
import lombok.AllArgsConstructor;
import ru.gb.storage.client.ui.controller.ExplorerController;
import ru.gb.storage.client.ui.controller.LoginController;
import ru.gb.storage.commons.message.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;

@AllArgsConstructor
public class ClientMessageHandler extends SimpleChannelInboundHandler<Message> {

    private final ExecutorService executorService;
    private final LoginController loginController;
    private final ExplorerController explorerController;
    private static final int BUFFER_SIZE = 64 * 1024;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        if (msg instanceof FileTransferMessage) {
            var message = (FileTransferMessage) msg;
            try(RandomAccessFile raf = new RandomAccessFile(message.getPath(), "rw")) {
                raf.seek(message.getStartPosition());
                raf.write(message.getContent());
                if (message.getIsDone()) {
                    System.out.println("File transfer is finished");
                }
            } catch (IOException e) {
                e.printStackTrace();
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
                case UPLOAD:
                    executorService.execute(
                            new FileUploader(ctx, message)
                    );
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