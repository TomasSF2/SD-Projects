package fctreddit.impl.server.rest;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import fctreddit.impl.kafka.utils.VersionFilter;
import fctreddit.impl.server.java.JavaContentRep;
import org.apache.kafka.common.errors.TopicExistsException;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import fctreddit.impl.kafka.replication.ContentReplicationProcessor;
import fctreddit.impl.kafka.KafkaSubscriber;
import fctreddit.impl.server.Discovery;

import javax.net.ssl.SSLContext;

public class ContentRepServer {

    private static Logger Log = Logger.getLogger(ContentServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;
    public static final String SERVICE = "Content";
    private static final String SERVER_URI_FMT = "https://%s:%s/rest";

    public static void main(String[] args) {

        try {
            // Iniciar subscrição Kafka para replicação
            KafkaSubscriber subscriber = KafkaSubscriber.createSubscriber(ContentRepResource.REPLICATIONPUBHOST, List.of(ContentRepResource.REPLICATION_TOPIC));
            subscriber.start(new ContentReplicationProcessor());

            // Servidor REST
            ResourceConfig config = new ResourceConfig();
            config.register(ContentRepResource.class);
            config.register(VersionFilter.class);

            String hostname = InetAddress.getLocalHost().getHostName();
            String serverURI = String.format(SERVER_URI_FMT, hostname, PORT);
            JavaContentRep.setServerURI(serverURI);

            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config, SSLContext.getDefault());

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

            Discovery d = new Discovery(Discovery.DISCOVERY_ADDR, SERVICE, serverURI);
            JavaContentRep.setDiscovery(d);
            d.start();

        } catch (Exception e) {
            Log.info(e.getMessage());
        }
    }
}