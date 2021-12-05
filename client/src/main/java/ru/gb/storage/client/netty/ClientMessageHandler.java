package ru.gb.storage.client.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;
import lombok.AllArgsConstructor;
import ru.gb.storage.commons.message.Message;

import java.util.concurrent.LinkedBlockingQueue;

@AllArgsConstructor
public class ClientMessageHandler extends SimpleChannelInboundHandler<Message> {

    private final LinkedBlockingQueue<Message> messagesQueue;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        try {
            messagesQueue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}