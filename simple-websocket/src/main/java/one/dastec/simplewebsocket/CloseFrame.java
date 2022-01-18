package one.dastec.simplewebsocket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class CloseFrame extends Frame {

    public CloseFrame(byte[] message){

        this.setMasked(false);
        this.setData(message);
        init();
    }

    public CloseFrame(boolean masked, Long reasonCode, String reason){
        this.setOpCode(OpCode.CLOSE);
        int msglength = 0;
        msglength = msglength + ((reasonCode==null)?0:2) + ((reasonCode==null)?0:reason.getBytes(StandardCharsets.UTF_8).length);
        ByteBuffer buf = ByteBuffer.allocate(msglength);
        if (reasonCode!=null){
            buf.putInt(reasonCode.intValue());
        }
        if (reason!=null){
            buf.put(reason.getBytes(StandardCharsets.UTF_8));
        }
        if (buf.hasArray()) {
            this.setData(buf.array());
        } else {
            this.setData(new byte[0]);
        }
        this.setMasked(masked);

        init();
    }

    private void init(){
        this.setOpCode(OpCode.CLOSE);
        this.setLength(getData().length);
        this.setFinalFlag(true);
        this.setFrameCount(1);
        this.setRsv(0);
        SecureRandom random = new SecureRandom();
        random.nextBytes(this.getMaskingKey());
    }
}
