package ru.gb.storage.client.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ru.gb.storage.commons.message.Message;
import ru.gb.storage.commons.message.SignMessage;

public class ClientMessageHandler extends SimpleChannelInboundHandler<Message> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        if (msg instanceof SignMessage) {
            var message = (SignMessage) msg;
            switch (message.getType()) {
                case IN:
                    if (message.isSuccess()) {
                        System.out.printf("User [%s] auth successful", message.getLogin());
                    } else {
                        System.out.printf("User [%s] auth failed", message.getLogin());
                    }
            }
        }
    }
}