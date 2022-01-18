package one.dastec.simplewebsocket;

import jakarta.websocket.*;
import jakarta.websocket.Session;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.CloseReasons;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.impl.SimpleLogger;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
class WebSocketServerTest {

    @BeforeAll
    public static void init() {
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "Debug");
    }

    @Test
    void startTest() throws Exception {
        URI uri = new URI("ws://localhost:9999");
        WebSocketServer server = new WebSocketServer(uri);
        Thread t = new Thread(server);
        t.start();
        int cnt = 0;
        while (cnt++ < 20) {
            if (server.isOpen()) {
                server.stop();
                break;
            } else {
                Thread.sleep(20);
            }
        }
        Assertions.assertTrue(cnt < 20, "Server did not start");

    }

    @Test
    void connectTest() throws Exception {
        String baseUriString = "ws://localhost:9999";
        String path = "/test";
        URI uri = new URI(baseUriString);
        WebSocketServer server = new WebSocketServer(uri);
        ServerMessageHandler serverHandler = new ServerMessageHandler(path);
        server.addMessageHandler(serverHandler);
        Thread t = new Thread(server);
        t.start();
        int cnt = 0;
        while (cnt++ < 20) {
            if (server.isOpen()) {
                final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

                ClientManager client = ClientManager.createClient();
                Endpoint ep = new TestClientEndPoint("On Open message from server.....................................................................................1111111111................................................................22222222222..................................................................");
                URI fullUri = new URI(baseUriString + path);
                jakarta.websocket.Session session = client.connectToServer(ep, cec, fullUri);
                Thread.sleep(5000);
                session.close(CloseReasons.GOING_AWAY.getCloseReason());
                Thread.sleep(5000);
                Assertions.assertTrue(serverHandler.isClosed());
                server.stop();
                break;
            } else {
                Thread.sleep(20);
            }
        }
        Assertions.assertTrue(cnt < 20, "Server did not start");

    }

    @Data
    class ServerMessageHandler implements WsMessageHandler {

        private final String url;
        private boolean closed=false;

        ServerMessageHandler(String url) {
            this.url = url;
        }

        @Override
        public String getUrl() {
            return url;
        }

        @Override
        public void onTextMessage(WsSession session, String message) {
            log.info("Message ["+message+"] from "+session.getId());
        }

        @Override
        public void onBinaryMessage(WsSession session, byte[] message) {

        }

        @Override
        public void onOpen(WsSession session) {
            session.sendTextMessage("On Open Message from client");
        }

        @Override
        public void onClose(WsSession session) {
            this.closed=true;
        }
    }

    interface MessageListener<T> {
        void onMessage(T message);
    }

    final List<MessageListener<String>> listeners = new ArrayList<>();

    class TestClientEndPoint extends Endpoint {

        private jakarta.websocket.Session session;


        private final String onOpenMessage;

        public TestClientEndPoint(String onOpenMessage){
            this.onOpenMessage = onOpenMessage;
        }

        public void addMessageListener(MessageListener<String> listener){
            listeners.add(listener);
        }

        public void removeMessageListener(MessageListener<String> listener){
            listeners.remove(listener);
        }

        @Override
        public void onOpen(jakarta.websocket.Session session, EndpointConfig endpointConfig) {
            try {
                this.session = session;
                session.addMessageHandler(new ClentMessageHandler());
                session.getBasicRemote().sendText(onOpenMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            super.onClose(session, closeReason);
            Assertions.assertTrue(closeReason.equals(CloseReason.CloseCodes.NORMAL_CLOSURE));
        }

    }

    class ClentMessageHandler implements MessageHandler.Whole<String> {

        @Override
        public void onMessage(String s) {
            log.info("Received message: " + s);
            for (MessageListener<String> listener: Collections.unmodifiableList(listeners)){
                listener.onMessage(s);
            }
        }
    }
}
