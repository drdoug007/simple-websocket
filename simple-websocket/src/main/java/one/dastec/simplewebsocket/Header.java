package one.dastec.simplewebsocket;

import lombok.Data;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class Header {
    private String method;
    private String url;
    private Map<String, List<String>> headers = new LinkedHashMap<>();

    public Header(String line1, String requestMethod, Set<String> urls) throws BadRequestException {
        String[] line = line1.split(" ");
        method = line[0];
        url = line[1];
        if (!method.equals(requestMethod)) {
            throw new BadRequestException();
        }
        checkUrl(urls, line[1]);
    }

    private void checkUrl(Set<String> urls, String s) throws BadRequestException {
        if (!urls.contains(s)){
            throw new BadRequestException();
        }
    }

    public void addHeaderLine(String line) throws BadRequestException {
        Matcher matcher = Pattern.compile("^([A-Za-z][-A-Za-z0-9]+): (.*)$").matcher(line);
        if (matcher.matches()) {
            List<String> values = Arrays.asList(matcher.group(2).split(";"));
            headers.put(matcher.group(1), values);
        } else {
            throw new BadRequestException();
        }
    }


}
