package ru.gb.storage.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.gb.storage.commons.handler.JsonDecoder;
import ru.gb.storage.commons.handler.JsonEncoder;
import ru.gb.storage.dao.*;
import ru.gb.storage.service.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public final class Server {

    private final int PORT;

    public Server(int port) {
        PORT = port;
    }

    public void start() {
        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        final ExecutorService executorService = Executors.newCachedThreadPool();
        final UserDao userDao = new UserDaoImpl();
        final StorageDao storageDao = new StorageDaoImpl();
        final FileDao fileDao = new FileDaoImpl();
        final PasswordEncoder encoder = new BCryptPasswordEncoder();
        final UserService userService = new UserServiceImpl(userDao, encoder);
        final AuthService authService = new AuthServiceImpl(userService, encoder);
        final StorageService storageService = new StorageServiceImpl(storageDao);
        final FileService fileService = new FileServiceImpl(fileDao);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 3, 0, 3),
                                    new LengthFieldPrepender(3),
                                    new JsonDecoder(),
                                    new JsonEncoder(),
                                    new ServerMessageHandler(
                                            executorService,
                                            authService,
                                            userService,
                                            storageService,
                                            fileService
                                    )
                            );
                        }
                    });

            // Start the server.
            ChannelFuture f = b.bind(PORT).sync();
            System.out.printf("Server started on port: %s", PORT);

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            executorService.shutdownNow();
        }
    }
}