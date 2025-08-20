package com.mt.agent.test;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
public class FlowableDemo {

    // 模拟流式处理函数A
    static Flowable<String> functionA() {
        return Flowable.create(emitter -> {
            try {
                emitter.onNext("▶ 函数A步骤1");
                emitter.onNext("▶ 函数A步骤2");
                emitter.onNext("✓ 函数A最终结果");
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        }, BackpressureStrategy.BUFFER);
    }

    public static void main(String[] args) {
        Flowable.create(emitter -> {
                    emitter.onNext("▶ 开始主流程");

                    functionA().subscribe(
                            item -> emitter.onNext("[A过程] " + item),
                            emitter::onError,
                            () -> {
                                emitter.onNext("✓ 主流程完成");
                                emitter.onComplete();
                            }
                    );

                }, BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe(
                        System.out::println,
                        Throwable::printStackTrace,
                        () -> System.out.println("全流程结束")
                );

        try { Thread.sleep(3000); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }
}
