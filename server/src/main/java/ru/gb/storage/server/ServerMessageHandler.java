package ru.gb.storage.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import ru.gb.storage.commons.message.*;
import ru.gb.storage.model.User;
import ru.gb.storage.service.AuthService;
import ru.gb.storage.service.UserService;

import java.util.concurrent.Executor;

@AllArgsConstructor
public class ServerMessageHandler extends SimpleChannelInboundHandler<Message> {

    private final Executor executor;
    private final AuthService authService;
    private final UserService userService;

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
                    final User newUser = new User();
                    newUser.setLogin(message.getLogin());
                    newUser.setPassword(message.getPassword());
                    final boolean addSuccess = userService.addNewUser(newUser);
                    message.setSuccess(addSuccess);
                    break;
                default:
            }
            message.setPassword(""); // clear raw password before response
            ctx.writeAndFlush(message);
        }
    }
}
