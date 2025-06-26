package fctreddit.impl.rest.servers;

import fctreddit.api.User;
import fctreddit.api.java.Users;
import fctreddit.api.rest.RestUsers;
import fctreddit.impl.java.servers.JavaUsers;

import java.util.List;
import java.util.logging.Logger;

public class RestUsersResource extends RestResource implements RestUsers {

	private static Logger Log = Logger.getLogger(RestUsersResource.class.getName());

	final Users impl;
	public RestUsersResource() {
		impl = new JavaUsers();
	}


	@Override
	public String createUser(User user) {
		return super.resultOrThrow(impl.createUser(user));
	}

	@Override
	public User getUser(String userId, String password) {
		return super.resultOrThrow(impl.getUser(userId, password));
	}

	@Override
	public User updateUser(String userId, String password, User user) {
		return super.resultOrThrow(impl.updateUser(userId, password, user));
	}

	@Override
	public User deleteUser(String userId, String password) {
		return super.resultOrThrow(impl.deleteUser(userId, password));
	}

	@Override
	public List<User> searchUsers(String pattern) {
		return super.resultOrThrow(impl.searchUsers(pattern));
	}
}
