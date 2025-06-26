package fctreddit.impl.java.servers;

import fctreddit.api.Post;
import fctreddit.api.User;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.impl.api.java.ExtendedContent;
import fctreddit.impl.java.servers.Data.Vote;
import utils.Hibernate;

import static fctreddit.impl.java.clients.Clients.ImageClients;
import static fctreddit.impl.java.clients.Clients.UsersClients;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class JavaContent implements ExtendedContent {
    private static final boolean UPVOTE = true;
    private static final boolean DOWNVOTE = false;
    private static final boolean ADD_VOTE = false;
    private static final boolean REMOVE_VOTE = true;

    private static Logger Log = Logger.getLogger(JavaContent.class.getName());
    private Hibernate hibernate = Hibernate.getInstance();

    private static final Map<String, Object> postLocks = new ConcurrentHashMap<>();

//    private static final Users usersClients = UsersClients.get();
//    private static final Image imageClients = ImageClients.get();

    private static Users usersClients;
    private static Image imageClients;

    private static Users getUsersClients() {
        if (usersClients == null) {
            usersClients = UsersClients.get();
        }
        return usersClients;
    }

    private static Image getImageClients() {
        if (imageClients == null) {
            imageClients = ImageClients.get();
        }
        return imageClients;
    }

    @Override
    public Result<String> createPost(Post post, String userPassword) {
        Log.info("createPost : post = " + post);

        var res = validPost(post, userPassword);
        if (!res.isOK())
            return Result.error(res.error());

        post.newPost();
        try {
            hibernate.persist(post);
            Log.info("Post " + post.getPostId() + " created.\n");

            if (post.getParentId() != null && !postLocks.isEmpty()) {
                Object lock = postLocks.get(post.getParentId());
                if (lock != null) {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            }

            return Result.ok(post.getPostId());
        } catch (Exception e) {
            e.printStackTrace();
            Log.info("Failed creating post: " + e.getMessage());
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
    }

    //ORDER BY NOT YET TESTED, TIMESTAMPS ARE OK
    @Override
    public Result<List<String>> getPosts(long timestamp, String sortOrder) {
        Log.info("getPosts : timestamp = " + timestamp + ", sortOrder = " + sortOrder);

        StringBuilder query = new StringBuilder("SELECT p.postId FROM Post p ");

        if (definedSortOrder(sortOrder) && sortOrder.equals(MOST_REPLIES)) {
            query.append("LEFT JOIN Post r ON r.parentId = p.postId ");
        }

        query.append("WHERE p.parentUrl IS NULL ");

        if (definedTimestamp(timestamp))
            query.append(" AND p.creationTimestamp >= ").append(timestamp).append(" ");

        if (definedSortOrder(sortOrder)) {
            if (sortOrder.equals(MOST_REPLIES))
                query.append("GROUP BY p.postId ORDER BY COUNT(r) DESC, p.postId ASC");
            else if (sortOrder.equals(MOST_UP_VOTES))
                query.append("ORDER BY p.upVote DESC, p.postId ASC");
        } else
            query.append("ORDER BY p.creationTimestamp ASC");

        List<String> list = List.of();
        try {
            list = hibernate.jpql(query.toString(), String.class);
            return Result.ok(list);
        } catch (Exception e) {
            e.printStackTrace();
            Log.info("Query failed: " + query + "\nError: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Post> getPost(String postId) {
        Log.info("getPost : post = " + postId);

        Post post;
        try {
            post = hibernate.get(Post.class, postId);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        // Check if post exists
        if (post == null) {
            Log.info("Post does not exist.");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        return Result.ok(post);
    }

    @Override
    public Result<List<String>> getPostAnswers(String postId, long maxTimeout) {
        Log.info("getPostAnswers : postId = " + postId + ", maxTimeout = " + maxTimeout);

        var res = getPost(postId);
        if (!res.isOK())
            return Result.error(Result.ErrorCode.NOT_FOUND);

        String query = format("SELECT p.postId FROM Post p WHERE p.parentId = '%s' ORDER BY p.creationTimestamp", postId);
        List<String> answers = List.of();

        if(maxTimeout > 0){
            Object lock = postLocks.computeIfAbsent(postId, k -> new Object());
            synchronized (lock) {
                try {
                    lock.wait(maxTimeout);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Result.error(Result.ErrorCode.INTERNAL_ERROR);
                }
            }
        }

        try {
            answers = hibernate.jpql(query, String.class);
            return Result.ok(answers);
        } catch (Exception e) {
            e.printStackTrace();
            Log.info("Query failed: " + query + "\nError: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    //NOT TESTED
    @Override
    public Result<Post> updatePost(String postId, String userPassword, Post post) {
        Log.info("updatePost : postId = " + postId + ", userPassword = " + userPassword);

        var oldPost = getPost(postId);
        if(!oldPost.isOK())
            return Result.error(oldPost.error());

        var user = validUser(oldPost.value().getAuthorId(), userPassword);
        if(!user.isOK())
            return Result.error(user.error());

        String query = format("SELECT p.postId FROM Post p WHERE p.parentId = '%s'", postId);
        if(!hibernate.jpql(query, String.class).isEmpty() || getupVotes(postId).value() > 0 || getDownVotes(postId).value() > 0)
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        oldPost.value().updatePostTo(post);

        try {
            hibernate.update(oldPost.value());
            Log.info("Post " + oldPost.value().getPostId() + " updated.\n");
            return Result.ok(oldPost.value());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
    }

    //NOT TESTED
    @Override
    public Result<Void> deletePost(String postId, String userPassword) {
        Log.info("deletePost : postId = " + postId + ", userPassword = " + userPassword);

        var postRes = getPost(postId);
        if(!postRes.isOK())
            return Result.error(postRes.error());

        Post post = postRes.value();

        var userRes = getUsersClients().getUser(post.getAuthorId(), userPassword);
        if(!userRes.isOK())
            return Result.error(userRes.error());

        User user = userRes.value();

        try {
            var allPostsToDelete = deletePostsAndReplies(post);
            if(!allPostsToDelete.isOK())
                return Result.error(allPostsToDelete.error());

            List<Post> allPostsToDeleteList = allPostsToDelete.value();

            hibernate.delete(allPostsToDeleteList.toArray());

            if(post.getMediaUrl() != null && !post.getMediaUrl().isEmpty()){
                String imageId =  post.getMediaUrl().substring(post.getMediaUrl().lastIndexOf("/") + 1);
                var res = getImageClients().deleteImage(user.getUserId(), imageId, userPassword);
                if(!res.isOK())
                    return Result.error(res.error());
            }

            Log.info("Post " + postId + ", its replies and its media deleted.");
            return Result.ok();

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> upVotePost(String postId, String userId, String userPassword) {
        Log.info("upVotePost : postId = " + postId + ", userId = " + userId);
        var res = votePost(UPVOTE, ADD_VOTE, postId, userId, userPassword);
        if (!res.isOK())
            return Result.error(res.error());
        return Result.ok();
    }

    @Override
    public Result<Void> removeUpVotePost(String postId, String userId, String userPassword) {
        Log.info("removeUpVotePost : postId = " + postId + ", userId = " + userId);
        var res = votePost(UPVOTE, REMOVE_VOTE, postId, userId, userPassword);
        if (!res.isOK())
            return Result.error(res.error());
        return Result.ok();
    }

    @Override
    public Result<Void> downVotePost(String postId, String userId, String userPassword) {
        Log.info("downVotePost : postId = " + postId + ", userId = " + userId);
        var res = votePost(DOWNVOTE, ADD_VOTE, postId, userId, userPassword);
        if (!res.isOK())
            return Result.error(res.error());
        return Result.ok();
    }

    @Override
    public Result<Void> removeDownVotePost(String postId, String userId, String userPassword) {
        Log.info("removeDownVotePost : postId = " + postId + ", userId = " + userId);
        var res = votePost(DOWNVOTE, REMOVE_VOTE, postId, userId, userPassword);
        if (!res.isOK())
            return Result.error(res.error());
        return Result.ok();
    }

    @Override
    public Result<Integer> getupVotes(String postId) {
        Log.info("getupVotes : postId = " + postId);
        return getVotes(UPVOTE, postId);
    }

    @Override
    public Result<Integer> getDownVotes(String postId) {
        Log.info("getDownVotes : postId = " + postId);
        return getVotes(DOWNVOTE, postId);
    }

    @Override
    public Result<Void> removeAuthorsFromPost(String userId) {
        Log.info("removeAuthorsFromPost : userId = " + userId);

        String query = "SELECT p FROM Post p WHERE p.authorId = '%s'";
        List<Post> allPosts = hibernate.jpql(format(query, userId), Post.class);

        try {
            for (Post post : allPosts) {
                post.setAuthorId(null);
                hibernate.update(post);
            }
            return Result.ok();
        } catch (Exception e) {
            e.printStackTrace();
            Log.info("Failed removing authors : " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> removeVotesFromPost(String userId, String userPassword) {
        Log.info("removeVotesFromPost : userId = " + userId + ", userPassword = " + userPassword);

        String query = "SELECT v FROM Vote v WHERE v.voterId = '%s'";
        List<Vote> allVotes = hibernate.jpql(format(query, userId), Vote.class);

        try {
            for (Vote vote : allVotes) {
                Result<Void> res;
                if(vote.isUpVoted()){
                    res = removeUpVotePost(vote.getPostId(), userId, userPassword);
                }else{
                    res = removeDownVotePost(vote.getPostId(), userId, userPassword);
                }
                if(!res.isOK())
                    return Result.error(res.error());
            }
            return Result.ok();

        } catch (Exception e) {
            e.printStackTrace();
            Log.info("Failed removing authors : " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    private Result<List<Post>> deletePostsAndReplies(Post post) {
        List<Post> allPostsToDelete = new ArrayList<>();

        allPostsToDelete.add(post);

        var res = collectReplies(post.getPostId(), allPostsToDelete);
        if (!res.isOK()) {
            return Result.error(res.error());
        }

        return Result.ok(allPostsToDelete);
    }

    private Result<Void> collectReplies(String parentId, List<Post> posts) {
        try {
            String query = "SELECT p FROM Post p WHERE p.parentId = '%s'";
            List<Post> replies = hibernate.jpql(format(query, parentId), Post.class);

            posts.addAll(replies);

            for (Post reply : replies) {
                var res = collectReplies(reply.getPostId(), posts);
                if (!res.isOK()) {
                    return Result.error(res.error());  // Return error result if anything goes wrong during recursion
                }
            }

            return Result.ok();
        } catch (Exception e) {
            Log.info("collectReplies failed: " + e.getMessage());
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);  // Return error if anything goes wrong
        }
    }

    private Result<Void> votePost(boolean upVote, boolean remove, String postId, String userId, String userPassword) {
        var postRes = getPost(postId);
        if(!postRes.isOK())
            return Result.error(postRes.error());

        Post post = postRes.value();

        var user = validUser(userId, userPassword);
        if(!user.isOK())
            return Result.error(user.error());

        var alrVoted = voteExist(remove, postId, userId);
        if(!alrVoted.isOK() && alrVoted.error() != Result.ErrorCode.NOT_FOUND)
            return Result.error(alrVoted.error());

        Vote vote;

        if(remove)
            vote = alrVoted.value();
        else
            vote = new Vote(upVote, postId, userId);
        try {
            if(remove){
                hibernate.delete(vote);
                Log.info("Post " + postId + " " + (upVote ? "upvote" : "downvote") + " removed.");
            } else{
                hibernate.persist(vote);
                Log.info("Post " + postId + " " + (upVote ? "upvoted" : "downvoted") + ".");
            }

            post.updateVotes(upVote, remove);

            hibernate.update(post);
            return Result.ok();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
    }


    private Result<Integer> getVotes(boolean upVote, String postId) {
        var post = getPost(postId);
        if(!post.isOK())
            return Result.error(post.error());

        if(upVote)
            return Result.ok(post.value().getUpVote());
        return Result.ok(post.value().getDownVote());
    }

    private Result<Void> validUser(String userId, String password) {
        var res = getUsersClients().getUser(userId, password);

        if(!res.isOK()) {
            if (res.error() == Result.ErrorCode.NOT_FOUND || res.error() == Result.ErrorCode.FORBIDDEN)
                return Result.error(res.error());
            else
                return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        return Result.ok();
    }

    private Result<Void> validPost(Post post, String userPassword) {
        String parentUrl = post.getParentUrl();
        String parentId = null;
        if(parentUrl != null && !parentUrl.isEmpty()){
            String[] parts = parentUrl.split("/");
            parentId = parts[parts.length - 1];
            var res = getPost(parentId);
            if(!res.isOK())
                return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        var res = validUser(post.getAuthorId(), userPassword);
        if(!res.isOK())
            return Result.error(res.error());

        return Result.ok();
    }

    private Result<Vote> voteExist(boolean remove, String postId, String userId) {
        String query = "SELECT v FROM Vote v WHERE v.postId = '%s' AND v.voterId = '%s'";
        var vote = hibernate.jpql(format(query, postId, userId), Vote.class);

        if (remove == vote.isEmpty())
            return Result.error(Result.ErrorCode.CONFLICT);

        if(remove)
            return Result.ok(vote.get(0));
        return Result.error(Result.ErrorCode.NOT_FOUND);
    }

    private boolean definedTimestamp(long timestamp) {
        return timestamp > 0;
    }

    private boolean definedSortOrder(String sortOrder) {
        return sortOrder != null && !sortOrder.isEmpty();
    }

}
