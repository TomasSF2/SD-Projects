package fctreddit.impl.rest.clients.rest;

import fctreddit.api.java.Users;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import fctreddit.api.java.Result;
import fctreddit.api.User;
import fctreddit.api.rest.RestUsers;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

public class RestUsersClient extends RestClient implements Users {
	private static Logger Log = Logger.getLogger(RestUsersClient.class.getName());

	public RestUsersClient(URI serverURI ) {
        super(serverURI, RestUsers.PATH);
	}

	private Result<String> _createUser(User user) {
		return super.getResult(target.request()
				.accept( MediaType.APPLICATION_JSON)
				.post(Entity.entity(user, MediaType.APPLICATION_JSON)), String.class);
	}
	private Result<User> _getUser(String userId, String password) {
		return super.getResult(target.path( userId )
				.queryParam(RestUsers.PASSWORD, password).request()
				.accept(MediaType.APPLICATION_JSON)
				.get(), User.class);
	}
	private Result<User> _updateUser(String userId, String password, User user) {
		return super.getResult(target.path(userId)
				.queryParam(RestUsers.PASSWORD, password).request()
				.accept( MediaType.APPLICATION_JSON)
				.put(Entity.entity(user, MediaType.APPLICATION_JSON)), User.class);
	}
	private Result<User> _deleteUser(String userId, String password) {
		return super.getResult(target.path( userId )
				.queryParam(RestUsers.PASSWORD, password).request()
				.accept(MediaType.APPLICATION_JSON)
				.delete(), User.class);
	}
	private Result<List<User>> _searchUser(String pattern) {
		return super.getResult(target.path("/").queryParam( RestUsers.QUERY, pattern).request()
				.accept(MediaType.APPLICATION_JSON)
				.get(), new GenericType<List<User>>() {});
	}

	@Override
	public Result<String> createUser(User user) {
		return super.reTry( () -> _createUser(user) );
	}

	@Override
	public Result<User> getUser(String userId, String password) {
		return super.reTry( () -> _getUser(userId, password) );
	}

	@Override
	public Result<User> updateUser(String userId, String password, User user) {
		return super.reTry( () -> _updateUser(userId, password, user) );
	}

	@Override
	public Result<User> deleteUser(String userId, String password) {
		return super.reTry( () -> _deleteUser(userId, password) );
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		return super.reTry( () -> _searchUser(pattern) );
	}

}
