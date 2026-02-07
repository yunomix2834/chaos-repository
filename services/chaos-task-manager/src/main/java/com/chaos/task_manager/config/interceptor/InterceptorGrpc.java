package com.chaos.task_manager.config.interceptor;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
public class InterceptorGrpc implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall,
            Metadata metadata,
            ServerCallHandler<ReqT, RespT> serverCallHandler) {

        try {
            Function<String, String> getValue = key -> metadata.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));

            Context context = Context.ROOT;

            return Contexts.interceptCall(context, serverCall, metadata, serverCallHandler);
        } catch (Exception e) {
            Status status = Status.UNAVAILABLE.withDescription(e.getMessage());
            serverCall.close(status, metadata);
        }

        return serverCallHandler.startCall(serverCall, metadata);
    }

}
