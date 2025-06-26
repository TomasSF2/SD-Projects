package fctreddit.impl.server.grpc;

import java.util.List;
import java.util.logging.Logger;

import fctreddit.api.Post;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.impl.client.grpc.DataModelAdaptor;
import fctreddit.impl.grpc.generated_java.ContentGrpc;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.ChangeVoteArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.CreatePostArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.CreatePostResult;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.DeletePostArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.EmptyMessage;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GetPostAnswersArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GetPostArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GetPostsArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GetPostsResult;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GetPostsResult.Builder;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GrpcPost;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.RemoveTracesOfUserArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.UpdatePostArgs;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.VoteCountResult;
import fctreddit.impl.server.java.JavaContent;
import fctreddit.impl.server.java.JavaUsers;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class GrpcContentServerStub extends GrpcStub implements ContentGrpc.AsyncService, BindableService {
	private static Logger Log = Logger.getLogger(GrpcContentServerStub.class.getName());

	private Content impl = new JavaContent();
	private static String SECRET;

	public GrpcContentServerStub(String secret) {
		SECRET = secret;
	}
	@Override
	public ServerServiceDefinition bindService() {
		return ContentGrpc.bindService(this);
	}

	@Override
	public void createPost(CreatePostArgs request, StreamObserver<CreatePostResult> responseObserver) {
        Result<String> res = null;
        try {
            res = impl.createPost(DataModelAdaptor.GrpcPost_to_Content(request.getPost()), request.getPassword());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if(! res.isOK() )
			responseObserver.onError(errorCodeToStatus(res.error()));
		else {
			responseObserver.onNext(CreatePostResult.newBuilder().setPostId(res.value()).build());
			responseObserver.onCompleted();
		}	
	}

	@Override
	public void getPosts(GetPostsArgs request, StreamObserver<GetPostsResult> responseObserver) {
		Result<List<String>> res = impl.getPosts(request.hasTimestamp() ? request.getTimestamp() : 0, 
												request.hasSortOrder() ? request.getSortOrder() : null);
		
		if (! res.isOK() ) {
			responseObserver.onError(errorCodeToStatus(res.error()));
		} else {
			Builder b = GetPostsResult.newBuilder();
			b.addAllPostId(res.value());
			responseObserver.onNext(b.build());
			responseObserver.onCompleted();
		}
	}

	@Override
	public void getPost(GetPostArgs request, StreamObserver<GrpcPost> responseObserver) {
		Result<Post> res = impl.getPost(request.getPostId());
		
		if (! res.isOK() ) {
			responseObserver.onError(errorCodeToStatus(res.error()));
		} else {
			responseObserver.onNext(DataModelAdaptor.Post_to_GrpcPost(res.value()));
			responseObserver.onCompleted();
		}
	}

	@Override
	public void getPostAnswers(GetPostAnswersArgs request, StreamObserver<GetPostsResult> responseObserver) {
		Result<List<String>> res = impl.getPostAnswers(request.getPostId(), request.hasTimeout() ? request.getTimeout() : 0);
		
		if( ! res.isOK() )
			responseObserver.onError(errorCodeToStatus(res.error()));
		else {
			Builder b = GetPostsResult.newBuilder();
			b.addAllPostId(res.value());
			responseObserver.onNext(b.build());
			responseObserver.onCompleted();
		}
	}

	@Override
	public void updatePost(UpdatePostArgs request, StreamObserver<GrpcPost> responseObserver) {
        Result<Post> res = null;
        try {
            res = impl.updatePost(request.getPostId(), request.getPassword(),
                                        DataModelAdaptor.GrpcPost_to_Content(request.getPost()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (! res.isOK() )
			responseObserver.onError(errorCodeToStatus(res.error()));
		else {
			responseObserver.onNext(DataModelAdaptor.Post_to_GrpcPost(res.value()));
			responseObserver.onCompleted();
		}
	}

	@Override
	public void deletePost(DeletePostArgs request, StreamObserver<EmptyMessage> responseObserver)  {
        Result<Void> res = null;
        try {
            res = impl.deletePost(request.getPostId(), request.getPassword());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (! res.isOK() ) {
			responseObserver.onError(errorCodeToStatus(res.error()));
		} else {
			responseObserver.onNext(EmptyMessage.getDefaultInstance());
			responseObserver.onCompleted();
		}
	}

	@Override
	public void upVotePost(ChangeVoteArgs request, StreamObserver<EmptyMessage> responseObserver)  {
        Result<Void> res = null;
        try {
            res = impl.upVotePost(request.getPostId(), request.getUserId(), request.getPassword());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (! res.isOK() ) {
			responseObserver.onError(errorCodeToStatus(res.error()));
		} else {
			responseObserver.onNext(EmptyMessage.getDefaultInstance());
			responseObserver.onCompleted();
		}
	}

	@Override
	public void removeUpVotePost(ChangeVoteArgs request, StreamObserver<EmptyMessage> responseObserver)  {
        Result<Void> res = null;
        try {
            res = impl.removeUpVotePost(request.getPostId(), request.getUserId(), request.getPassword());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (! res.isOK() ) {
			responseObserver.onError(errorCodeToStatus(res.error()));
		} else {
			responseObserver.onNext(EmptyMessage.getDefaultInstance());
			responseObserver.onCompleted();
		}
	}

	@Override
	public void downVotePost(ChangeVoteArgs request, StreamObserver<EmptyMessage> responseObserver)  {
        Result<Void> res = null;
        try {
            res = impl.downVotePost(request.getPostId(), request.getUserId(), request.getPassword());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (! res.isOK() ) {
			responseObserver.onError(errorCodeToStatus(res.error()));
		} else {
			responseObserver.onNext(EmptyMessage.getDefaultInstance());
			responseObserver.onCompleted();
		}
	}

	@Override
	public void removeDownVotePost(ChangeVoteArgs request, StreamObserver<EmptyMessage> responseObserver)  {
        Result<Void> res = null;
        try {
            res = impl.removeDownVotePost(request.getPostId(), request.getUserId(), request.getPassword());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (! res.isOK() ) {
			responseObserver.onError(errorCodeToStatus(res.error()));
		} else {
			responseObserver.onNext(EmptyMessage.getDefaultInstance());
			responseObserver.onCompleted();
		}		
	}

	@Override
	public void getUpVotes(GetPostArgs request, StreamObserver<VoteCountResult> responseObserver) {
		Result<Integer> res = impl.getupVotes(request.getPostId());
		
		if (! res.isOK() ) {
			responseObserver.onError(errorCodeToStatus(res.error()));
		} else {
			responseObserver.onNext(VoteCountResult.newBuilder().setCount(res.value()).build());
			responseObserver.onCompleted();
		}
	}

	@Override
	public void getDownVotes(GetPostArgs request, StreamObserver<VoteCountResult> responseObserver) {
		Result<Integer> res = impl.getDownVotes(request.getPostId());
		
		if (! res.isOK() ) {
			responseObserver.onError(errorCodeToStatus(res.error()));
		} else {
			responseObserver.onNext(VoteCountResult.newBuilder().setCount(res.value()).build());
			responseObserver.onCompleted();
		}
	}

	@Override
	public void removeTracesOfUser(RemoveTracesOfUserArgs request, StreamObserver<EmptyMessage> responseObserver) {

		this.verifyInternalAuthorization(request.getSecret(), SECRET);

		Result<Void> res = impl.removeTracesOfUser("", request.getUserId());
		
		if (! res.isOK() ) {
			responseObserver.onError(errorCodeToStatus(res.error()));
		} else {
			responseObserver.onNext(EmptyMessage.getDefaultInstance());
			responseObserver.onCompleted();
		}
	}

	protected void verifyInternalAuthorization(String auth, String expectedSecret) {
		if (auth == null || !auth.equals(expectedSecret)) {
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);
		}
	}
}
