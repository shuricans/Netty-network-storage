package ru.gb.storage.client.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import ru.gb.storage.client.io.LocalFileManager;
import ru.gb.storage.client.ui.components.DownloadHBox;
import ru.gb.storage.client.ui.controller.DownloadsController;
import ru.gb.storage.client.ui.controller.ExplorerController;
import ru.gb.storage.client.ui.controller.LoginController;
import ru.gb.storage.commons.io.File;
import ru.gb.storage.commons.message.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
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
    private final DownloadsController downloadsController;
    private final LocalFileManager localFileManager = new LocalFileManager();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        // download
        if (msg instanceof FileTransferMessage) {
            var message = (FileTransferMessage) msg;
            try (RandomAccessFile raf = new RandomAccessFile(message.getDestPath(), "rw")) {
                raf.seek(message.getStartPosition());
                raf.write(message.getContent());
                if (message.getProgress() >= 0) {
                    Platform.runLater(() -> {
                        DownloadHBox downloadHBox = downloadsController.getDownloadHBox(message.getFileId());
                        if (downloadHBox == null) {
                            final DownloadHBox hBox = new DownloadHBox("download", message.getDestPath());
                            hBox.init();
                            hBox.setProgressValue(message.getProgress() * .01d);
                            downloadsController.addDownloadHBox(message.getFileId(), hBox);
                        } else {
                            downloadHBox.setProgressValue(message.getProgress() * .01d);
                        }
                        if (message.isDone()) {
                            if (downloadHBox != null) {
                                downloadHBox.setProgressValue(1d);
                            }
                            explorerController.refreshLocal();
                            explorerController.showDownloadSpinner(false);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // upload
        if (msg instanceof FileTransferProgressMessage) {
            var message = (FileTransferProgressMessage) msg;
            Platform.runLater(() -> {
                DownloadHBox downloadHBox = downloadsController.getDownloadHBox(message.getFileId());
                if (downloadHBox == null) {
                    final DownloadHBox hBox = new DownloadHBox("upload", message.getDestPath());
                    hBox.init();
                    hBox.setProgressValue(message.getProgress() * .01d);
                    downloadsController.addDownloadHBox(message.getFileId(), hBox);
                } else {
                    downloadHBox.setProgressValue(message.getProgress() * .01d);
                }
                if (message.isDone()) {
                    if (downloadHBox != null) {
                        downloadHBox.setProgressValue(1d);
                    }
                    explorerController.refreshRemote();
                    explorerController.showDownloadSpinner(false);
                }
            });
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
                fileRequestMessage.setFile(remoteFile);
                fileRequestMessage.setType(FileRequestMessage.Type.DOWNLOAD);
                fileRequestMessage.setParentDirId(remoteFile.getParentId());
                fileRequestMessage.setStorageId(remoteFile.getStorageId());
                String destPath = Paths.get(message.getPath(), remoteFile.getName()).toString();
                fileRequestMessage.setDestPath(destPath);
                fileRequestMessage.setRealPath(remoteFile.getPath());
                ctx.writeAndFlush(fileRequestMessage);
            }
        }

        if (msg instanceof ListFilesMessage) {
            var message = (ListFilesMessage) msg;
            Platform.runLater(() -> explorerController.updateRemoteFiles(message.getFiles()));
        }

        if (msg instanceof FileRequestMessage) {
            var message = (FileRequestMessage) msg;
            switch (message.getType()) {
                case UPLOAD: // server ready to accept this regular file
                    Platform.runLater(() -> {
                        final DownloadHBox downloadHBox = downloadsController.getDownloadHBox(message.getFile().getId());
                        if (downloadHBox == null) {
                            final DownloadHBox hBox = new DownloadHBox("upload", message.getDestPath());
                            hBox.init();
                            downloadsController.addDownloadHBox(message.getFile().getId(), hBox);
                        }
                    });
                    executorService.execute(
                            new FileUploader(ctx, message)
                    );
                    break;
                case GET: // server want to get all children of this directory
                    final File directory = message.getFile();
                    final ObservableList<File> localFiles = localFileManager.getLocalFiles(directory.getPath());
                    for (File localFile : localFiles) {
                        final FileRequestMessage fileRequestMessage = new FileRequestMessage();
                        fileRequestMessage.setFile(localFile);
                        fileRequestMessage.setType(FileRequestMessage.Type.UPLOAD);
                        fileRequestMessage.setParentDirId(directory.getId());
                        fileRequestMessage.setStorageId(message.getStorageId());
                        final String destPath = message.getDestPath();
                        fileRequestMessage.setDestPath(Path.of(destPath, localFile.getName()).toString());
                        ctx.writeAndFlush(fileRequestMessage);
                    }
            }
        }

        if (msg instanceof SignMessage) {
            var message = (SignMessage) msg;
            Platform.runLater(() -> loginController.auth(message));
        }
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

                    if (availableBytes > BUFFER_SIZE) {
                        buffer = new byte[BUFFER_SIZE];
                        if (currentProgress % 5 == 0 && currentProgress > progress) {
                            progress = currentProgress;
                            fileTransferMessage.setProgress(progress);
                        } else {
                            fileTransferMessage.setProgress(-1);
                        }
                    } else if (availableBytes == BUFFER_SIZE) {
                        buffer = new byte[BUFFER_SIZE];
                        isDone = true;
                        fileTransferMessage.setProgress(100);
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