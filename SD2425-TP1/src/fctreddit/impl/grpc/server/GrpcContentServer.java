package fctreddit.impl.grpc.server;

import fctreddit.api.java.Content;
import fctreddit.api.java.Users;
import fctreddit.impl.discovery.Discovery;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerCredentials;

import java.net.InetAddress;
import java.util.logging.Logger;

public class GrpcContentServer {
    public static final int PORT = 15678;

    private static final String GRPC_CTX = "/grpc";
    private static final String SERVER_BASE_URI = "grpc://%s:%s%s";

    private static Logger Log = Logger.getLogger(GrpcUsersServer.class.getName());

    public static void main(String[] args) throws Exception {

        GrpcContentServerStub stub = new GrpcContentServerStub();
        ServerCredentials cred = InsecureServerCredentials.create();
        Server server = Grpc.newServerBuilderForPort(PORT, cred) .addService(stub).build();
        String serverURI = String.format(SERVER_BASE_URI, InetAddress.getLocalHost().getHostAddress(), PORT, GRPC_CTX);

        Discovery disc = new Discovery(Discovery.DISCOVERY_ADDR, Content.NAME, serverURI);
        disc.start();

        Log.info(String.format("Content gRPC Server ready @ %s\n", serverURI));
        server.start().awaitTermination();
    }
}
