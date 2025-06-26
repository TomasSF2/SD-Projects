package fctreddit.impl.grpc.server;

import fctreddit.api.User;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.impl.grpc.generated_java.UsersGrpc;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.CreateUserArgs;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.CreateUserResult;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.GetUserArgs;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.GetUserResult;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.UpdateUserArgs;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.UpdateUserResult;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.DeleteUserArgs;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.DeleteUserResult;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.SearchUserArgs;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.GrpcUser ;
import fctreddit.impl.java.servers.JavaUsers;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;

import java.util.List;

import static fctreddit.impl.grpc.util.DataModelAdaptor.GrpcUser_to_User;
import static fctreddit.impl.grpc.util.DataModelAdaptor.User_to_GrpcUser;

public class GrpcUsersServerStub extends AbstractGrpcStub implements UsersGrpc.AsyncService {

    Users impl = new JavaUsers();

    @Override
    public ServerServiceDefinition bindService() {
        return UsersGrpc.bindService(this);
    }

    @Override
    public void createUser(CreateUserArgs request, StreamObserver<CreateUserResult> responseObserver) {
        Result<String> res = impl.createUser( GrpcUser_to_User(request.getUser()));
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( CreateUserResult.newBuilder().setUserId( res.value() ).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getUser(GetUserArgs request, StreamObserver<GetUserResult> responseObserver) {
        Result<User> res = impl.getUser(request.getUserId(), request.getPassword());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( GetUserResult.newBuilder().setUser(User_to_GrpcUser(res.value())).build() );
            responseObserver.onCompleted();
        }    }

    @Override
    public void updateUser(UpdateUserArgs request, StreamObserver<UpdateUserResult> responseObserver) {
        var res = impl.updateUser(request.getUserId(), request.getPassword(), GrpcUser_to_User(request.getUser()));
        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(UpdateUserResult.newBuilder().setUser(User_to_GrpcUser(res.value())).build());
            responseObserver.onCompleted();
        }    }

    @Override
    public void deleteUser(DeleteUserArgs request, StreamObserver<DeleteUserResult> responseObserver) {
        var res = impl.deleteUser(request.getUserId(), request.getPassword());
        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(DeleteUserResult.newBuilder().setUser(User_to_GrpcUser(res.value())).build());
            responseObserver.onCompleted();
        }    }

    @Override
    public void searchUsers(SearchUserArgs request, StreamObserver<GrpcUser> responseObserver) {
        Result<List<User>> res = impl.searchUsers(request.getPattern());

        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            for(User u: res.value()) {
                responseObserver.onNext( User_to_GrpcUser(u));
            }
            responseObserver.onCompleted();
        }    }

}
