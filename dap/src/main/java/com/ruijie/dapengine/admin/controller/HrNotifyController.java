package com.ruijie.dapengine.admin.controller;

import com.ruijie.dapengine.admin.service.QueueMessageHandleService;
import com.ruijie.dapengine.common.model.MtmConsumeQueueDTO;
import com.ruijie.dapengine.common.model.MtmConsumeQueueResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HR 系统员工变动通知接收控制器。
 *
 * <p>提供一个 Webhook 端点，供 HR 系统在员工发生入离调转时主动推送事件通知
 * </p>
 */
@RestController
@RequestMapping("/dap-engine/admin/osds-staff")
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
public class HrNotifyController {

    private final QueueMessageHandleService queueMessageHandleService;

    public HrNotifyController(QueueMessageHandleService queueMessageHandleService) {
        this.queueMessageHandleService = queueMessageHandleService;
    }

    /**
     * 处理员工消息
     */
    @PostMapping("/notify")
    public MtmConsumeQueueResult notify(@RequestBody MtmConsumeQueueDTO dto) {
        try {
            if (dto == null || dto.getMsgContent() == null || dto.getMsgContent().trim().isEmpty()) {
                return new MtmConsumeQueueResult("msgContent 不能为空", "500", "");
            }
            queueMessageHandleService.updateStaffInfo(dto.getMsgContent());
            return new MtmConsumeQueueResult("", "200", "");
        } catch (Exception ex) {
            String sb = "【消息内容：" + dto.getMsgContent() + "】\n" +
                    "【Message：" + ex.getMessage() + "】\n";
            String msg = "处理员工消息[" + dto.getMessageCode() + "]失败:" + sb;
            return new MtmConsumeQueueResult("", "500", msg);
        }
    }
}
