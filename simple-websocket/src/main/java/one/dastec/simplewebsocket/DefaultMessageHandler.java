package one.dastec.simplewebsocket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultMessageHandler implements WsMessageHandler {

    private final String url;

    public DefaultMessageHandler(String url){
        this.url = url;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void onTextMessage(WsSession session, String message) {
        log.info("Session: "+session.getId()+ " Message: "+ message);
    }

    @Override
    public void onBinaryMessage(WsSession session, byte[] message) {

    }

    @Override
    public void onOpen(WsSession session) {
        log.info("Open Session: "+session.getId());
    }

    @Override
    public void onClose(WsSession session) {
        log.info("Close Session: "+session.getId());
    }
}
