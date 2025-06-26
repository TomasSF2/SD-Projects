package fctreddit.impl.grpc.clients;

import fctreddit.api.Post;
import fctreddit.api.java.Result;
import fctreddit.impl.api.java.ExtendedContent;
import fctreddit.impl.grpc.generated_java.ContentGrpc;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.CreatePostArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GetPostsArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GetPostArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GetPostAnswersArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.UpdatePostArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.DeletePostArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.ChangeVoteArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.RemoveAuthorArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.RemoveVotesArgs;
import io.grpc.LoadBalancerRegistry;
import io.grpc.internal.PickFirstLoadBalancerProvider;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import static fctreddit.impl.grpc.util.DataModelAdaptor.*;

public class GrpcContentClient extends GrpcClient implements ExtendedContent {

    static{
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
    }

    private static Logger Log = Logger.getLogger(GrpcContentClient.class.getName());

    final ContentGrpc.ContentBlockingStub stub;


    public GrpcContentClient(URI serverURI ) {
        super(serverURI);
        stub = ContentGrpc.newBlockingStub(channel);
    }
    @Override
    public Result<String> createPost(Post post, String userPassword) {
        return super.getResult(() -> {
            var res = stub.createPost(CreatePostArgs.newBuilder()
                    .setPost(Post_to_GrpcPost(post))
                    .build());
            return res.getPostId();
        });
    }

    @Override
    public Result<List<String>> getPosts(long timestamp, String sortOrder) {
        return super.getResult(() -> {
            var res = stub.getPosts(GetPostsArgs.newBuilder()
                    .setTimestamp(timestamp)
                    .setSortOrder(sortOrder)
                    .build());
            return res.getPostIdList();
        });
    }

    @Override
    public Result<Post> getPost(String postId) {
        return super.getResult(() -> {
            var res = stub.getPost(GetPostArgs.newBuilder()
                    .setPostId(postId)
                    .build());
            return GrpcPost_to_Post(res);
        });
    }

    @Override
    public Result<List<String>> getPostAnswers(String postId, long maxTimeout) {
        return super.getResult(() -> {
            var res = stub.getPostAnswers(GetPostAnswersArgs.newBuilder()
                    .setPostId(postId)
                    .setTimeout(maxTimeout)
                    .build());
            return res.getPostIdList();
        });
    }

    @Override
    public Result<Post> updatePost(String postId, String userPassword, Post post) {
        return super.getResult(() -> {
            var res = stub.updatePost(UpdatePostArgs.newBuilder()
                    .setPostId(postId)
                    .setPostId(userPassword)
                    .setPost(Post_to_GrpcPost(post))
                    .build());
            return GrpcPost_to_Post(res);
        });
    }

    @Override
    public Result<Void> deletePost(String postId, String userPassword) {
        return super.getResult(() -> {
            stub.deletePost(DeletePostArgs.newBuilder()
                    .setPostId(postId)
                    .setPostId(userPassword)
                    .build());
        });
    }

    @Override
    public Result<Void> upVotePost(String postId, String userId, String userPassword) {
        return super.getResult(() -> {
            stub.upVotePost(ChangeVoteArgs.newBuilder()
                    .setPostId(postId)
                    .setUserId(userId)
                    .setPostId(userPassword)
                    .build());
        });
    }

    @Override
    public Result<Void> removeUpVotePost(String postId, String userId, String userPassword) {
        return super.getResult(() -> {
            stub.removeUpVotePost(ChangeVoteArgs.newBuilder()
                    .setPostId(postId)
                    .setUserId(userId)
                    .setPostId(userPassword)
                    .build());
        });
    }

    @Override
    public Result<Void> downVotePost(String postId, String userId, String userPassword) {
        return super.getResult(() -> {
             stub.downVotePost(ChangeVoteArgs.newBuilder()
                    .setPostId(postId)
                    .setUserId(userId)
                    .setPostId(userPassword)
                    .build());
        });
    }

    @Override
    public Result<Void> removeDownVotePost(String postId, String userId, String userPassword) {
        return super.getResult(() -> {
            stub.removeDownVotePost(ChangeVoteArgs.newBuilder()
                    .setPostId(postId)
                    .setUserId(userId)
                    .setPostId(userPassword)
                    .build());
        });
    }

    @Override
    public Result<Integer> getupVotes(String postId) {
        return super.getResult(() -> {
            var res = stub.getUpVotes(GetPostArgs.newBuilder()
                    .setPostId(postId)
                    .build());
            return res.getCount();
        });
    }

    @Override
    public Result<Integer> getDownVotes(String postId) {
        return super.getResult(() -> {
            var res = stub.getDownVotes(GetPostArgs.newBuilder()
                    .setPostId(postId)
                    .build());
            return res.getCount();
        });
    }

    @Override
    public Result<Void> removeAuthorsFromPost(String userId) {
        return super.getResult(() -> {
            stub.removeAuthorsFromPost(RemoveAuthorArgs.newBuilder()
                    .setUserId(userId)
                    .build());
        });
    }

    @Override
    public Result<Void> removeVotesFromPost(String userId, String userPassword) {
        return super.getResult(() -> {
            stub.removeVotesFromPost(RemoveVotesArgs.newBuilder()
                    .setUserId(userId)
                    .setPassword(userPassword)
                    .build());
        });
    }
}
