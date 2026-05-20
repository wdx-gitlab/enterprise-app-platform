package com.ruijie.dapengine.sync;

import com.ruijie.dapengine.admin.service.MetadataConfigService;
import com.ruijie.dapengine.admin.service.SchemaStatusService;
import com.ruijie.dapengine.common.enums.SchemaStatus;
import com.ruijie.dapengine.common.model.SubjectDTO;
import com.ruijie.dapengine.entity.SyncConfigEntity;
import com.ruijie.dapengine.repository.SubjectRepository;
import com.ruijie.dapengine.repository.SyncConfigRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SyncSchedulerImpl 单元测试（JUnit4 + Mockito）。
 * 覆盖：init() 对 SCHEDULE+APPLIED 主题注册任务，对 PENDING 主题不注册。
 * 覆盖：EVENT 模式不注册；reschedule() 取消旧 Future 并注册新 cron。
 * 覆盖：status=0 时 reschedule() 只 cancel 不注册。
 */
@RunWith(MockitoJUnitRunner.class)
public class SyncSchedulerImplTest {

    @Mock private SubjectRepository subjectRepository;
    @Mock private SyncConfigRepository syncConfigRepository;
    @Mock private MetadataConfigService metadataConfigService;
    @Mock private SchemaStatusService schemaStatusService;
    @Mock private SyncExecutor syncExecutor;
    @Mock private TaskScheduler taskScheduler;
    @Mock private ScheduledFuture<?> scheduledFuture;

    private SyncSchedulerImpl scheduler;

    @Before
    public void setUp() {
        scheduler = new SyncSchedulerImpl(subjectRepository, syncConfigRepository,
                metadataConfigService, schemaStatusService, syncExecutor, taskScheduler);
    }

    private SubjectDTO makeSubject(String code) {
        SubjectDTO dto = new SubjectDTO();
        dto.setCode(code);
        dto.setName(code);
        dto.setStatus(1);
        dto.setIsDelete(0);
        return dto;
    }

    private SyncConfigEntity makeScheduleConfig(String code) {
        SyncConfigEntity e = new SyncConfigEntity();
        e.setSubjectCode(code);
        e.setSyncMode("SCHEDULE");
        e.setCronExpr("0 */5 * * * ?");
        e.setStatus(1);
        return e;
    }

    @SuppressWarnings("unchecked")
    private void mockSchedule() {
        when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
                .thenReturn((ScheduledFuture) scheduledFuture);
    }

    @Test
    public void should_register_schedule_task_for_applied_schedule_subject() {
        mockSchedule();
        when(subjectRepository.listActive()).thenReturn(Collections.singletonList(makeSubject("CUST")));
        when(metadataConfigService.getActiveFieldDTOs("CUST")).thenReturn(Collections.emptyList());
        when(schemaStatusService.computeStatus(eq("CUST"), anyList())).thenReturn(SchemaStatus.APPLIED);
        when(syncConfigRepository.findBySubjectCode("CUST")).thenReturn(makeScheduleConfig("CUST"));

        scheduler.init();

        assertThat(scheduler.futureMap).containsKey("CUST");
        verify(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    public void should_not_register_task_for_pending_subject() {
        when(subjectRepository.listActive()).thenReturn(Collections.singletonList(makeSubject("CUST")));
        when(metadataConfigService.getActiveFieldDTOs("CUST")).thenReturn(Collections.emptyList());
        when(schemaStatusService.computeStatus(eq("CUST"), anyList())).thenReturn(SchemaStatus.PENDING);

        scheduler.init();

        assertThat(scheduler.futureMap).doesNotContainKey("CUST");
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    public void should_not_register_task_for_event_mode_subject() {
        when(subjectRepository.listActive()).thenReturn(Collections.singletonList(makeSubject("CUST")));
        when(metadataConfigService.getActiveFieldDTOs("CUST")).thenReturn(Collections.emptyList());
        when(schemaStatusService.computeStatus(eq("CUST"), anyList())).thenReturn(SchemaStatus.APPLIED);
        SyncConfigEntity eventConfig = makeScheduleConfig("CUST");
        eventConfig.setSyncMode("EVENT");
        when(syncConfigRepository.findBySubjectCode("CUST")).thenReturn(eventConfig);

        scheduler.init();

        assertThat(scheduler.futureMap).doesNotContainKey("CUST");
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    public void should_register_multiple_subjects_in_init() {
        mockSchedule();
        when(subjectRepository.listActive()).thenReturn(
                Arrays.asList(makeSubject("CUST"), makeSubject("PRODUCT")));
        when(metadataConfigService.getActiveFieldDTOs(anyString())).thenReturn(Collections.emptyList());
        when(schemaStatusService.computeStatus(anyString(), anyList())).thenReturn(SchemaStatus.APPLIED);
        when(syncConfigRepository.findBySubjectCode("CUST")).thenReturn(makeScheduleConfig("CUST"));
        when(syncConfigRepository.findBySubjectCode("PRODUCT")).thenReturn(makeScheduleConfig("PRODUCT"));

        scheduler.init();

        assertThat(scheduler.futureMap).containsKeys("CUST", "PRODUCT");
        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_cancel_old_and_register_new_on_reschedule() {
        // 先模拟已有一个已注册的 Future
        mockSchedule();
        scheduler.futureMap.put("CUST", (ScheduledFuture) scheduledFuture);

        when(metadataConfigService.getActiveFieldDTOs("CUST")).thenReturn(Collections.emptyList());
        when(schemaStatusService.computeStatus(eq("CUST"), anyList())).thenReturn(SchemaStatus.APPLIED);
        when(syncConfigRepository.findBySubjectCode("CUST")).thenReturn(makeScheduleConfig("CUST"));

        scheduler.reschedule("CUST");

        // 旧 Future 应被 cancel
        verify(scheduledFuture).cancel(false);
        // 新 Future 应被注册
        assertThat(scheduler.futureMap).containsKey("CUST");
        verify(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_only_cancel_when_status_is_disabled() {
        scheduler.futureMap.put("CUST", (ScheduledFuture) scheduledFuture);

        SyncConfigEntity disabledConfig = makeScheduleConfig("CUST");
        disabledConfig.setStatus(0);
        when(syncConfigRepository.findBySubjectCode("CUST")).thenReturn(disabledConfig);

        scheduler.reschedule("CUST");

        verify(scheduledFuture).cancel(false);
        // 不注册新任务
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
        assertThat(scheduler.futureMap).doesNotContainKey("CUST");
    }
}
