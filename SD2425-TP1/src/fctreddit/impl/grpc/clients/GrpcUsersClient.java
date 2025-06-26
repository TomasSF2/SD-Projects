package fctreddit.impl.grpc.clients;

import fctreddit.api.User;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import fctreddit.impl.grpc.generated_java.UsersGrpc;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.CreateUserArgs;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.GetUserArgs;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.UpdateUserArgs;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.DeleteUserArgs;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.SearchUserArgs;
import io.grpc.LoadBalancerRegistry;
import io.grpc.internal.PickFirstLoadBalancerProvider;

import static fctreddit.impl.grpc.util.DataModelAdaptor.GrpcUser_to_User;
import static fctreddit.impl.grpc.util.DataModelAdaptor.User_to_GrpcUser;

public class GrpcUsersClient extends GrpcClient implements Users {

    static{
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
    }

    private static Logger Log = Logger.getLogger(GrpcUsersClient.class.getName());

    final UsersGrpc.UsersBlockingStub stub;

    public GrpcUsersClient(URI serverURI ) {
        super(serverURI);
        stub = UsersGrpc.newBlockingStub(channel);
    }

    @Override
    public Result<String> createUser(User user) {
        return super.getResult(() -> {
            var res = stub.createUser(CreateUserArgs.newBuilder()
                    .setUser(User_to_GrpcUser(user))
                    .build());
            return res.getUserId();
        });
    }

    @Override
    public Result<User> getUser(String userId, String password) {
        return super.getResult(()-> {
            var res = stub.getUser(GetUserArgs.newBuilder()
                    .setUserId(userId)
                    .setPassword(password)
                    .build());
            return GrpcUser_to_User(res.getUser());
        });
    }

    @Override
    public Result<User> updateUser(String userId, String password, User user) {
        return super.getResult(() -> {
            var res = stub.updateUser(UpdateUserArgs.newBuilder()
                    .setUserId(userId)
                    .setPassword(password)
                    .setUser(User_to_GrpcUser(user))
                    .build());
            return GrpcUser_to_User(res.getUser());
        });
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {
        return super.getResult(() -> {
            var res = stub.deleteUser(DeleteUserArgs.newBuilder()
                    .setUserId(userId)
                    .setPassword(password)
                    .build());
            return GrpcUser_to_User(res.getUser());
        });
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        return super.getResult(() -> {
            var res = stub.searchUsers(SearchUserArgs.newBuilder()
                    .setPattern(pattern)
                    .build());
            var list = new ArrayList<User>();
            res.forEachRemaining( user -> list.add( GrpcUser_to_User(user)) );
            return list;
        });
    }
}
