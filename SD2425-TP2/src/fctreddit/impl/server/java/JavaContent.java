package fctreddit.impl.server.java;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import fctreddit.api.Post;
import fctreddit.api.PostVote;
import fctreddit.api.User;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.api.java.Result.ErrorCode;
import fctreddit.api.java.Users;
import fctreddit.api.kafka.RecordProcessor;
import fctreddit.api.rest.RestContent;
import fctreddit.impl.client.UsersClient;
import fctreddit.impl.kafka.KafkaPublisher;
import fctreddit.impl.kafka.KafkaSubscriber;
import fctreddit.impl.kafka.KafkaUtils;
import fctreddit.impl.server.Hibernate;
import fctreddit.impl.server.Hibernate.TX;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class JavaContent extends JavaServer implements Content, RecordProcessor {

	private static Logger Log = Logger.getLogger(JavaContent.class.getName());

	private Hibernate hibernate;

	private static final HashMap<String, String> postLocks = new HashMap<String, String>();

	private static String serverURI;


	private static final String TOPIC_CONTENT_DELETED_IMAGE = "content-deleted-image";
	private static final String TOPIC_IMAGE_REFERENCED = "image-referenced";
	private static final String TOPIC_IMAGE_UNREFERENCED = "image-not-referenced";
	private static final String TOPIC_CONTENT_DELETED_USER = "content-deleted-user";
	private static boolean init = false;

	private static KafkaSubscriber subscriber = null;
	private static KafkaPublisher publisher = null;

	private void initKafka(){
			Log.info("\n\n\t Starting kafka topic creation...\n\n\n");
			KafkaUtils.createTopic(TOPIC_CONTENT_DELETED_IMAGE);
			KafkaUtils.createTopic(TOPIC_IMAGE_REFERENCED);
			KafkaUtils.createTopic(TOPIC_IMAGE_UNREFERENCED);
			KafkaUtils.createTopic(TOPIC_CONTENT_DELETED_USER);
			if(subscriber == null)
				subscriber = KafkaSubscriber.createSubscriber("kafka:9092", List.of(TOPIC_CONTENT_DELETED_IMAGE, TOPIC_CONTENT_DELETED_USER));
			if(publisher == null)
				publisher = KafkaPublisher.createPublisher("kafka:9092");

			subscriber.start(this);
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

	public JavaContent() {
		hibernate = Hibernate.getInstance();

		startAsync();
	}

	public static void setServerURI(String serverURI) {
		if (JavaContent.serverURI == null)
			JavaContent.serverURI = serverURI;
	}

	@Override
	public Result<String> createPost(Post post, String userPassword) throws Exception {
		Log.info("Registering new post: " + post.toString() + " with password: " + userPassword);

		if (post.getAuthorId() == null || post.getAuthorId().isBlank() || post.getContent() == null
				|| post.getContent().isBlank())
			return Result.error(ErrorCode.BAD_REQUEST);

		if (userPassword == null)
			return Result.error(ErrorCode.FORBIDDEN);

		Users uc = getUsersClient();

		Log.info("Using Users client: " + uc.getClass().getCanonicalName());
		Log.info("Users server uri: " + ((UsersClient) uc).getServerURI());
		Result<User> owner = uc.getUser(post.getAuthorId(), userPassword);
		if (!owner.isOK()) {
			Log.info("Could not retrieve user information for: " + post.getAuthorId() + " -> " + owner.toString());
			return Result.error(owner.error());
		}

		TX tx = hibernate.beginTransaction();

		if (post.getParentUrl() != null && !post.getParentUrl().isBlank()) {
			String postID = extractResourceID(post.getParentUrl());
			Log.info("Trying to check if parent post exists: " + postID);
			Post p = hibernate.get(tx, Post.class, postID);
			if (p == null) {
				hibernate.abortTransaction(tx);
				return Result.error(ErrorCode.NOT_FOUND);
			}
		}

		post.setCreationTimestamp(System.currentTimeMillis());
		post.setUpVote(0);
		post.setDownVote(0);

		Log.info("Trying to store post");

		while (true) {
			post.setPostId(UUID.randomUUID().toString());
			try {
				hibernate.persist(tx, post);
				hibernate.commitTransaction(tx);
				if (post.getMediaUrl() != null && !post.getMediaUrl().isBlank()) {
					String imageId = extractResourceID(post.getMediaUrl());
					publisher.publish(TOPIC_IMAGE_REFERENCED, imageId);
				}
			} catch (Exception ex) { // The transaction has failed, which means we have to restart the whole
										// transaction
				Log.info("Failed to commit transaction creating post");
				ex.printStackTrace();

				hibernate.abortTransaction(tx);
				Log.info("Aborting and restarting...");
				tx = hibernate.beginTransaction();
				if (post.getParentUrl() != null && !post.getParentUrl().isBlank()) {
					String postID = extractResourceID(post.getParentUrl());
					Log.info("Trying to check if parent post exists: " + postID);
					Post p = hibernate.get(tx, Post.class, postID);
					if (p == null) {
						hibernate.abortTransaction(tx);
						return Result.error(ErrorCode.NOT_FOUND);
					}
				}
				continue;
			}
			break;
		}

		try {
			// To unlock waiting threads
			synchronized (JavaContent.postLocks) {
				JavaContent.postLocks.put(post.getPostId(), post.getPostId());

				if (post.getParentUrl() != null) {
					String parentId = extractResourceID(post.getParentUrl());
					String lock = JavaContent.postLocks.get(parentId);
					synchronized (lock) {
						lock.notifyAll();
					}
				}
			}
		} catch (Exception e) {
			// Unable to notify due tome strange event.
			Log.info("Ubale to notify potentiallly waiting threads due to: " + e.getMessage());
			e.printStackTrace();
		}

		return Result.ok(post.getPostId());
	}

	@Override
	public Result<List<String>> getPosts(long timestamp, String sortOrder) {
		Log.info("Getting Posts with timestamp=" + timestamp + " sortOrder=" + sortOrder);

		String baseSQLStatement = null;

		if (sortOrder != null && !sortOrder.isBlank()) {
			if (sortOrder.equalsIgnoreCase(Content.MOST_UP_VOTES)) {
				baseSQLStatement = "SELECT postId FROM (SELECT p.postId as postId, "
						+ "(SELECT COUNT(*) FROM PostVote pv where p.postId = pv.postId AND pv.upVote='true') as upVotes "
						+ "from Post p WHERE "
						+ (timestamp > 0 ? "p.creationTimestamp >= '" + timestamp + "' AND " : "")
						+ "p.parentURL IS NULL) ORDER BY upVotes DESC, postID ASC";
			} else if (sortOrder.equalsIgnoreCase(Content.MOST_REPLIES)) {
				baseSQLStatement = "SELECT postId FROM (SELECT p.postId as postId, "
						+ "(SELECT COUNT(*) FROM Post p2 where p2.parentUrl = CONCAT('" + JavaContent.serverURI
						+ RestContent.PATH + "/',p.postId)) as replies " + "from Post p WHERE "
						+ (timestamp > 0 ? "p.creationTimestamp >= '" + timestamp + "' AND " : "")
						+ "p.parentURL IS NULL) ORDER BY replies DESC, postID ASC";
			} else {
				Log.info("Invalid sortOrder: '" + sortOrder + "' going for default ordering...");
				baseSQLStatement = "SELECT p.postId from Post p WHERE "
						+ (timestamp > 0 ? "p.creationTimestamp >= '" + timestamp + "' AND " : "")
						+ "p.parentURL IS NULL ORDER BY p.creationTimestamp ASC";
			}
		} else {
			baseSQLStatement = "SELECT p.postId from Post p WHERE "
					+ (timestamp > 0 ? "p.creationTimestamp >= '" + timestamp + "' AND " : "")
					+ "p.parentURL IS NULL ORDER BY p.creationTimestamp ASC";
		}

		try {
			List<String> list = null;
			Log.info("Executing selection of Posts with the following query:\n" + baseSQLStatement);
			list = hibernate.sql(baseSQLStatement, String.class);
			Log.info("Output generated (in this order):");
			for (int i = 0; i < list.size(); i++) {
				Log.info("\t" + list.get(i).toString() + " \ttimestamp: "
						+ hibernate.get(Post.class, list.get(i)).getCreationTimestamp() + " \tReplies: "
						+ this.getPostAnswers(list.get(i), 0).value().size() + " \tUpvotes: "
						+ this.getupVotes(list.get(i)).value());
			}
			return Result.ok(list);
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Result<Post> getPost(String postId) {
		Post p = hibernate.get(Post.class, postId);
		Result<Integer> res = this.getupVotes(postId);
		if (res.isOK())
			p.setUpVote(res.value());
		res = this.getDownVotes(postId);
		if (res.isOK())
			p.setDownVote(res.value());

		if (p != null)
			return Result.ok(p);
		else
			return Result.error(ErrorCode.NOT_FOUND);
	}

	@Override
	public Result<List<String>> getPostAnswers(String postId, long maxTimeout) {
		long startOperation = System.currentTimeMillis();
		Log.info("Getting Answers for Post " + postId + " maxTimeout=" + maxTimeout);

		Post p = hibernate.get(Post.class, postId);
		if (p == null)
			return Result.error(ErrorCode.NOT_FOUND);

		String parentURL = serverURI + RestContent.PATH + "/" + postId;
		List<String> list = null;
		try {
			list = hibernate.sql(
					"SELECT p.postId from Post p WHERE p.parentURL='" + parentURL + "' ORDER BY p.creationTimestamp",
					String.class);
		} catch (Exception e) {
			e.printStackTrace();
			return Result.error(ErrorCode.INTERNAL_ERROR);
		}

		if (maxTimeout > 0) {
			String lock = null;
			synchronized (JavaContent.postLocks) {
				lock = JavaContent.postLocks.get(postId);
			}
			synchronized (lock) {
				long deadline = startOperation + maxTimeout;

				while (System.currentTimeMillis() < deadline) {

					try {
						lock.wait(maxTimeout);
					} catch (InterruptedException e) {
						// Ignore this case...
					}

					List<String> redo = null;
					try {
						redo = hibernate.sql("SELECT p.postId from Post p WHERE p.parentURL='" + parentURL
								+ "' ORDER BY p.creationTimestamp", String.class);
					} catch (Exception e) {
						e.printStackTrace();
						return Result.error(ErrorCode.INTERNAL_ERROR);
					}

					if (redo.size() > list.size()) {
						list = redo;
						break;
					}
				}
			}
		}

		return Result.ok(list);
	}

	@Override
	public Result<Post> updatePost(String postId, String userPassword, Post post) throws Exception {
		TX tx = hibernate.beginTransaction();

		Post p = hibernate.get(tx, Post.class, postId);

		if (p == null) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.NOT_FOUND);
		}

		if (post.getPostId() != null) {
			hibernate.abortTransaction(tx);
			Log.info("Cannot update post" + postId + ", since the postId cannot be updated");
			return Result.error(ErrorCode.BAD_REQUEST);
		}

		if (post.getAuthorId() != null) {
			hibernate.abortTransaction(tx);
			Log.info("Cannot update post" + postId + ", since the authordId cannot be updated");
			return Result.error(ErrorCode.BAD_REQUEST);
		}

		if (userPassword == null) {
			hibernate.abortTransaction(tx);
			Log.info("Cannot update post" + postId + ", since no user password was provided");
			return Result.error(ErrorCode.FORBIDDEN);
		}

		Result<User> u = this.getUsersClient().getUser(p.getAuthorId(), userPassword);
		if (!u.isOK()) {
			hibernate.abortTransaction(tx);
			return Result.error(u.error());
		}

		// Check if there are answers
		String parentURL = serverURI + RestContent.PATH + "/" + postId;
		if (hibernate.sql(tx, "SELECT p.postId from Post p WHERE p.parentURL='" + parentURL + "'", String.class)
				.size() > 0) {
			hibernate.abortTransaction(tx);
			Log.info("Cannot update post" + postId + ", since there is at least one answer.");
			return Result.error(ErrorCode.BAD_REQUEST);
		}

		// Check if there are votes
		List<Integer> resp = hibernate.sql(tx, "SELECT COUNT(*) from PostVote pv WHERE pv.postId='" + postId + "'",
				Integer.class);
		if (resp.iterator().next() > 0) {
			hibernate.abortTransaction(tx);
			Log.info("Cannot update post" + postId + ", since there is at least one upVote.");
			return Result.error(ErrorCode.BAD_REQUEST);
		}

		// We can update finally
		if (post.getContent() != null)
			p.setContent(post.getContent());
		if (post.getMediaUrl() != null){
			String imageIdOld;
			if(p.getMediaUrl() != null && !p.getMediaUrl().isBlank()){
			 	imageIdOld = extractResourceID(p.getMediaUrl());
				publisher.publish(TOPIC_IMAGE_UNREFERENCED, imageIdOld);
			}
			String imageIdNew = extractResourceID(post.getMediaUrl());
			p.setMediaUrl(post.getMediaUrl());
			publisher.publish(TOPIC_IMAGE_REFERENCED, imageIdNew);
			Log.info("Published information about update");
		}

		try {
			hibernate.persist(tx, p);
			hibernate.commitTransaction(tx);
			Log.info("Successfully updated post " + postId + " mediaURL=" + p.getMediaUrl());
		} catch (Exception e) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.BAD_REQUEST);
		}

		return Result.ok(p);
	}

	@Override
	public Result<Void> deletePost(String postId, String userPassword) throws Exception {
		TX tx = hibernate.beginTransaction();

		Post p = hibernate.get(tx, Post.class, postId);

		if (p == null) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.NOT_FOUND);
		}

		if (p.getAuthorId() == null || userPassword == null) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.FORBIDDEN);
		}

		Result<User> u = this.getUsersClient().getUser(p.getAuthorId(), userPassword);
		if (!u.isOK())
			return Result.error(u.error());

		// We can delete... maybe get the entirety of descendants and start from back to
		// start.
		LinkedList<Post> pending = new LinkedList<Post>();
		pending.add(p);
		LinkedList<Post> allElementsToDelete = new LinkedList<Post>();

		while (!pending.isEmpty()) {
			Post current = pending.removeFirst();
			String parentURL = serverURI + RestContent.PATH + "/" + current.getPostId();
			List<String> descendants = hibernate.sql(tx,
					"SELECT p.postId from Post p WHERE p.parentURL='" + parentURL + "' ORDER BY p.creationTimestamp",
					String.class);
			for (String id : descendants)
				pending.addLast(hibernate.get(tx, Post.class, id));
			allElementsToDelete.addFirst(current);
		}

		try {
			for (Post d : allElementsToDelete) {
				int number = hibernate.sql(tx, "DELETE from PostVote pv WHERE pv.postId='" + d.getPostId() + "'");
				Log.info("Deleted " + number + " votes (upVotes + downVotes)");
				hibernate.delete(tx, d);

				if (d.getMediaUrl() != null) {
					String imageId = extractResourceID(d.getMediaUrl());
					publisher.publish(TOPIC_IMAGE_UNREFERENCED, imageId);
				}

				synchronized (JavaContent.postLocks) {
					String s = JavaContent.postLocks.remove(d.getPostId());
					if(s!= null) {
						synchronized (s) {
							s.notifyAll();
						}
					}
				}
			}
//			if (p.getMediaUrl() != null) {
//				Result<Void> res = getImageClient().deleteImage(p.getAuthorId(), imageId, userPassword);
//				if (!res.isOK()) {
//					Log.info("Failed to delete image of post " + postId + " that has id: " + imageId + " and owner "
//							+ p.getAuthorId() + ": " + res.error().toString());
//				}
//			}
			hibernate.commitTransaction(tx);
		} catch (Exception e) {
			e.printStackTrace();
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.INTERNAL_ERROR);
		}

		return Result.ok();
	}

	@Override
	public Result<Void> upVotePost(String postId, String userId, String userPassword) throws Exception {
		Log.info("Executing upVote on " + postId + " with Userid:" + userId + " Password: " + userPassword);

		if (userPassword == null)
			return Result.error(ErrorCode.FORBIDDEN);

		Result<User> u = this.getUsersClient().getUser(userId, userPassword);
		if (!u.isOK())
			return Result.error(u.error());

		Log.info("Retrieved user: " + u.value());

		TX tx = hibernate.beginTransaction();

		Post p = hibernate.get(tx, Post.class, postId);

		if (p == null) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.NOT_FOUND);
		}

		try {
			hibernate.persist(tx, new PostVote(userId, postId, true));
			hibernate.commitTransaction(tx);
		} catch (Exception e) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.CONFLICT);
		}

		return Result.ok();
	}

	@Override
	public Result<Void> removeUpVotePost(String postId, String userId, String userPassword) throws Exception {
		Log.info("Executing removeUpVote on " + postId + " with Userid:" + userId + " Password: " + userPassword);

		if (userPassword == null)
			return Result.error(ErrorCode.FORBIDDEN);

		Result<User> u = this.getUsersClient().getUser(userId, userPassword);
		if (!u.isOK())
			return Result.error(u.error());

		Log.info("Retrieved user: " + u.value());

		TX tx = hibernate.beginTransaction();

		List<PostVote> i = hibernate.sql(tx, "SELECT * from PostVote pv WHERE pv.userId='" + userId
				+ "' AND pv.postId='" + postId + "' AND pv.upVote='true'", PostVote.class);
		if (i.size() == 0) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.NOT_FOUND);
		}

		try {
			hibernate.delete(tx, i.iterator().next());
			hibernate.commitTransaction(tx);
		} catch (Exception e) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.INTERNAL_ERROR);
		}

		return Result.ok();
	}

	@Override
	public Result<Void> downVotePost(String postId, String userId, String userPassword) throws Exception {
		Log.info("Executing downVote on " + postId + " with Userid:" + userId + " Password: " + userPassword);

		if (userPassword == null)
			return Result.error(ErrorCode.FORBIDDEN);

		Result<User> u = this.getUsersClient().getUser(userId, userPassword);
		if (!u.isOK())
			return Result.error(u.error());

		Log.info("Retrieved user: " + u.value());

		TX tx = hibernate.beginTransaction();

		Post p = hibernate.get(tx, Post.class, postId);

		if (p == null) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.NOT_FOUND);
		}

		try {
			hibernate.persist(tx, new PostVote(userId, postId, false));
			hibernate.commitTransaction(tx);
		} catch (Exception e) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.CONFLICT);
		}

		return Result.ok();
	}

	@Override
	public Result<Void> removeDownVotePost(String postId, String userId, String userPassword) throws Exception {
		Log.info("Executing removeDownVote on " + postId + " with Userid:" + userId + " Password: " + userPassword);

		if (userPassword == null)
			return Result.error(ErrorCode.FORBIDDEN);

		Result<User> u = this.getUsersClient().getUser(userId, userPassword);
		if (!u.isOK())
			return Result.error(u.error());

		Log.info("Retrieved user: " + u.value());

		TX tx = hibernate.beginTransaction();

		List<PostVote> i = hibernate.sql(tx, "SELECT * from PostVote pv WHERE pv.userId='" + userId
				+ "' AND pv.postId='" + postId + "' AND pv.upVote='false'", PostVote.class);
		if (i.size() == 0) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.NOT_FOUND);
		}

		try {
			hibernate.delete(tx, i.iterator().next());
			hibernate.commitTransaction(tx);
		} catch (Exception e) {
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.INTERNAL_ERROR);
		}

		return Result.ok();
	}

	@Override
	public Result<Integer> getupVotes(String postId) {
		Log.info("Executing getUpVotes on " + postId);
		Post p = hibernate.get(Post.class, postId);
		if (p == null)
			return Result.error(ErrorCode.NOT_FOUND);

		List<Integer> count = hibernate.sql(
				"SELECT COUNT(*) from PostVote pv WHERE pv.postId='" + postId + "'  AND pv.upVote='true'",
				Integer.class);
		return Result.ok(count.iterator().next());

	}

	@Override
	public Result<Integer> getDownVotes(String postId) {
		Log.info("Executing getDownVotes on " + postId);
		Post p = hibernate.get(Post.class, postId);
		if (p == null)
			return Result.error(ErrorCode.NOT_FOUND);

		List<Integer> count = hibernate.sql(
				"SELECT COUNT(*) from PostVote pv WHERE pv.postId='" + postId + "' AND pv.upVote='false'",
				Integer.class);
		return Result.ok(count.iterator().next());
	}

	@Override
	public Result<Void> removeTracesOfUser(String secret, String userId) {
		Log.info("Executing a removeTracesOfUser on " + userId);
		TX tx = null;
		try {
			tx = hibernate.beginTransaction();

			hibernate.sql(tx, "DELETE from PostVote pv where pv.userId='" + userId + "'");

			hibernate.sql(tx, "UPDATE Post p SET p.authorId=NULL where p.authorId='" + userId + "'");

			hibernate.commitTransaction(tx);

		} catch (Exception e) {
			e.printStackTrace();
			hibernate.abortTransaction(tx);
			return Result.error(ErrorCode.INTERNAL_ERROR);
		}

		return Result.ok();
	}

	private void mediaUrlImageDeleted(String imageUrl){
		Log.info("\nSETTING IMAGE URL TO NULL");
		TX tx = hibernate.beginTransaction();

		List<Post> posts = List.of();
		List<String> ids = List.of();

		try {
			posts = hibernate.jpql("SELECT p from Post p WHERE p.mediaUrl='" + imageUrl + "'",
					Post.class);

			if(posts.isEmpty())
				hibernate.abortTransaction(tx);

			for(Post p : posts){
				p.setMediaUrl(null);

				hibernate.update(tx, p);

			}

			hibernate.commitTransaction(tx);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}

	}
	@Override
	public void onReceive(ConsumerRecord<String, String> r) {
		System.out.println("\n\n\t" + r.topic() + " , " +  r.offset() + " -> " + r.value() + "\n\n\t");

		if(r.topic().equals(TOPIC_CONTENT_DELETED_IMAGE)){
			mediaUrlImageDeleted(r.value());
		}
		if(r.topic().equals(TOPIC_CONTENT_DELETED_USER)){
			removeTracesOfUser("", r.value());
		}

	}
}
