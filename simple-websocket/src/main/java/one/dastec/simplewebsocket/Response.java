package one.dastec.simplewebsocket;

import lombok.Data;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class Response {

    private String status;
    private String message;
    private String body;
    private Map<String, String> headers = new LinkedHashMap<>();

    public Response(String status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String toString() {
        String msg = "HTTP/1.1 " + status + " " + message + "\r\n";
        msg += "Date: " + new Date() + "\r\n";
        msg += "Server: TouchPoint Teller\r\n";
        // msg += "Content-Length: "+body.length()+"\r\n";
        for (Map.Entry<String, String> header : headers.entrySet()) {
            msg += header.getKey() + ": " + header.getValue() + "\r\n";
        }
        return message + "\r\n";
    }


}
