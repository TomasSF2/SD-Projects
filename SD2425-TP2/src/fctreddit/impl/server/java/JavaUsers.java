package fctreddit.impl.server.java;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import fctreddit.api.User;
import fctreddit.api.java.Result;
import fctreddit.api.java.Result.ErrorCode;
import fctreddit.api.java.Users;
import fctreddit.api.kafka.RecordProcessor;
import fctreddit.impl.kafka.KafkaPublisher;
import fctreddit.impl.kafka.KafkaSubscriber;
import fctreddit.impl.kafka.KafkaUtils;
import fctreddit.impl.server.Hibernate;
import fctreddit.impl.server.Hibernate.TX;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class JavaUsers extends JavaServer implements Users {

	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());

	private Hibernate hibernate;

	private static String SECRET;

	private static final String TOPIC_IMAGE_REFERENCED = "image-referenced";
	private static final String TOPIC_IMAGE_DELETED_USER = "image-deleted-user";
	private static final String TOPIC_CONTENT_DELETED_USER = "content-deleted-user";
	private static volatile boolean init = false;

	private static KafkaPublisher publisher = null;

	private void initKafka(){
		Log.info("\n\n\t Starting kafka topic creation...\n\n\n");
		KafkaUtils.createTopic(TOPIC_IMAGE_REFERENCED);
		KafkaUtils.createTopic(TOPIC_IMAGE_DELETED_USER);
		KafkaUtils.createTopic(TOPIC_CONTENT_DELETED_USER);

		if(publisher == null)
			publisher = KafkaPublisher.createPublisher("kafka:9092");
	}

	public void startAsync(){
		if (!init) {
			synchronized (JavaImage.class) {
				if (!init) { // double-checked locking
					initKafka();
					init = true;
				}
			}
		}
	}

	public JavaUsers(String secret) {
		hibernate = Hibernate.getInstance();
		SECRET = secret;
		startAsync();
	}

	@Override
	public Result<String> createUser(User user) {
		Log.info("createUser : " + user);

		// Check if user data is valid
		if (user.getUserId() == null || user.getUserId().isBlank() || user.getPassword() == null
				|| user.getPassword().isBlank() || user.getFullName() == null || user.getFullName().isBlank()
				|| user.getEmail() == null || user.getEmail().isBlank()) {
			Log.info("User object invalid.");
			return Result.error(ErrorCode.BAD_REQUEST);
		}

		try {
			hibernate.persist(user);
		} catch (Exception e) {
			e.printStackTrace(); // Most likely the exception is due to the user already existing...
			Log.info("User already exists.");
			return Result.error(ErrorCode.CONFLICT);
		}

		Log.info("Successfully created user: " + user.getUserId());
		return Result.ok(user.getUserId());
	}

	@Override
	public Result<User> getUser(String userId, String password) {
		Log.info("getUser : user = " + userId + "; pwd = " + password);

		// Check if user is valid
		if (userId == null) {
			Log.info("UserId null.");
			return Result.error(ErrorCode.BAD_REQUEST);
		}

		if (password == null) {
			Log.info("Getting user " + userId + ": Password null.");
			return Result.error(ErrorCode.FORBIDDEN);
		}

		User user = null;
		try {
			user = hibernate.get(User.class, userId);
		} catch (Exception e) {
			e.printStackTrace();
			return Result.error(ErrorCode.INTERNAL_ERROR);
		}

		// Check if user exists
		if (user == null) {
			Log.info("User does not exist.");
			return Result.error(ErrorCode.NOT_FOUND);
		}

		// Check if the password is correct
		if (!user.getPassword().equals(password)) {
			Log.info("Getting user " + userId + ":Password is incorrect. (got " + password + " shoud be " + user.getPassword());
			return Result.error(ErrorCode.FORBIDDEN);
		}

		return Result.ok(user);
	}

	@Override
	public Result<User> updateUser(String userId, String password, User user) {
		Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; userData = " + user);

		TX tx = hibernate.beginTransaction();

		User u = hibernate.get(tx, User.class, userId);

		if (u == null) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.NOT_FOUND);
		}

		if (!u.getPassword().equals(password)) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.FORBIDDEN);
		}

		if (user.getFullName() != null)
			u.setFullName(user.getFullName());
		if (user.getPassword() != null)
			u.setPassword(user.getPassword());
		if (user.getEmail() != null)
			u.setEmail(user.getEmail());
		if (user.getAvatarUrl() != null)
			u.setAvatarUrl(user.getAvatarUrl());

		try {
			hibernate.update(tx, u);
			hibernate.commitTransaction(tx);

			if(user.getAvatarUrl() != null) {
				publisher.publish(TOPIC_IMAGE_REFERENCED, extractResourceID(user.getAvatarUrl()));
			}
		} catch (Exception e) {
			e.printStackTrace();
			hibernate.abortTransaction(tx);
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		}

		return Result.ok(u);
	}

	@Override
	public Result<User> deleteUser(String userId, String password) {
		Log.info("deleteUser : user = " + userId + "; pwd = " + password);

		TX tx = hibernate.beginTransaction();

		User u = hibernate.get(tx, User.class, userId);

		if (u == null) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.NOT_FOUND);
		}

		if (!u.getPassword().equals(password)) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.FORBIDDEN);
		}

		try {
			if (u.getAvatarUrl() != null && !u.getAvatarUrl().isBlank()){
//				this.getImageClient().deleteImage(userId, extractResourceID(u.getAvatarUrl()), password);
				publisher.publish(TOPIC_IMAGE_DELETED_USER, extractResourceID(u.getAvatarUrl()));
			}



//			Result<Void> purgeUserinfo = this.getContentClient().removeTracesOfUser(SECRET, userId);
//			if(purgeUserinfo.isOK()) {
//				hibernate.delete(u);
//				hibernate.commitTransaction(tx);

			long offset = publisher.publish(TOPIC_CONTENT_DELETED_USER, userId);
			if(offset >= 0) {
				hibernate.delete(u);
				hibernate.commitTransaction(tx);
			} else {
				Log.info("Failed to execute the purgeUserInfo operation on Content");
				hibernate.abortTransaction(tx);
				return Result.error(Result.ErrorCode.INTERNAL_ERROR);
			}
		
		} catch (Exception e) {
			e.printStackTrace();
			hibernate.abortTransaction(tx);
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		}

		return Result.ok(u);
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		Log.info("searchUsers : pattern = " + pattern);

		try {
			List<User> list = null;
			if (pattern != null && !pattern.isBlank())
				list = hibernate.sql("SELECT * FROM User u WHERE u.userId LIKE '%" + pattern + "%'", User.class);
			else
				list = hibernate.sql("SELECT * from User u", User.class);
			for (User u : list)
				u.setPassword("");
			return Result.ok(list);
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		}
	}
}
