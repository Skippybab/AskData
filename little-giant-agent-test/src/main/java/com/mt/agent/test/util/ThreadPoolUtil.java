package com.mt.agent.test.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池工具类
 * 用于管理测试任务的多线程执行
 *
 * @author MT Agent Team
 * @version 1.0
 * @since 2025-01-15
 */
@Slf4j
@Component
public class ThreadPoolUtil {

    private ExecutorService executorService;
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 100;
    private static final int KEEP_ALIVE_TIME = 60;

    @PostConstruct
    public void init() {
        ThreadFactory namedThreadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("test-thread-" + threadNumber.getAndIncrement());
                thread.setDaemon(false);
                return thread;
            }
        };

        executorService = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                namedThreadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());

        log.info("线程池初始化完成，核心线程数: {}, 最大线程数: {}", CORE_POOL_SIZE, MAX_POOL_SIZE);
    }

    /**
     * 提交任务到线程池
     *
     * @param task 任务
     * @return Future对象
     */
    public <T> Future<T> submit(Callable<T> task) {
        return executorService.submit(task);
    }

    /**
     * 提交任务到线程池
     *
     * @param task 任务
     * @return Future对象
     */
    public Future<?> submit(Runnable task) {
        return executorService.submit(task);
    }

    /**
     * 批量提交任务并等待全部完成
     *
     * @param tasks   任务列表
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 所有任务的执行结果
     */
    public <T> CompletableFuture<Void> submitAllAndWait(java.util.List<Callable<T>> tasks,
            long timeout,
            TimeUnit unit) {
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                tasks.stream()
                        .map(task -> CompletableFuture.supplyAsync(() -> {
                            try {
                                return task.call();
                            } catch (Exception e) {
                                log.error("任务执行失败: {}", e.getMessage(), e);
                                throw new RuntimeException(e);
                            }
                        }, executorService))
                        .toArray(CompletableFuture[]::new));

        return allOf.orTimeout(timeout, unit);
    }

    /**
     * 安全关闭线程池
     */
    @PreDestroy
    public void shutdown() {
        if (executorService != null) {
            log.info("开始关闭线程池...");
            executorService.shutdown();

            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("线程池未能在30秒内正常关闭，强制关闭");
                    executorService.shutdownNow();

                    if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                        log.error("线程池强制关闭失败");
                    }
                }
            } catch (InterruptedException e) {
                log.warn("等待线程池关闭时被中断");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }

            log.info("线程池已关闭");
        }
    }

    /**
     * 获取线程池状态信息
     *
     * @return 状态信息
     */
    public String getPoolStatus() {
        if (executorService instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
            return String.format("Pool状态: [核心线程数=%d, 活跃线程数=%d, 池大小=%d, 队列大小=%d, 完成任务数=%d]",
                    tpe.getCorePoolSize(),
                    tpe.getActiveCount(),
                    tpe.getPoolSize(),
                    tpe.getQueue().size(),
                    tpe.getCompletedTaskCount());
        }
        return "无法获取线程池状态";
    }

    /**
     * 检查线程池是否可用
     *
     * @return true表示线程池可用
     */
    public boolean isAvailable() {
        return executorService != null && !executorService.isShutdown();
    }
}