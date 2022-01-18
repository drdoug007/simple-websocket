package one.dastec.simplewebsocket;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class ConnectionHandler implements FrameSender, Closeable {

    private WebSocketServer webSocketServer;
    private final boolean allowExtensions = false;
    private WsSession session;

    private Socket socket;
    private LinkedBlockingQueue<Frame> writeQueue = new LinkedBlockingQueue<>();
    

    public ConnectionHandler(WebSocketServer webSocketServer, Socket client) {
        this.webSocketServer = webSocketServer;

        this.socket = client;

        log.debug("A client connected.");

        try (InputStream in = client.getInputStream(); OutputStream out = client.getOutputStream(); Scanner s = new Scanner(in, "UTF-8")) {

            try {
                String key = "";
                Header header = getHeader(s);
                Map<String, List<String>> hdrs = header.getHeaders();
                if (hdrs.containsKey("Sec-WebSocket-Key")) {
                    key = hdrs.get("Sec-WebSocket-Key").get(0);
                } else {
                    throw new BadRequestException();
                }
                byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                        + "Connection: Upgrade\r\n"
                        + "Upgrade: websocket\r\n"
                        + "Sec-WebSocket-Accept: "
                        + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))
                        + "\r\n\r\n").getBytes("UTF-8");
                out.write(response, 0, response.length);
                session = new WsSession(webSocketServer.getMessageHandlers().get(header.getUrl()), this);
                webSocketServer.addSession(session);
                WsMessageHandler mh = session.getMessageHandler();
                mh.onOpen(session);
                while (webSocketServer.isOpen()) {
                    if (in.available() > 0) {
                        Frame frame = readFrame(in);
                        switch (frame.getOpCode()) {
                            case BINARY:
                                mh.onBinaryMessage(session, frame.getData());
                                break;
                            case CLOSE:
                                mh.onClose(session);
                                break;
                            case CONT:
                                log.debug("CONT");
                                break;
                            case PING:
                                log.debug("PING");
                                break;
                            case PONG:
                                log.debug("PONG");
                                break;
                            case TEXT:
                                mh.onTextMessage(session, new String(frame.getData(), StandardCharsets.UTF_8));
                                break;
                            default:
                                break;
                        }
                    }
                    processWriteQueue(out);
                    Thread.sleep(20);
                }
            } catch (BadRequestException e) {
                Response badRequest = new Response("400", "Bad Request");
                out.write(badRequest.toString().getBytes(StandardCharsets.UTF_8));
                out.flush();
            } finally {
                in.close();
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            webSocketServer.removeSession(session.getId());
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    private void processWriteQueue(OutputStream out) throws IOException {
        if (!writeQueue.isEmpty()) {
            Frame frame = writeQueue.poll();
            byte[] b = frame.toBytes();
            out.write(b, 0, b.length);
            out.flush();
        }

    }

    @Override
    public void close() {
        try {
            this.socket.close();
        } catch (IOException e) {
        }
    }

    @Override
    public void sendFrame(Frame frame){

        try (ByteArrayInputStream in = new ByteArrayInputStream(frame.toBytes())){
            Frame newFrame = this.readFrame(in);
            System.out.println(newFrame.equals(frame));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.writeQueue.add(frame);
    }

    private byte[] encodeLength(final long length) {
        byte[] lengthBytes;
        if (length <= 125) {
            lengthBytes = new byte[1];
            lengthBytes[0] = (byte) length;
        } else {
            byte[] b = toArray(length);
            if (length <= 0xFFFF) {
                lengthBytes = new byte[3];
                lengthBytes[0] = 126;
                System.arraycopy(b, 6, lengthBytes, 1, 2);
            } else {
                lengthBytes = new byte[9];
                lengthBytes[0] = 127;
                System.arraycopy(b, 0, lengthBytes, 1, 8);
            }
        }
        return lengthBytes;
    }

    public byte[] toArray(long value) {
        byte[] b = new byte[8];
        for (int i = 7; i >= 0 && value > 0; i--) {
            b[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return b;
    }


    private Frame readFrame(InputStream in) throws IOException {
        Frame frame = new Frame();

        byte[] startFrame = new byte[2];
        int c = in.read(startFrame);

        if (c != startFrame.length){
            throw new FrameException("Invalid startFrame");
        }
        frame.setOpCode(OpCode.find(startFrame[0] & 0x0f));
        frame.setRsv((startFrame[0] & 0x70) >> 4);
        frame.setFinalFlag((startFrame[0] & 0x80) != 0);
        frame.setMasked((startFrame[1] & 0x80) != 0);
        int payload1 = startFrame[1] & 0x7F;

        if (frame.getRsv() != 0 && !this.allowExtensions) {
            throw new FrameException("Extensions not allowed");
        }
        // if (!frame.isMasked()) {
        //     throw new FrameException("Message not masked");
        // }

        if (frame.getOpCode().value > 7) {
            if (!frame.isFinalFlag()) {
                throw new FrameException("Fragmented control frame");
            }

            if (payload1 > 125) {

            }

            if (frame.getOpCode() == OpCode.CLOSE && payload1 < 2) {

            }
        } else {

            if (!(frame.getOpCode() == OpCode.CONT || frame.getOpCode() == OpCode.TEXT || frame.getOpCode() == OpCode.BINARY)) {
                throw new FrameException("Invalid opcode");
            }

            if (frame.getFrameCount() == 0 && frame.getOpCode() == OpCode.CONT) {
                throw new FrameException("continuation data outside of fragmented data");
            }
            if (frame.getFrameCount() != 0 && frame.getOpCode() != OpCode.CONT) {
                throw new FrameException("invalid fragmented data");
            }
        }

        if (payload1 == 126) {
            frame.setLength(readShort(in));
        } else if (payload1 == 127) {
            frame.setLength(readLong(in));
        } else {
            frame.setLength(payload1);
        }

        log.debug(frame.getOpCode() +" Frame length = " + frame.getLength());

        if (frame.isMasked()) {
            c = in.read(frame.getMaskingKey());
            if (c != frame.getMaskingKey().length) {
                throw new ReadException("Invalid number of bytes read");
            }
        }

        frame.setData(new byte[(int) frame.getLength()]);
        c = in.read(frame.getData());
        if (c != frame.getData().length) {
            throw new ReadException("Invalid number of bytes read");
        }

        if (frame.isMasked()) {
            frame.unmask();
        }

        frame.setFrameCount(frame.getFrameCount()+1);
        return frame;
    }


    private int readInt(InputStream in) throws IOException {
        return (readShort(in) & 0xFFFF) << 16 | readShort(in) & 0xFFFF;
    }

    private long readLong(InputStream in) throws IOException {
        return (readInt(in) & 0xFFFFFFFL) << 32 | readInt(in) & 0xFFFFFFFL;
    }

    private int readShort(InputStream in) throws IOException {
        byte[] l = new byte[2];
        int c = in.read(l);
        if (c != l.length) {
            throw new ReadException("Invalid number of bytes read");
        }
        short s = (short) ((l[0] & 0xff) << 8 | (l[1]) & 0xff);
        return s & 0xFFFF;
    }

    public Header getHeader(Scanner s) throws BadRequestException {
        String firstLine = s.useDelimiter("\\r\\n").next();

        Header header = new Header(firstLine, "GET", this.webSocketServer.getMessageHandlers().keySet());

        do {
            String line = s.useDelimiter("\\r\\n").next();
            if (line.length() == 0) {
                break;
            }
            header.addHeaderLine(line);
        } while (true);

        return header;
    }


}
