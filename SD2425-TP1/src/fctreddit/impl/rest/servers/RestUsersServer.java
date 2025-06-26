package fctreddit.impl.rest.servers;

import fctreddit.api.java.Users;
import fctreddit.impl.discovery.Discovery;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.hsqldb.rights.User;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.logging.Logger;

public class RestUsersServer {

	private static Logger Log = Logger.getLogger(RestUsersServer.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}
	
	public static final int PORT = 3456;
	private static final String SERVER_URI_FMT = "http://%s:%s/rest";
	
	public static void main(String[] args) {
		try {

		ResourceConfig config = new ResourceConfig();
		config.register(RestUsersResource.class);

		String ip = InetAddress.getLocalHost().getHostAddress();
		String serverURI = String.format(SERVER_URI_FMT, ip, PORT);
		JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config);

		Discovery disc = new Discovery(Discovery.DISCOVERY_ADDR, Users.NAME, serverURI);
		disc.start();

		Log.info(String.format("%s Server ready @ %s\n", Users.NAME, serverURI));

		//More code can be executed here...
		} catch( Exception e) {
			Log.severe(e.getMessage());
		}
	}	
}
