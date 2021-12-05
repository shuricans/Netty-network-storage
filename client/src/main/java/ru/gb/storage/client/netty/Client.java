package ru.gb.storage.client.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import lombok.RequiredArgsConstructor;
import ru.gb.storage.client.ui.controller.ExplorerController;
import ru.gb.storage.client.ui.controller.LoginController;
import ru.gb.storage.commons.handler.JsonDecoder;
import ru.gb.storage.commons.handler.JsonEncoder;
import ru.gb.storage.commons.message.Message;

import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
public final class Client implements Runnable {

    private final int port;
    private final String inetHost;
    private final ExecutorService executorService;
    private final LoginController loginController;
    private final ExplorerController explorerController;
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
                                            executorService,
                                            loginController,
                                            explorerController
                                    )
                            );
                        }
                    });
            // Start the client.
            Channel channel = bootstrap.connect(inetHost, port).sync().channel();
            System.out.println("Client started...");
            loginController.setClient(this);
            explorerController.setClient(this);

            // Wait until the connection is closed.
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public ChannelFuture sendMessage(Message msg) {
        return channel.writeAndFlush(msg);
    }
}