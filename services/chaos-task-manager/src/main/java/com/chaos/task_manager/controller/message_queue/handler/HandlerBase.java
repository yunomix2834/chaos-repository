package com.chaos.task_manager.controller.message_queue.handler;

public interface HandlerBase<T> {
    void handle(T MessageRequestType);
}
