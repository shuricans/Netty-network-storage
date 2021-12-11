package ru.gb.storage.client.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import lombok.RequiredArgsConstructor;
import ru.gb.storage.client.io.LocalFileManager;
import ru.gb.storage.client.ui.controller.DownloadsController;
import ru.gb.storage.client.ui.controller.ExplorerController;
import ru.gb.storage.client.ui.controller.LoginController;
import ru.gb.storage.commons.handler.JsonDecoder;
import ru.gb.storage.commons.handler.JsonEncoder;
import ru.gb.storage.commons.message.Message;

import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
public final class Client implements Runnable {

    private final ClientService clientService;
    private final int port;
    private final String inetHost;
    private final ExecutorService executorService;
    private final LoginController loginController;
    private final ExplorerController explorerController;
    private final DownloadsController downloadsController;
    private SocketChannel channel;

    @Override
    public void run() {
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            channel = ch;
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(
                                            1024 * 1024,
                                            0,
                                            3,
                                            0,
                                            3),
                                    new LengthFieldPrepender(3),
                                    new JsonDecoder(),
                                    new JsonEncoder(),
                                    new ClientMessageHandler(
                                            clientService,
                                            executorService,
                                            loginController,
                                            explorerController,
                                            downloadsController,
                                            new LocalFileManager(downloadsController)
                                    )
                            );
                        }
                    });
            // Start the client.
            Channel channel = bootstrap.connect(inetHost, port).sync().channel();
            System.out.println("Client started...");
            clientService.connectionResponseEvent(true);

            // Wait until the connection is closed.
            channel.closeFuture().sync();
        } catch (Exception e) {
            clientService.connectionResponseEvent(false);
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public void sendMessage(Message msg) {
        channel.writeAndFlush(msg);
    }
}