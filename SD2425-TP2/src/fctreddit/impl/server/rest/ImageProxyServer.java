package fctreddit.impl.server.rest;

import fctreddit.impl.server.Discovery;
import fctreddit.impl.server.java.JavaImage;
import fctreddit.impl.server.java.JavaImageProxy;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

public class ImageProxyServer {

    private static Logger Log = Logger.getLogger(ImageServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;
    public static final String SERVICE = "Image";
    private static final String SERVER_URI_FMT = "https://%s:%s/rest";
    private static final String ALBUM = "album-";


    public static void main(String[] args) {
        try {
            ResourceConfig config = new ResourceConfig();
            config.register(ImageProxyResource.class);

            String hostname = InetAddress.getLocalHost().getHostName();
            String serverURI = String.format(SERVER_URI_FMT, hostname, PORT);
            ImageProxyResource.setServerBaseURI(serverURI);

            JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config, SSLContext.getDefault());

            Log.info(String.format("%s Proxy Server ready @ %s\n",  SERVICE, serverURI));

            Discovery d = new Discovery(Discovery.DISCOVERY_ADDR, SERVICE, serverURI);
            JavaImageProxy.setDiscovery(d);
            d.start();

            JavaImageProxy.resetState = Boolean.parseBoolean(args[0]);
            JavaImageProxy.ALBUM_NAME = ALBUM + hostname;
            //More code can be executed here...
        } catch( Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
