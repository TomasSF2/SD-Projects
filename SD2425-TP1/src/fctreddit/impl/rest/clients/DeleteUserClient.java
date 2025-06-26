package fctreddit.impl.rest.clients;


import fctreddit.impl.rest.clients.rest.RestUsersClient;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class DeleteUserClient {

    private static Logger Log = Logger.getLogger(GetUserClient.class.getName());

    public static void main(String[] args) throws IOException {

        if( args.length != 3) {
            System.err.println( "Use: java " + CreateUserClient.class.getCanonicalName() + " url userId password");
            return;
        }

        String serverUrl = args[0];
        String userId = args[1];
        String password = args[2];

        System.out.println("Sending request to server.");

        //TODO: complete this client code - DONE

        var client = new RestUsersClient( URI.create( serverUrl ) );

        var result = client.deleteUser(userId, password);
        if( result.isOK()  )
            Log.info("Deleted user:" + result.value() );
        else
            Log.info("Delete user failed with error: " + result.error());

    }

}
