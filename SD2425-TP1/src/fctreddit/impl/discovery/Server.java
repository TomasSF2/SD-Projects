package fctreddit.impl.discovery;

import java.net.URI;
import java.time.LocalDateTime;

public class Server {


    private String hostName;
    private URI uri;
    private LocalDateTime time;

    public Server(String hostName, URI uri, LocalDateTime time) {
        this.hostName = hostName;
        this.uri = uri;
        this.time = time;
    }


    public String getHostName() {
        return hostName;
    }

    public URI getUri() {
        return uri;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
