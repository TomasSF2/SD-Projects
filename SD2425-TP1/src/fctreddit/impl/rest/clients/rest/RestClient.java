package fctreddit.impl.rest.clients.rest;


import fctreddit.api.java.Result;
import fctreddit.api.java.Result.ErrorCode;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import utils.Sleep;

import java.net.URI;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static fctreddit.api.java.Result.error;
import static fctreddit.api.java.Result.ok;


public class RestClient {
	private static Logger Log = Logger.getLogger(RestClient.class.getName());

	protected static final int READ_TIMEOUT = 10000;
	protected static final int CONNECT_TIMEOUT = 10000;

	protected static final int MAX_RETRIES = 3;
	protected static final int RETRY_SLEEP = 1000;

	final Client client;
	final URI serverURI;
	final ClientConfig config;
	final WebTarget target;

	protected RestClient(URI serverURI, String servicePath ) {
		this.serverURI = serverURI;

		this.config = new ClientConfig();
		config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
		config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);

		this.client = ClientBuilder.newClient(config);
		this.target = client.target( serverURI ).path( servicePath );
	}

	protected <T> Result<T> reTry(Supplier<Result<T>> func) {
		for (int i = 0; i < MAX_RETRIES; i++)
			try {
				return func.get();
			} catch (ProcessingException x) {
				Log.fine("Timeout: " + x.getMessage());
				Sleep.ms(RETRY_SLEEP);
			} catch (Exception x) {
				x.printStackTrace();
				return error(Result.ErrorCode.INTERNAL_ERROR);
			}

		System.err.println("TIMEOUT...");
		return error(Result.ErrorCode.TIMEOUT);
	}

	protected Result<Void> getResult(Response response) {
		return handleResponse(response, null, null);
	}

	protected <T> Result<T> getResult(Response response, Class<T> type) {
		return handleResponse(response, type, null);
	}

	protected <T> Result<T> getResult(Response response, GenericType<T> type) {
		return handleResponse(response, null, type);
	}

	private <T> Result<T> handleResponse(Response response, Class<T> classType, GenericType<T> genericType) {
		try {
			Status status = response.getStatusInfo().toEnum();
			if (status == Status.OK && response.hasEntity()) {
				T entity = classType != null ? response.readEntity(classType)
						: (genericType != null ? response.readEntity(genericType) : null);
				return ok(entity);
			} else if (status == Status.NO_CONTENT) {
				return ok();
			}
			return error(getErrorCodeFrom(status.getStatusCode()));
		} finally {
			response.close();
		}
	}
	
	public static ErrorCode getErrorCodeFrom(int status) {
		return switch (status) {
		case 200, 204 -> ErrorCode.OK;
		case 409 -> ErrorCode.CONFLICT;
		case 403 -> ErrorCode.FORBIDDEN;
		case 404 -> ErrorCode.NOT_FOUND;
		case 400 -> ErrorCode.BAD_REQUEST;
		case 500 -> ErrorCode.INTERNAL_ERROR;
		case 501 -> ErrorCode.NOT_IMPLEMENTED;
		default -> ErrorCode.INTERNAL_ERROR;
		};
	}

}
