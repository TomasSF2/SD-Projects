package fctreddit.impl.grpc.server;

import com.google.protobuf.ByteString;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.impl.grpc.generated_java.ImageGrpc;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf.CreateImageResult;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf.GetImageResult;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf.DeleteImageResult;

import fctreddit.impl.java.servers.JavaImage;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;


public class GrpcImageServerStub extends AbstractGrpcStub implements ImageGrpc.AsyncService {

    Image impl = new JavaImage();

    @Override
    public ServerServiceDefinition bindService() {
        return ImageGrpc.bindService(this);
    }

    @Override
    public void createImage(ImageProtoBuf.CreateImageArgs request, StreamObserver<ImageProtoBuf.CreateImageResult> responseObserver) {
        Result<String> res = impl.createImage( request.getUserId(), request.getImageContents().toByteArray(), request.getPassword());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(CreateImageResult.newBuilder().setImageId( res.value() ).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getImage(ImageProtoBuf.GetImageArgs request, StreamObserver<ImageProtoBuf.GetImageResult> responseObserver) {
        Result<byte[]> res = impl.getImage( request.getUserId(), request.getImageId());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(GetImageResult.newBuilder().setData(ByteString.copyFrom(res.value())).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void deleteImage(ImageProtoBuf.DeleteImageArgs request, StreamObserver<ImageProtoBuf.DeleteImageResult> responseObserver) {
        Result<Void> res = impl.deleteImage( request.getUserId(), request.getImageId(), request.getPassword());
        if( ! res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(DeleteImageResult.newBuilder().build());
            responseObserver.onCompleted();
        }
    }
}
