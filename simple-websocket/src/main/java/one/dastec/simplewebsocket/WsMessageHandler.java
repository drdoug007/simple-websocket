package one.dastec.simplewebsocket;


public interface WsMessageHandler {

    String getUrl();

    void onTextMessage(WsSession session, String message);

    void onBinaryMessage(WsSession session, byte[] message);

    void onOpen(WsSession session);

    void onClose(WsSession session);
}
