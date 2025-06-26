package fctreddit.impl.client.grpc;

import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Iterator;

import com.google.protobuf.ByteString;

import fctreddit.api.java.Result;
import fctreddit.api.java.Result.ErrorCode;
import fctreddit.impl.client.ImageClient;
import fctreddit.impl.grpc.generated_java.ImageGrpc;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf.CreateImageArgs;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf.CreateImageResult;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf.DeleteImageArgs;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf.DeleteImageResult;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf.GetImageArgs;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf.GetImageResult;
import io.grpc.Channel;
import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.TrustManagerFactory;

public class GrpcImageClient extends ImageClient {

	static {
		LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
	}
	
	final ImageGrpc.ImageBlockingStub stub;
	
	public GrpcImageClient(URI serverURI) throws Exception {
		super(serverURI);
		String trustStoreFilename = System.getProperty("javax.net.ssl.trustStore");
		String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");

		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		try(FileInputStream input = new FileInputStream(trustStoreFilename)) {
			trustStore.load(input, trustStorePassword.toCharArray());
		}

		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
				TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(trustStore);

		SslContext context = GrpcSslContexts
				.configure(
						SslContextBuilder.forClient().trustManager(trustManagerFactory)
				).build();

		Channel channel = NettyChannelBuilder
				.forAddress(serverURI.getHost(), serverURI.getPort())
				.sslContext(context)
				.enableRetry()
				.build();

		stub = ImageGrpc.newBlockingStub(channel);
	}

	@Override
	public Result<String> createImage(String userId, byte[] imageContents, String password) {
		try {
			CreateImageResult res = stub.createImage(CreateImageArgs.newBuilder()
					.setUserId(userId)
					.setImageContents(ByteString.copyFrom(imageContents))
					.setPassword(password)
					.build());
			
			return Result.ok(res.getImageId());
		
		} catch (StatusRuntimeException sre) {
			return Result.error(statusToErrorCode(sre.getStatus()));
		}
	}

	@Override
	public Result<byte[]> getImage(String userId, String imageId) {
		try {
			Iterator<GetImageResult> res = stub.getImage(GetImageArgs.newBuilder()
					.setUserId(userId)
					.setImageId(imageId)
					.build());
			
			ByteString bs = ByteString.empty();
			
			while(res.hasNext()) {
				bs = bs.concat(res.next().getData());
			}
			
			return Result.ok(bs.toByteArray());
		} catch (StatusRuntimeException sre) {
			return Result.error(statusToErrorCode(sre.getStatus()));
		}
	}

	@Override
	public Result<Void> deleteImage(String userId, String imageId, String password) {
		try {
			@SuppressWarnings("unused")
			DeleteImageResult res = stub.deleteImage(DeleteImageArgs
					.newBuilder()
					.setUserId(userId)
					.setImageId(imageId)
					.setPassword(password)
					.build());
			
			return Result.ok();
			
		} catch (StatusRuntimeException sre) {
			return Result.error(statusToErrorCode(sre.getStatus()));
		}
	}
	
	static ErrorCode statusToErrorCode( Status status ) {
    	return switch( status.getCode() ) {
    		case OK -> ErrorCode.OK;
    		case NOT_FOUND -> ErrorCode.NOT_FOUND;
    		case ALREADY_EXISTS -> ErrorCode.CONFLICT;
    		case PERMISSION_DENIED -> ErrorCode.FORBIDDEN;
    		case INVALID_ARGUMENT -> ErrorCode.BAD_REQUEST;
    		case UNIMPLEMENTED -> ErrorCode.NOT_IMPLEMENTED;
    		default -> ErrorCode.INTERNAL_ERROR;
    	};
    }
}
