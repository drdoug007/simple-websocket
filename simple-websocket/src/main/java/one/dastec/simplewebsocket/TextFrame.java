package one.dastec.simplewebsocket;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class TextFrame extends Frame {

    public TextFrame(String message){
        this.setOpCode(OpCode.TEXT);
        this.setData(message.getBytes(StandardCharsets.UTF_8));
        this.setLength(getData().length);
        this.setFinalFlag(true);
        this.setFrameCount(1);
        this.setRsv(0);
        this.setMasked(false);
        this.setFrameCount(1);

        SecureRandom random = new SecureRandom();
        random.nextBytes(this.getMaskingKey());
    }
}
