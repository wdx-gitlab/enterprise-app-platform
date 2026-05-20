package com.ruijie.uspportal.eventbus.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 统一事件载荷模型。
 *
 * <p>用于在事件中心内部封装标准化事件元数据以及业务载荷内容。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UspEventPayload {

    private String eventId;

    private String topic;

    private String payloadJson;

    private String payloadClass;

    private String sourceSystem;

    private String sourceModule;

    private String businessKey;

    private LocalDateTime publishedTime;

    /**
     * 将 JSON 载荷反序列化为目标类型对象。
     *
     * @param objectMapper Jackson 对象映射器
     * @param targetType 目标类型
     * @param <T> 返回类型
     * @return 反序列化后的载荷对象
     */
    public <T> T readPayload(ObjectMapper objectMapper, Class<T> targetType) {
        try {
            return objectMapper.readValue(payloadJson, targetType);
        } catch (IOException ex) {
            throw new IllegalArgumentException("事件反序列化失败: " + ex.getMessage(), ex);
        }
    }
}
