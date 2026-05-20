package com.ruijie.dapengine.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 主题级同步串行锁管理器。
 * <p>
 * 同一 Subject 并发触发同步时，后续调用会等待锁释放（最多 {@code timeoutSeconds} 秒）。
 * 不同 Subject 之间完全并行，互不阻塞。
 * </p>
 */
public class SubjectSyncLockManager {

    private static final Logger log = LoggerFactory.getLogger(SubjectSyncLockManager.class);

    /** Subject Code → ReentrantLock 的映射，Lock 实例按需创建并永久保留（总数等于 Subject 数）。 */
    final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /**
     * 尝试获取指定 Subject 的同步锁，等待最多 {@code timeoutSeconds} 秒。
     *
     * @param subjectCode    Subject code
     * @param timeoutSeconds 最大等待秒数
     * @return {@code true} 表示成功获取锁，{@code false} 表示超时未获取
     */
    public boolean tryLock(String subjectCode, int timeoutSeconds) {
        ReentrantLock lock = lockMap.computeIfAbsent(subjectCode, k -> new ReentrantLock(true));
        try {
            boolean acquired = lock.tryLock(timeoutSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("[SubjectSyncLock] tryLock timeout after {}s for subject={}", timeoutSeconds, subjectCode);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[SubjectSyncLock] tryLock interrupted for subject={}", subjectCode);
            return false;
        }
    }

    /**
     * 释放指定 Subject 的同步锁。
     * <p>调用方必须是锁持有者，否则抛出 {@link IllegalMonitorStateException}（ReentrantLock 语义）。</p>
     *
     * @param subjectCode Subject code
     */
    public void unlock(String subjectCode) {
        ReentrantLock lock = lockMap.get(subjectCode);
        if (lock == null) {
            log.warn("[SubjectSyncLock] unlock called but no lock found for subject={}", subjectCode);
            return;
        }
        if (!lock.isHeldByCurrentThread()) {
            log.warn("[SubjectSyncLock] unlock called by non-owner thread for subject={}", subjectCode);
            return;
        }
        lock.unlock();
    }
}
