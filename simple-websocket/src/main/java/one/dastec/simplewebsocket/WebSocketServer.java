package one.dastec.simplewebsocket;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class WebSocketServer implements Runnable {

	
	private Map<String, WsSession> sessionMap = new ConcurrentHashMap<>();
	
	private AtomicBoolean isOpen = new AtomicBoolean(false);
	public static final int MASK_SIZE = 4;
	private final List<ConnectionHandler> handlers = new ArrayList<>();
	private URI baseUri;
	private final Map<String, WsMessageHandler> messageHandlers = new ConcurrentHashMap<>();
	private ServerSocket server;
	
	public WebSocketServer(URI baseUri){
		this.baseUri = baseUri;
	}
	
	public void addMessageHandler(WsMessageHandler messageHandler){
		this.messageHandlers.put(messageHandler.getUrl(), messageHandler);
	}

	@Override
	public void run() {
		try { 
			if (isOpen.get()) {
				return;
			}
			try {
				server = new ServerSocket(baseUri.getPort());
				isOpen.set(true);
				while (isOpen.get()) {
					log.info("WebSocket Server has started on: "+baseUri.toString());
					log.info("Waiting for a connection...");
					Socket client = server.accept();
					ConnectionHandler handler = new ConnectionHandler(this, client);
					handlers.add(handler);
				}
			} finally {
				if (server!=null){
					server.close();
				}
			}
		} catch (Exception e) {
			
		}
	}

	public Map<String, WsMessageHandler> getMessageHandlers(){
		return messageHandlers;
	}

	public void addSession(WsSession session){
		this.sessionMap.put(session.getId(), session);
	}

	public void removeSession(String sessionId){
		sessionMap.remove(sessionId);
	}

	public WsSession getSession(String sessionId){
		return sessionMap.get(sessionId);
	}

	public void stop() {
		isOpen.set(false);
		for (ConnectionHandler handler: new ArrayList<>(handlers)) {
			handler.close();
		}
		handlers.clear();
		try {
			if (server!=null) {
				server.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isOpen(){
		return this.isOpen.get();
	}

}
