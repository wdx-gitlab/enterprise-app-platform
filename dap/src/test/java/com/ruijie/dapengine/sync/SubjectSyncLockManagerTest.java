package com.ruijie.dapengine.sync;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * SubjectSyncLockManager 单元测试（JUnit4）。
 * 覆盖：同 Subject 串行（持锁期间第二次 tryLock(0) 返回 false）。
 * 覆盖：不同 Subject 并行（两个 Subject 同时持锁互不影响）。
 * 覆盖：unlock 后同 Subject 可再次 tryLock。
 */
@RunWith(JUnit4.class)
public class SubjectSyncLockManagerTest {

    private SubjectSyncLockManager lockManager;

    @Before
    public void setUp() {
        lockManager = new SubjectSyncLockManager();
    }

    @Test
    public void should_acquire_lock_on_first_try_lock() {
        boolean acquired = lockManager.tryLock("CUST", 1);
        assertThat(acquired).isTrue();
        lockManager.unlock("CUST");
    }

    @Test
    public void should_return_false_when_same_subject_locked_and_timeout_zero() throws Exception {
        // 主线程获取锁
        lockManager.tryLock("CUST", 1);

        AtomicBoolean secondResult = new AtomicBoolean(true);
        CountDownLatch latch = new CountDownLatch(1);

        Thread second = new Thread(() -> {
            secondResult.set(lockManager.tryLock("CUST", 0)); // 超时 0s
            latch.countDown();
        });
        second.start();
        latch.await(2, TimeUnit.SECONDS);

        assertThat(secondResult.get()).isFalse();
        lockManager.unlock("CUST");
    }

    @Test
    public void should_allow_different_subjects_to_lock_concurrently() throws Exception {
        AtomicBoolean custAcquired = new AtomicBoolean(false);
        AtomicBoolean productAcquired = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(2);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(() -> {
            custAcquired.set(lockManager.tryLock("CUST", 1));
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            lockManager.unlock("CUST");
            latch.countDown();
        });
        pool.submit(() -> {
            productAcquired.set(lockManager.tryLock("PRODUCT", 1));
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            lockManager.unlock("PRODUCT");
            latch.countDown();
        });

        latch.await(3, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(custAcquired.get()).isTrue();
        assertThat(productAcquired.get()).isTrue();
    }

    @Test
    public void should_allow_relock_after_unlock() {
        boolean first = lockManager.tryLock("CUST", 1);
        assertThat(first).isTrue();
        lockManager.unlock("CUST");

        boolean second = lockManager.tryLock("CUST", 1);
        assertThat(second).isTrue();
        lockManager.unlock("CUST");
    }

    @Test
    public void should_not_throw_when_unlock_without_lock() {
        // unlock 在未持锁时不抛异常，只记录 WARN
        assertThatCode(() -> lockManager.unlock("NONEXISTENT"))
                .doesNotThrowAnyException();
    }

    @Test
    public void should_not_throw_when_non_owner_unlocks() throws Exception {
        // 主线程加锁，另一线程尝试 unlock（非持锁线程）
        lockManager.tryLock("CUST", 1);

        AtomicBoolean threw = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        Thread other = new Thread(() -> {
            try {
                lockManager.unlock("CUST");
            } catch (Exception e) {
                threw.set(true);
            } finally {
                latch.countDown();
            }
        });
        other.start();
        latch.await(1, TimeUnit.SECONDS);

        assertThat(threw.get()).isFalse(); // 仅记录 WARN 日志，不抛异常
        lockManager.unlock("CUST");
    }
}
