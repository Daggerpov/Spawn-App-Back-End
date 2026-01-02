package com.danielagapov.spawn.shared.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution.
 * Defines thread pool settings for async operations like email sending.
 */
@Configuration
public class AsyncConfiguration implements AsyncConfigurer {

    /**
     * Configure the thread pool executor for async email operations.
     * Uses a dedicated thread pool to prevent blocking the main application threads.
     */
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - minimum number of threads to keep alive
        executor.setCorePoolSize(2);
        
        // Maximum pool size - maximum number of threads to create
        executor.setMaxPoolSize(5);
        
        // Queue capacity - number of tasks to queue before creating new threads
        executor.setQueueCapacity(100);
        
        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("email-async-");
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // Maximum time to wait for tasks to complete on shutdown (30 seconds)
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        return executor;
    }

    /**
     * Default executor for async operations not explicitly configured.
     */
    @Override
    public Executor getAsyncExecutor() {
        return emailTaskExecutor();
    }

    /**
     * Handle exceptions thrown by async methods.
     * This prevents silent failures in async operations.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            System.err.println("Async exception in method: " + method.getName());
            System.err.println("Exception message: " + throwable.getMessage());
            throwable.printStackTrace();
        };
    }
}



