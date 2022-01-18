package one.dastec.simplewebsocket;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.ByteBuffer;
import java.security.SecureRandom;


@Data
@EqualsAndHashCode
public class Frame {

    private int frameCount;

    private boolean finalFlag = false;

    private boolean masked = false;

    @EqualsAndHashCode.Exclude
    private byte[] maskingKey = new byte[4];

    private int rsv;

    private OpCode opCode;

    private byte[] data;

    @EqualsAndHashCode.Exclude
    private long length;

    private static final SecureRandom reuseableRandom = new SecureRandom();

    public void unmask() {
        for (int i = 0; i < length; i++) {
            data[i] ^= maskingKey[i % WebSocketServer.MASK_SIZE];
        }
    }

    public byte getRsvByte(){
        switch(rsv){
            case 1: return 0x40;
            case 2: return 0x20;
            case 3: return 0x10;
            default: return 0;
        }
    }

    public byte getMaskByte(){
        return masked ? (byte)-128:0;
    }

    public int getSizeBytes(){
        if (data.length <= 125) {
            return 1;
        } else if (data.length <= 65535){
            return 2;
        }
        return 8;
    }

    private byte[] toByteArray(long val, int bytecount) {
        byte[] buffer = new byte[bytecount];
        int highest = 8 * bytecount - 8;
        for (int i = 0; i < bytecount; i++) {
            buffer[i] = (byte) (val >>> (highest - 8 * i));
        }
        return buffer;
    }

    public byte[] toBytes(){
        int sizebytes = getSizeBytes();
        ByteBuffer buf = ByteBuffer.allocate(1 + (sizebytes > 1 ? sizebytes + 1 : sizebytes) + (masked ? 4 : 0) + data.length);
        byte optcode = (byte)opCode.value;
        byte one = (byte) (finalFlag? -128 : 0);
        one |= optcode;
        one |= getRsvByte();
        buf.put(one);
        byte[] payloadLengthBytes = toByteArray(data.length, sizebytes);

        if (sizebytes == 1){
            buf.put((byte) (payloadLengthBytes[0] | getMaskByte()));
        } else if (sizebytes == 2){
            buf.put((byte) (126 | getMaskByte()));
            buf.put(payloadLengthBytes);
        } else if (sizebytes == 8){
            buf.put((byte) (127 | getMaskByte()));
            buf.put(payloadLengthBytes);
        } else {
            throw new IllegalStateException("Invalid size");
        }
        if (masked){
            ByteBuffer maskkey = ByteBuffer.allocate(4);
            maskkey.putInt(reuseableRandom.nextInt());
            buf.put(maskkey.array());
            for (int i = 0; i<data.length; i++) {
                buf.put((byte) (data[i] ^ maskkey.get(i % 4)));
            }
        } else {
            buf.put(data);
        }
        buf.flip();
        return buf.array();
    }
}
