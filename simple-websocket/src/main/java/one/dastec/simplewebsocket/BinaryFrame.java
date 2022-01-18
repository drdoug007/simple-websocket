package one.dastec.simplewebsocket;

import java.security.SecureRandom;

public class BinaryFrame extends Frame {

    public BinaryFrame(byte[] message){
        this.setOpCode(OpCode.BINARY);
        this.setData(message);
        this.setLength(getData().length);
        this.setFinalFlag(true);
        this.setFrameCount(1);
        this.setRsv(0);
        this.setMasked(false);

        SecureRandom random = new SecureRandom();
        random.nextBytes(this.getMaskingKey());
    }
}
