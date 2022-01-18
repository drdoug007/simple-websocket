package one.dastec.simplewebsocket;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Data
@RequiredArgsConstructor
public class WsSession {

    private final String id = UUID.randomUUID().toString();

    private final WsMessageHandler messageHandler;
    private final FrameSender frameSender;

    public void sendTextMessage(String message){
        Frame frame = new TextFrame(message);
        frameSender.sendFrame(frame);
    }

    public void sendBinaryMessage(byte[] message){
        Frame frame = new BinaryFrame(message);
        frameSender.sendFrame(frame);
    }

}
