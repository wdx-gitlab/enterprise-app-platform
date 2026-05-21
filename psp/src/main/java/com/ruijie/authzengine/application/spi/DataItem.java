package com.ruijie.authzengine.application.spi;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 治理数据通用载体，用于 Shadow Mode 下宿主适配器返回主体或资源数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataItem {

    /** 主体/资源唯一标识 */
    private String id;

    /** 编码（如 userCode、menuCode 等） */
    private String code;

    /** 显示名称 */
    private String name;

    /** 状态（ENABLED / DISABLED 等） */
    private String status;

    /** 扩展属性，由宿主自行填充 */
    private Map<String, Object> attributes;
}
