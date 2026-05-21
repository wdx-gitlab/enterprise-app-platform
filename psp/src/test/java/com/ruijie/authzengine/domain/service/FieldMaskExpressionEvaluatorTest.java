package com.ruijie.authzengine.domain.service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FieldMaskExpressionEvaluatorTest {

    @Test
    void shouldEvaluateMiddleMaskScript() {
        FieldMaskExpressionEvaluator evaluator = new FieldMaskExpressionEvaluator(500L);

        String masked = evaluator.evaluate(
            "#originalValue.length() > 6 ? #originalValue.substring(0,3) + '***' + #originalValue.substring(#originalValue.length()-3) : '***'",
            "DEPT_PACKAGE"
        );

        Assertions.assertEquals("DEP***AGE", masked);
    }

    @Test
    void shouldEvaluateMaskScriptWithFieldNameAndParam() {
        FieldMaskExpressionEvaluator evaluator = new FieldMaskExpressionEvaluator(500L);

        String masked = evaluator.evaluate(
            "#fieldName == 'mobilePhone' ? #originalValue.substring(0, #param['keepHead']) + '****' : #originalValue",
            "13812345678",
            "mobilePhone",
            Collections.singletonMap("keepHead", 3)
        );

        Assertions.assertEquals("138****", masked);
    }

    @Test
    void shouldCancelFutureWhenMaskEvaluationTimesOut() {
        TimeoutAwareExecutorService executorService = new TimeoutAwareExecutorService();
        FieldMaskExpressionEvaluator evaluator = new FieldMaskExpressionEvaluator(1L, executorService);

        String masked = evaluator.evaluate("#originalValue", "13812345678");

        Assertions.assertEquals(FieldMaskExpressionEvaluator.MASK_ERROR_PLACEHOLDER, masked);
        Assertions.assertTrue(executorService.cancelled.get(), "超时时应取消后台任务");
    }

    private static class TimeoutAwareExecutorService extends AbstractExecutorService {

        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return new FutureTask<T>(task) {
                @Override
                public T get(long timeout, TimeUnit unit) throws TimeoutException {
                    throw new TimeoutException("timeout");
                }

                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    cancelled.set(true);
                    return true;
                }
            };
        }
    }
}