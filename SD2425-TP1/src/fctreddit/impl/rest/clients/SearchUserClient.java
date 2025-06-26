package fctreddit.impl.rest.clients;


import fctreddit.impl.rest.clients.rest.RestUsersClient;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class SearchUserClient {

    private static Logger Log = Logger.getLogger(GetUserClient.class.getName());

    public static void main(String[] args) throws IOException {

        if( args.length != 2) {
            System.err.println( "Use: java " + CreateUserClient.class.getCanonicalName() + " url query");
            return;
        }

        String serverUrl = args[0];
        String query = args[1];

        System.out.println("Sending request to server.");


        var client = new RestUsersClient( URI.create( serverUrl ) );

        var result = client.searchUsers(query);
        if( result.isOK()  )
            Log.info("Found users:" + result.value() );
        else
            Log.info("Search user failed with error: " + result.error());

    }
}
