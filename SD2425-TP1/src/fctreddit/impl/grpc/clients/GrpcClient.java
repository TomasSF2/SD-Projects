package fctreddit.impl.grpc.clients;

import fctreddit.api.java.Result;
import fctreddit.api.java.Result.ErrorCode;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.net.URI;
import java.util.function.Supplier;

public class GrpcClient {

    final protected Channel channel;

    public GrpcClient(URI serverURI) {
        this.channel = ManagedChannelBuilder.forAddress(serverURI.getHost(), serverURI.getPort())
                .usePlaintext().enableRetry().build();
    }

    protected <T> Result<T> getResult(Supplier<T> func) {
        try {
            return Result.ok(func.get());
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        } catch (Exception x) {
            x.printStackTrace();
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    protected Result<Void> getResult(Runnable proc) {
        return getResult( () -> {
            proc.run();
            return null;
        } );
    }

    static Result.ErrorCode statusToErrorCode(Status status ) {
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
