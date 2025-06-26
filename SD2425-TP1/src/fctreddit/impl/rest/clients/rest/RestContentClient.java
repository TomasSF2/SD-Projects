package fctreddit.impl.rest.clients.rest;

import fctreddit.api.Post;
import fctreddit.api.java.Result;
import fctreddit.api.rest.RestContent;
import fctreddit.impl.api.java.ExtendedContent;
import fctreddit.impl.api.rest.RestExtendedContent;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

public class RestContentClient extends RestClient implements ExtendedContent {

    private static Logger Log = Logger.getLogger(RestContentClient.class.getName());

    public RestContentClient(URI serverURI) {
        super(serverURI, RestContent.PATH);
    }

    public Result<String> _createPost(Post post, String userPassword) {
        return super.getResult(target
                .queryParam(RestContent.PASSWORD, userPassword)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(post, MediaType.APPLICATION_JSON)), String.class
        );
    }

    public Result<List<String>> _getPosts(long timestamp, String sortOrder) {
        return super.getResult(target
                .queryParam(RestContent.TIMESTAMP, timestamp)
                .queryParam(RestContent.SORTBY, sortOrder)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get(), new GenericType<List<String>>() {}
        );
    }

    public Result<Post> _getPost(String postId) {
        return super.getResult(target
                .path(postId)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get(), Post.class
        );
    }

    public Result<List<String>> _getPostAnswers(String postId, long maxTimeout) {
        return super.getResult(target
                .path(postId)
                .path(RestContent.REPLIES)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get(), new GenericType<List<String>>() {}
        );
    }

    public Result<Post> _updatePost(String postId, String userPassword, Post post) {
        return super.getResult(target
                .path(postId)
                .queryParam(RestContent.PASSWORD, userPassword)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .put(Entity.entity(post, MediaType.APPLICATION_JSON)), Post.class
        );
    }

    public Result<Void> _deletePost(String postId, String userPassword) {
        return super.getResult(target
                .path(postId)
                .queryParam(RestContent.PASSWORD, userPassword)
                .request()
                .delete()
        );
    }

    public Result<Void> _upVotePost(String postId, String userId, String userPassword) {
        return super.getResult(target
                .path(postId)
                .path(RestContent.UPVOTE)
                .path(userId)
                .queryParam(RestContent.PASSWORD, userPassword)
                .request()
                .post(Entity.json(null))
        );
    }

    public Result<Void> _removeUpVotePost(String postId, String userId, String userPassword) {
        return super.getResult(target
                .path(postId)
                .path(RestContent.UPVOTE)
                .path(userId)
                .queryParam(RestContent.PASSWORD, userPassword)
                .request()
                .delete()
        );
    }

    public Result<Void> _downVotePost(String postId, String userId, String userPassword) {
        return super.getResult(target
                .path(postId)
                .path(RestContent.DOWNVOTE)
                .path(userId)
                .queryParam(RestContent.PASSWORD, userPassword)
                .request()
                .post(Entity.json(null))
        );
    }

    public Result<Void> _removeDownVotePost(String postId, String userId, String userPassword) {
        return super.getResult(target
                .path(postId)
                .path(RestContent.DOWNVOTE)
                .path(userId)
                .queryParam(RestContent.PASSWORD, userPassword)
                .request()
                .delete()
        );
    }

    public Result<Integer> _getupVotes(String postId) {
        return super.getResult(target
                .path(postId)
                .path(RestContent.UPVOTE)
                .request()
                .get(), Integer.class
        );
    }

    public Result<Integer> _getDownVotes(String postId) {
        return super.getResult(target
                .path(postId)
                .path(RestContent.DOWNVOTE)
                .request()
                .get(), Integer.class
        );
    }

    public Result<Void> _removeAuthorsFromPost(String userId) {
        return super.getResult(target
                .path(userId)
                .path(RestExtendedContent.ALL)
                .request()
                .delete()
        );
    }

    public Result<Void> _removeVotesFromPost(String userId, String userPassword) {
        return super.getResult(target
                .path(userId)
                .path(RestExtendedContent.VOTES)
                .path(RestExtendedContent.ALL)
                .queryParam(RestContent.PASSWORD, userPassword)
                .request()
                .delete()
        );
    }

    @Override
    public Result<String> createPost(Post post, String userPassword) {
        return super.reTry( () -> _createPost(post, userPassword));
    }

    @Override
    public Result<List<String>> getPosts(long timestamp, String sortOrder) {
        return super.reTry( () -> _getPosts(timestamp, sortOrder));
    }

    @Override
    public Result<Post> getPost(String postId) {
        return super.reTry( () -> _getPost(postId));
    }

    @Override
    public Result<List<String>> getPostAnswers(String postId, long maxTimeout) {
        return super.reTry( () -> _getPostAnswers(postId, maxTimeout));
    }

    @Override
    public Result<Post> updatePost(String postId, String userPassword, Post post) {
        return super.reTry( () -> _updatePost(postId, userPassword, post));
    }

    @Override
    public Result<Void> deletePost(String postId, String userPassword) {
        return super.reTry( () -> _deletePost(postId, userPassword));
    }

    @Override
    public Result<Void> upVotePost(String postId, String userId, String userPassword) {
        return super.reTry( () -> _upVotePost(postId, userPassword, postId));
    }

    @Override
    public Result<Void> removeUpVotePost(String postId, String userId, String userPassword) {
        return super.reTry( () -> _removeUpVotePost(postId, userPassword, postId));
    }

    @Override
    public Result<Void> downVotePost(String postId, String userId, String userPassword) {
        return super.reTry( () -> _downVotePost(postId, userPassword, postId));
    }

    @Override
    public Result<Void> removeDownVotePost(String postId, String userId, String userPassword) {
        return super.reTry( () -> _removeDownVotePost(postId, userPassword, postId));
    }

    @Override
    public Result<Integer> getupVotes(String postId) {
        return super.reTry( () -> _getupVotes(postId));
    }

    @Override
    public Result<Integer> getDownVotes(String postId) {
        return super.reTry( () -> _getDownVotes(postId));
    }

    @Override
    public Result<Void> removeAuthorsFromPost(String userId) {
        return super.reTry(() -> _removeAuthorsFromPost(userId));
    }

    @Override
    public Result<Void> removeVotesFromPost(String userId, String userPassword) {
        return super.reTry(() -> _removeVotesFromPost(userId, userPassword));
    }
}
