package fctreddit.impl.rest.clients;


import fctreddit.impl.rest.clients.rest.RestUsersClient;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class GetUserClient {
	
	private static Logger Log = Logger.getLogger(GetUserClient.class.getName());


	public static void main(String[] args) throws IOException {
		
		if( args.length != 3) {
			System.err.println( "Use: java " + CreateUserClient.class.getCanonicalName() + " url userId password");
			return;
		}
		
		String serverUrl = args[0];
		String userId = args[1];
		String password = args[2];
		
		var client = new RestUsersClient( URI.create( serverUrl ) );
			
		var result = client.getUser(userId, password);
		if( result.isOK()  )
			Log.info("Get user:" + result.value() );
		else
			Log.info("Get user failed with error: " + result.error());
		
	}
	
}
