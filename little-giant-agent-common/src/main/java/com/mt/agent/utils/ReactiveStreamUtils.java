package com.mt.agent.utils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.http.codec.ServerSentEvent;

/**
 * 响应式流工具类
 * 提供处理响应式流操作的通用方法
 *
 * @author lfz
 */
public class ReactiveStreamUtils {

    /**
     * 执行同步操作并将结果转换为响应式流
     * 将同步操作包装在单独的线程中执行，避免阻塞主线程
     *
     * @param syncOperation 同步操作，返回 Flux 结果
     * @param errorHandler  错误处理器，接收异常并返回替代 Flux
     * @param <T>           流中元素的类型
     * @return 响应式流
     */
    public static <T> Flux<T> executeStreamOperation(
            Callable<Flux<T>> syncOperation,
            Function<Throwable, Flux<T>> errorHandler) {

        return Mono.fromCallable(syncOperation)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(flux -> flux)
                .onErrorResume(errorHandler);
    }

    /**
     * 执行同步操作并将结果转换为响应式流，包含资源清理功能
     * 将同步操作包装在单独的线程中执行，避免阻塞主线程
     *
     * @param syncOperation   同步操作，返回 Flux 结果
     * @param errorHandler    错误处理器，接收异常并返回替代 Flux
     * @param finallyCallback 最终回调，无论成功失败都会执行
     * @param <T>             流中元素的类型
     * @return 响应式流
     */
    public static <T> Flux<T> executeStreamOperation(
            Callable<Flux<T>> syncOperation,
            Function<Throwable, Flux<T>> errorHandler,
            Consumer<reactor.core.publisher.SignalType> finallyCallback) {

        return Mono.fromCallable(syncOperation)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(flux -> flux)
                .doFinally(finallyCallback)
                .onErrorResume(errorHandler);
    }

    /**
     * 执行同步操作并将结果转换为ServerSentEvent响应式流，包含资源清理功能
     * 专门用于处理返回ServerSentEvent的流操作
     *
     * @param syncOperation   同步操作，返回 Flux<ServerSentEvent<?>> 结果
     * @param errorHandler    错误处理器，接收异常并返回替代 Flux<ServerSentEvent<?>>
     * @param finallyCallback 最终回调，无论成功失败都会执行
     * @return ServerSentEvent响应式流
     */
    public static Flux<ServerSentEvent<?>> executeServerSentEventOperation(
            Callable<Flux<ServerSentEvent<?>>> syncOperation,
            Function<Throwable, Flux<ServerSentEvent<?>>> errorHandler,
            Consumer<reactor.core.publisher.SignalType> finallyCallback) {

        return Mono.fromCallable(syncOperation)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(flux -> flux)
                .doFinally(finallyCallback)
                .onErrorResume(errorHandler);
    }

    /**
     * 为流添加超时处理
     *
     * @param flux           原始流
     * @param timeoutSeconds 超时秒数
     * @param timeoutHandler 超时处理器，返回替代流
     * @param <T>            流中元素的类型
     * @return 添加了超时处理的流
     */
    public static <T> Flux<T> withTimeout(
            Flux<T> flux,
            long timeoutSeconds,
            Function<Throwable, Flux<T>> timeoutHandler) {

        return flux
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .onErrorResume(timeoutHandler);
    }

    /**
     * 串联两个流，第二个流在第一个流完成后开始
     *
     * @param first  第一个流
     * @param second 第二个流
     * @param <T>    流中元素的类型
     * @return 串联后的流
     */
    public static <T> Flux<T> concat(Flux<T> first, Flux<T> second) {
        return first.concatWith(second);
    }

    /**
     * 串联流与单个元素，元素在流完成后发出
     *
     * @param flux      原始流
     * @param finalItem 最终元素
     * @param <T>       流中元素的类型
     * @return 串联后的流
     */
    public static <T> Flux<T> concatWithFinalItem(Flux<T> flux, T finalItem) {
        return flux.concatWith(Mono.just(finalItem));
    }
}