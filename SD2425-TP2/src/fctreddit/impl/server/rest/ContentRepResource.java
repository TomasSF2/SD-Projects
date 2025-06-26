package fctreddit.impl.server.rest;

import fctreddit.api.Post;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.api.rest.RestContent;
import fctreddit.impl.kafka.KafkaPublisher;
import fctreddit.impl.kafka.replication.ReplicationEvent;
import fctreddit.impl.kafka.utils.SyncPoint;
import fctreddit.impl.server.java.JavaContentRep;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import com.google.gson.Gson;

public class ContentRepResource extends RestResource implements RestContent {

    public static Logger Log = Logger.getLogger(ContentResource.class.getName());

    public static final String REPLICATION_TOPIC = "replication-events";
    public static final String REPLICATIONPUBHOST = "kafka:9092";

    private final Content impl;
    private final KafkaPublisher publisher;
    private final SyncPoint syncPoint;
    private final Gson gson = new Gson();

    public ContentRepResource() {
        this.impl = new JavaContentRep();
        this.publisher = KafkaPublisher.createPublisher(REPLICATIONPUBHOST);
        this.syncPoint = SyncPoint.getSyncPoint();
    }

    private Object handleReplication(String op, String key, String value) {

        ReplicationEvent event = new ReplicationEvent(0, op, key, value);

        long offset = publisher.publish(REPLICATION_TOPIC, key, event.toString());

        Result result = syncPoint.waitForResult(offset);

        if (!result.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(result.error()));
        }

        return result.value();
    }

    @Override
    public String createPost(Post post, String userPassword) {
        syncPoint.waitForClientVersion();
        post.setPostId(UUID.randomUUID().toString());
        post.setCreationTimestamp(System.currentTimeMillis());
        String value = gson.toJson(post) + "#" + userPassword;
        return (String) handleReplication("CREATE", "/posts", value);
    }

    @Override
    public List<String> getPosts(long timestamp, String sortOrder) {
        syncPoint.waitForClientVersion();
        Result<List<String>> res = impl.getPosts(timestamp, sortOrder);
        if (res.isOK()) return res.value();
        throw new WebApplicationException(errorCodeToStatus(res.error()));
    }

    @Override
    public Post getPost(String postId) {
        syncPoint.waitForClientVersion();
        Result<Post> res = impl.getPost(postId);
        if (res.isOK()) return res.value();
        throw new WebApplicationException(errorCodeToStatus(res.error()));
    }

    @Override
    public List<String> getPostAnswers(String postId, long timeout) {
        syncPoint.waitForClientVersion();
        Result<List<String>> res = impl.getPostAnswers(postId, timeout);
        if (res.isOK()) return res.value();
        throw new WebApplicationException(errorCodeToStatus(res.error()));
    }

    @Override
    public Post updatePost(String postId, String userPassword, Post post) {
        syncPoint.waitForClientVersion();
        String value = postId + "#" + userPassword + "#" + gson.toJson(post);
        return (Post) handleReplication("UPDATE", "/posts/" + postId, value);
    }

    @Override
    public void deletePost(String postId, String userPassword) {
        syncPoint.waitForClientVersion();
        String value = postId + "#" + userPassword;
        handleReplication("DELETE", "/posts/" + postId, value);
    }

    @Override
    public void upVotePost(String postId, String userId, String userPassword) {
        syncPoint.waitForClientVersion();
        String value = postId + "#" + userId + "#" + userPassword;
        handleReplication("UPVOTE", "/posts/" + postId + "/upvote", value);
    }

    @Override
    public void removeUpVotePost(String postId, String userId, String userPassword) {
        syncPoint.waitForClientVersion();
        String value = postId + "#" + userId + "#" + userPassword;
        handleReplication("REMOVE_UPVOTE", "/posts/" + postId + "/upvote/remove", value);
    }

    @Override
    public void downVotePost(String postId, String userId, String userPassword) {
        syncPoint.waitForClientVersion();
        String value = postId + "#" + userId + "#" + userPassword;
        handleReplication("DOWNVOTE", "/posts/" + postId + "/downvote", value);
    }

    @Override
    public void removeDownVotePost(String postId, String userId, String userPassword) {
        syncPoint.waitForClientVersion();
        String value = postId + "#" + userId + "#" + userPassword;
        handleReplication("REMOVE_DOWNVOTE", "/posts/" + postId + "/downvote/remove", value);
    }

    @Override
    public Integer getupVotes(String postId) {
        syncPoint.waitForClientVersion();
        Result<Integer> res = impl.getupVotes(postId);
        if (res.isOK()) return res.value();
        throw new WebApplicationException(errorCodeToStatus(res.error()));
    }

    @Override
    public Integer getDownVotes(String postId) {
        syncPoint.waitForClientVersion();
        Result<Integer> res = impl.getDownVotes(postId);
        if (res.isOK()) return res.value();
        throw new WebApplicationException(errorCodeToStatus(res.error()));
    }

    @Override
    public void removeTracesOfUser(String secret, String userId) {
        syncPoint.waitForClientVersion();
        impl.removeTracesOfUser(secret, userId);
    }
}
