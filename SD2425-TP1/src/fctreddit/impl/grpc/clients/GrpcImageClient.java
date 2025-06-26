package fctreddit.impl.grpc.clients;

import com.google.protobuf.ByteString;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf.CreateImageArgs;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf.GetImageArgs;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf.DeleteImageArgs;
import fctreddit.impl.grpc.generated_java.ImageGrpc;
import io.grpc.LoadBalancerRegistry;
import io.grpc.internal.PickFirstLoadBalancerProvider;

import java.net.URI;


public class GrpcImageClient extends GrpcClient implements Image {
    static{
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
    }

    final ImageGrpc.ImageBlockingStub stub;

    public GrpcImageClient(URI serverURI ) {
        super(serverURI);
        stub = ImageGrpc.newBlockingStub(channel);


    }
    @Override
    public Result<String> createImage(String userId, byte[] imageContents, String password) {
        return super.getResult(() -> {
            var res = stub.createImage(CreateImageArgs.newBuilder()
                    .setUserId(userId)
                    .setImageContents(ByteString.copyFrom(imageContents))
                    .setPassword(password)
                    .build());
            return res.getImageId();
        });
    }

    @Override
    public Result<byte[]> getImage(String userId, String imageId) {
        return super.getResult(() -> {
            var res = stub.getImage(GetImageArgs.newBuilder()
                    .setUserId(userId)
                    .setImageId(imageId)
                    .build());
            return res.next().getData().toByteArray();
        });
    }

    @Override
    public Result<Void> deleteImage(String userId, String imageId, String password) {
        return super.getResult(() -> {
            stub.deleteImage(DeleteImageArgs.newBuilder()
                    .setUserId(userId)
                    .setImageId(imageId)
                    .setPassword(password)
                    .build());
        });
    }
}
