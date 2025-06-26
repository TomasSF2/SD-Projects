package fctreddit.impl.server.rest;

import fctreddit.api.java.Result;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;

public abstract class RestResource {

	protected static Status errorCodeToStatus( Result.ErrorCode error ) {
    	Status status =  switch( error) {
    	case NOT_FOUND -> Status.NOT_FOUND; 
    	case CONFLICT -> Status.CONFLICT;
    	case FORBIDDEN -> Status.FORBIDDEN;
    	case NOT_IMPLEMENTED -> Status.NOT_IMPLEMENTED;
    	case BAD_REQUEST -> Status.BAD_REQUEST;
    	default -> Status.INTERNAL_SERVER_ERROR;
    	};
    	
    	return status;
    }

	protected void verifyInternalAuthorization(String auth, String expectedSecret) {
		if (auth == null || !auth.equals("Bearer " + expectedSecret)) {
			throw new WebApplicationException(Status.UNAUTHORIZED);
		}
	}

}
