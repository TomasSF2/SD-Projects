package fctreddit.impl.java.clients;

import fctreddit.api.java.Content;
import fctreddit.api.java.Image;
import fctreddit.api.java.Users;
import fctreddit.impl.api.java.ExtendedContent;
import fctreddit.impl.rest.clients.rest.RestContentClient;
import fctreddit.impl.rest.clients.rest.RestUsersClient;
import fctreddit.impl.rest.clients.rest.RestImageClient;
import fctreddit.impl.grpc.clients.GrpcUsersClient;
import fctreddit.impl.grpc.clients.GrpcImageClient;
import fctreddit.impl.grpc.clients.GrpcContentClient;

public class Clients {

    public static final ClientFactory<Users> UsersClients = new ClientFactory<>(Users.NAME, RestUsersClient::new, GrpcUsersClient::new);
    public static final ClientFactory<Image> ImageClients = new ClientFactory<>(Image.NAME, RestImageClient::new, GrpcImageClient::new);
    public static final ClientFactory<ExtendedContent> ContentClients = new ClientFactory<>(Content.NAME, RestContentClient::new, GrpcContentClient::new);


}
