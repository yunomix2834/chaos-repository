package com.chaos.task_manager.controller.grpc;

import com.arena.proto.TaskAck;
import com.arena.proto.TaskCommand;
import com.arena.proto.TaskManagerServiceGrpc;
import com.chaos.task_manager.service.ArenaTaskService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import reactor.core.publisher.Mono;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class TaskManagerGrpcController extends TaskManagerServiceGrpc.TaskManagerServiceImplBase {

    private final ArenaTaskService arenaTaskService;

    @Override
    public void submit(TaskCommand request, StreamObserver<TaskAck> responseObserver) {
        arenaTaskService.submitFromScript(request)
                .onErrorResume(ex -> {
                    log.error("[GRPC][SUBMIT] failed: {}", ex.getMessage(), ex);
                    return Mono.just(TaskAck.newBuilder()
                            .setArenaId(request.getArenaId())
                            .setTaskId(request.getTaskId())
                            .setAccepted(false)
                            .setMessage(ex.getMessage())
                            .build());
                })
                .subscribe(ack -> {
                    responseObserver.onNext(ack);
                    responseObserver.onCompleted();
                });
    }
}
