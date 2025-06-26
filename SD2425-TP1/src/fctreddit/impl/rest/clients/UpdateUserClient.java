package fctreddit.impl.rest.clients;


import fctreddit.api.User;
import fctreddit.impl.rest.clients.rest.RestUsersClient;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class UpdateUserClient {
    private static Logger Log = Logger.getLogger(GetUserClient.class.getName());

    public static void main(String[] args) throws IOException {

        if( args.length != 6) {
            System.err.println( "Use: java " + CreateUserClient.class.getCanonicalName() + " url userId oldpwd fullName email password");
            return;
        }

        String serverUrl = args[0];
        String userId = args[1];
        String oldpwd = args[2];
        String fullName = args[3];
        String email = args[4];
        String password = args[5];

        User usr = new User( userId, fullName, email, password);

        System.out.println("Sending request to server.");

        var client = new RestUsersClient( URI.create( serverUrl ) );

        var result = client.updateUser(userId, oldpwd, usr);
        if( result.isOK()  )
            Log.info("Get user:" + result.value() );
        else
            Log.info("Get user failed with error: " + result.error());


    }

}
