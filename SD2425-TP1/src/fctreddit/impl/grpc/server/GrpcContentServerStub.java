package fctreddit.impl.grpc.server;

import fctreddit.api.Post;
import fctreddit.api.java.Result;
import fctreddit.impl.api.java.ExtendedContent;
import fctreddit.impl.grpc.generated_java.ContentGrpc;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.CreatePostArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.CreatePostResult;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GetPostsArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GetPostsResult;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GetPostArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GetPostAnswersArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.UpdatePostArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.DeletePostArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.ChangeVoteArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.EmptyMessage;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.VoteCountResult;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.RemoveAuthorArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.RemoveVotesArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GrpcPost;
import fctreddit.impl.java.servers.JavaContent;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.logging.Logger;

import static fctreddit.impl.grpc.util.DataModelAdaptor.GrpcPost_to_Post;
import static fctreddit.impl.grpc.util.DataModelAdaptor.Post_to_GrpcPost;

public class GrpcContentServerStub extends AbstractGrpcStub implements ContentGrpc.AsyncService {
    private static Logger Log = Logger.getLogger(GrpcContentServerStub.class.getName());

    ExtendedContent impl = new JavaContent();

    @Override
    public ServerServiceDefinition bindService() {
        return ContentGrpc.bindService(this);
    }

    @Override
    public void createPost(CreatePostArgs request, StreamObserver<CreatePostResult> responseObserver) {
        Result<String> res = impl.createPost( GrpcPost_to_Post(request.getPost()), request.getPassword());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( CreatePostResult.newBuilder().setPostId( res.value() ).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getPosts(GetPostsArgs request, StreamObserver<GetPostsResult> responseObserver) {
        Result<List<String>> res = impl.getPosts( request.getTimestamp(), request.getSortOrder());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( GetPostsResult.newBuilder().addAllPostId(res.value()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getPost(GetPostArgs request, StreamObserver<GrpcPost> responseObserver) {
        Result<Post> res = impl.getPost( request.getPostId());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( Post_to_GrpcPost(res.value()));
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getPostAnswers(GetPostAnswersArgs request, StreamObserver<GetPostsResult> responseObserver) {
        Result<List<String>> res = impl.getPostAnswers( request.getPostId(), request.getTimeout());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( GetPostsResult.newBuilder().addAllPostId(res.value()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void updatePost(UpdatePostArgs request, StreamObserver<GrpcPost> responseObserver) {
        Result<Post> res = impl.updatePost( request.getPostId(), request.getPassword(), GrpcPost_to_Post(request.getPost()));
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(Post_to_GrpcPost(res.value()));
            responseObserver.onCompleted();
        }
    }

    @Override
    public void deletePost(DeletePostArgs request, StreamObserver<EmptyMessage> responseObserver) {
        Result<Void> res = impl.deletePost( request.getPostId(), request.getPassword());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(EmptyMessage.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void upVotePost(ChangeVoteArgs request, StreamObserver<EmptyMessage> responseObserver) {
        Result<Void> res = impl.upVotePost( request.getPostId(), request.getUserId(), request.getPassword());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(EmptyMessage.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void removeUpVotePost(ChangeVoteArgs request, StreamObserver<EmptyMessage> responseObserver) {
        Result<Void> res = impl.removeUpVotePost( request.getPostId(), request.getUserId(), request.getPassword());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(EmptyMessage.newBuilder().build());
            responseObserver.onCompleted();
        }    }

    @Override
    public void downVotePost(ChangeVoteArgs request, StreamObserver<EmptyMessage> responseObserver) {
        Result<Void> res = impl.downVotePost( request.getPostId(), request.getUserId(), request.getPassword());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(EmptyMessage.newBuilder().build());
            responseObserver.onCompleted();
        }    }

    @Override
    public void removeDownVotePost(ChangeVoteArgs request, StreamObserver<EmptyMessage> responseObserver) {
        Result<Void> res = impl.removeDownVotePost( request.getPostId(), request.getUserId(), request.getPassword());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(EmptyMessage.newBuilder().build());
            responseObserver.onCompleted();
        }    }

    @Override
    public void getUpVotes(GetPostArgs request, StreamObserver<VoteCountResult> responseObserver) {
        Result<Integer> res = impl.getupVotes( request.getPostId());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(VoteCountResult.newBuilder().setCount(res.value()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getDownVotes(GetPostArgs request, StreamObserver<VoteCountResult> responseObserver) {
        Result<Integer> res = impl.getDownVotes( request.getPostId());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(VoteCountResult.newBuilder().setCount(res.value()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void removeAuthorsFromPost(RemoveAuthorArgs request, StreamObserver<EmptyMessage> responseObserver) {
        Result<Void> res = impl.removeAuthorsFromPost( request.getUserId());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(EmptyMessage.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void removeVotesFromPost(RemoveVotesArgs request, StreamObserver<EmptyMessage> responseObserver) {
        Result<Void> res = impl.removeVotesFromPost( request.getUserId(), request.getPassword());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(EmptyMessage.newBuilder().build());
            responseObserver.onCompleted();
        }
    }
}
