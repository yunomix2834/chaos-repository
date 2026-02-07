package com.chaos.task_manager.controller.message_queue.handler;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HandlerRegistry {

    private final Map<String, HandlerBase<?>> handlers;

    public HandlerRegistry(Map<String, HandlerBase<?>> handlers) {
        this.handlers = handlers;
    }

    public boolean contains(String beanName) {
        return handlers.containsKey(beanName);
    }

    @SuppressWarnings("unchecked")
    public <T> HandlerBase<T> get(String beanName) {
        return (HandlerBase<T>) handlers.get(beanName);
    }
}
