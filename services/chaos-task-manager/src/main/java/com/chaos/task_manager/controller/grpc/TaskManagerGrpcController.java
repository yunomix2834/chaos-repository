package com.chaos.task_manager.controller.grpc;

import com.arena.proto.TaskManagerServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class TaskManagerGrpcController extends TaskManagerServiceGrpc.TaskManagerServiceImplBase {



}
