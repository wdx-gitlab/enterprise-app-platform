package com.ruijie.uspportal.eventbus.service.impl;

import com.ruijie.uspportal.eventbus.service.EventCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
/**
 * Outbox 调度任务。
 *
 * <p>通过定时任务周期性触发待派发事件批处理。</p>
 */
public class UspOutboxDispatcher {

    private final EventCenterService eventCenterService;

    @Autowired
    public UspOutboxDispatcher(EventCenterService eventCenterService) {
        this.eventCenterService = eventCenterService;
    }

    @Scheduled(fixedDelayString = "${usp.portal.eventbus.dispatcher.fixed-delay-ms:3000}")
    /**
     * 定时派发当前待处理的出站事件。
     */
    public void dispatchPendingEvents() {
        try {
            eventCenterService.dispatchPendingEvents();
        } catch (Exception ex) {
            log.error("Dispatch pending events failed", ex);
        }
    }
}
