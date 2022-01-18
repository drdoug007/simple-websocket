package one.dastec.simplewebsocket;

import java.io.IOException;

public class FrameException extends IOException {

    private String message;

    public FrameException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
