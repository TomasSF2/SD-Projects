package fctreddit.impl.java.clients;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import fctreddit.api.java.Result;
import fctreddit.impl.discovery.Discovery;

public class ClientFactory<T> {

    private static Logger Log = Logger.getLogger(ClientFactory.class.getName());

    private static final String REST = "/rest";
    private static final String GRPC = "/grpc";

    private final String serviceName;
    private final Function<URI, T> restClientFunc;
    private final Function<URI, T> grpcClientFunc;

    public ClientFactory(String serviceName, Function<URI, T> restClientFunc, Function<URI, T> grpcClientFunc) {
        this.serviceName = serviceName;
        this.restClientFunc = restClientFunc;
        this.grpcClientFunc = grpcClientFunc;
    }

    private T newClient(URI serverURI) {
        String uriStr = serverURI.toString();

          if (uriStr.endsWith(REST)) {
            return restClientFunc.apply(serverURI);
        } else if (uriStr.endsWith(GRPC)) {
            return grpcClientFunc.apply(serverURI);
        } else {
            throw new RuntimeException("Unknown service type: " + serverURI);
        }
    }

    public T get() {
        try {
            Discovery discovery = new Discovery(Discovery.DISCOVERY_ADDR);
            discovery.start();
            URI[] uris = discovery.knownUrisOf(serviceName, 1);

            return newClient(uris[0]);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(Result.ErrorCode.INTERNAL_ERROR.toString());
        }
    }

    public T get(URI uri) {
        return newClient(uri);
    }

    public List<T> all() {
        try {
            Discovery discovery = new Discovery(Discovery.DISCOVERY_ADDR);
            URI[] uris = discovery.knownUrisOf(serviceName, 1);
            return Arrays.stream(uris).map(uri -> newClient(uri)).toList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(Result.ErrorCode.INTERNAL_ERROR.toString());
        }
    }
}
