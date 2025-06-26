package fctreddit.impl.rest.servers;

import fctreddit.api.Post;
import fctreddit.impl.api.java.ExtendedContent;
import fctreddit.impl.api.rest.RestExtendedContent;
import fctreddit.impl.java.servers.JavaContent;

import java.util.List;
import java.util.logging.Logger;

public class RestContentResource extends RestResource implements RestExtendedContent {

    private static Logger Log = Logger.getLogger(RestContentResource.class.getName());

    ExtendedContent impl;
    public RestContentResource() {
        impl = new JavaContent();
    }

    @Override
    public String createPost(Post post, String userPassword) {
        return super.resultOrThrow(impl.createPost(post, userPassword));
    }

    @Override
    public List<String> getPosts(long timestamp, String sortOrder) {
        return super.resultOrThrow(impl.getPosts(timestamp, sortOrder));
    }

    @Override
    public Post getPost(String postId) {
        return super.resultOrThrow(impl.getPost(postId));
    }

    @Override
    public List<String> getPostAnswers(String postId, long timeout) {
        return super.resultOrThrow(impl.getPostAnswers(postId, timeout));
    }

    @Override
    public Post updatePost(String postId, String userPassword, Post post) {
        return super.resultOrThrow(impl.updatePost(postId, userPassword, post));
    }

    @Override
    public void deletePost(String postId, String userPassword) {
        super.resultOrThrow(impl.deletePost(postId, userPassword));
    }

    @Override
    public void upVotePost(String postId, String userId, String userPassword) {
        super.resultOrThrow(impl.upVotePost(postId, userId, userPassword));
    }

    @Override
    public void removeUpVotePost(String postId, String userId, String userPassword) {
        super.resultOrThrow(impl.removeUpVotePost(postId, userId, userPassword));
    }

    @Override
    public void downVotePost(String postId, String userId, String userPassword) {
        super.resultOrThrow(impl.downVotePost(postId, userId, userPassword));
    }

    @Override
    public void removeDownVotePost(String postId, String userId, String userPassword) {
        super.resultOrThrow(impl.removeDownVotePost(postId, userId, userPassword));
    }

    @Override
    public Integer getupVotes(String postId) {
        return super.resultOrThrow(impl.getupVotes(postId));
    }

    @Override
    public Integer getDownVotes(String postId) {
        return super.resultOrThrow(impl.getDownVotes(postId));
    }

    @Override
    public void removeAuthorsFromPost(String userId) {
        super.resultOrThrow(impl.removeAuthorsFromPost(userId));
    }

    @Override
    public void removeVotesFromPost(String userId, String userPassword) {
        super.resultOrThrow(impl.removeVotesFromPost(userId, userPassword));
    }
}
