package com.ruijie.dapengine.sync;

import com.ruijie.dapengine.admin.service.MetadataConfigService;
import com.ruijie.dapengine.admin.service.SchemaStatusService;
import com.ruijie.dapengine.common.enums.SchemaStatus;
import com.ruijie.dapengine.common.model.FieldConfigDTO;
import com.ruijie.dapengine.common.model.SubjectDTO;
import com.ruijie.dapengine.entity.SyncConfigEntity;
import com.ruijie.dapengine.repository.SubjectRepository;
import com.ruijie.dapengine.repository.SyncConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态定时同步调度器实现。
 * <p>
 * 启动时扫描所有 SCHEDULE+APPLIED 主题并注册 Cron 任务；
 * 保存配置后调 reschedule() 重新注册。
 * </p>
 */
public class SyncSchedulerImpl implements SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncSchedulerImpl.class);

    private final SubjectRepository subjectRepository;
    private final SyncConfigRepository syncConfigRepository;
    private final MetadataConfigService metadataConfigService;
    private final SchemaStatusService schemaStatusService;
    private final SyncExecutor syncExecutor;
    private final TaskScheduler taskScheduler;

    /** 当前注册的定时任务 Future，供测试断言 */
    final ConcurrentHashMap<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();

    public SyncSchedulerImpl(SubjectRepository subjectRepository,
                              SyncConfigRepository syncConfigRepository,
                              MetadataConfigService metadataConfigService,
                              SchemaStatusService schemaStatusService,
                              SyncExecutor syncExecutor,
                              TaskScheduler taskScheduler) {
        this.subjectRepository = subjectRepository;
        this.syncConfigRepository = syncConfigRepository;
        this.metadataConfigService = metadataConfigService;
        this.schemaStatusService = schemaStatusService;
        this.syncExecutor = syncExecutor;
        this.taskScheduler = taskScheduler;
    }

    /**
     * 应用启动后自动恢复所有 SCHEDULE+APPLIED 主题的定时任务。
     */
    @PostConstruct
    public void init() {
        try {
            List<SubjectDTO> subjects = subjectRepository.listActive();
            for (SubjectDTO subject : subjects) {
                String code = subject.getCode();
                try {
                    List<FieldConfigDTO> fields = metadataConfigService.getActiveFieldDTOs(code);
                    SchemaStatus status = schemaStatusService.computeStatus(code, fields);
                    if (status != SchemaStatus.APPLIED) {
                        continue;
                    }
                    SyncConfigEntity config = syncConfigRepository.findBySubjectCode(code);
                    if (config == null || config.getStatus() != 1
                            || !"SCHEDULE".equals(config.getSyncMode())) {
                        continue;
                    }
                    scheduleSubject(code, config.getCronExpr());
                } catch (Exception e) {
                    log.warn("[SyncScheduler] init failed for subject={}: {}", code, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[SyncScheduler] init error: {}", e.getMessage(), e);
        }
    }

    /**
     * 重新注册指定 Subject 的定时同步任务。
     * 若配置为 EVENT 模式或 status=0，则只取消不注册。
     */
    @Override
    public void reschedule(String subjectCode) {
        cancelIfExists(subjectCode);
        try {
            SyncConfigEntity config = syncConfigRepository.findBySubjectCode(subjectCode);
            if (config == null || config.getStatus() != 1
                    || !"SCHEDULE".equals(config.getSyncMode())) {
                return;
            }
            List<FieldConfigDTO> fields = metadataConfigService.getActiveFieldDTOs(subjectCode);
            SchemaStatus status = schemaStatusService.computeStatus(subjectCode, fields);
            if (status != SchemaStatus.APPLIED) {
                log.info("[SyncScheduler] skip reschedule for {} (schema not APPLIED)", subjectCode);
                return;
            }
            scheduleSubject(subjectCode, config.getCronExpr());
        } catch (Exception e) {
            log.warn("[SyncScheduler] reschedule failed for subject={}: {}", subjectCode, e.getMessage());
        }
    }

    private void scheduleSubject(String subjectCode, String cronExpr) {
        if (cronExpr == null || cronExpr.isEmpty()) {
            log.warn("[SyncScheduler] no cronExpr for subject={}, skip", subjectCode);
            return;
        }
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> {
                    try {
                        syncExecutor.executeSync(subjectCode,
                                com.ruijie.dapengine.common.enums.TriggerAction.DELTA);
                    } catch (Exception e) {
                        log.error("[SyncScheduler] scheduled task error for {}: {}",
                                subjectCode, e.getMessage(), e);
                    }
                },
                new CronTrigger(cronExpr));
        futureMap.put(subjectCode, future);
        log.info("[SyncScheduler] scheduled subject={} cron={}", subjectCode, cronExpr);
    }

    private void cancelIfExists(String subjectCode) {
        ScheduledFuture<?> existing = futureMap.remove(subjectCode);
        if (existing != null) {
            existing.cancel(false);
            log.info("[SyncScheduler] cancelled scheduled task for subject={}", subjectCode);
        }
    }
}
