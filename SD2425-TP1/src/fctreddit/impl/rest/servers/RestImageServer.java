package fctreddit.impl.rest.servers;

import fctreddit.api.java.Image;
import fctreddit.impl.discovery.Discovery;
import fctreddit.impl.java.servers.JavaImage;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

public class RestImageServer {
    private static Logger Log = Logger.getLogger(RestUsersServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 4567;
    private static final String SERVER_URI_FMT = "http://%s:%s/rest";

    public static void main(String[] args) {
        try {

            ResourceConfig config = new ResourceConfig();
            config.register(RestImageResource.class);

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format(SERVER_URI_FMT, ip, PORT);
            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

            Discovery disc = new Discovery(Discovery.DISCOVERY_ADDR, Image.NAME, serverURI);
            disc.start();

            Log.info(String.format("%s Server ready @ %s\n", Image.NAME, serverURI));
            JavaImage.serverURI = serverURI;

            // More code can be executed here...
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
