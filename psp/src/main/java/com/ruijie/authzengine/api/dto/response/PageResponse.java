package com.ruijie.authzengine.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 治理分页响应。
 *
 * @param <T> 记录类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "治理分页响应")
public class PageResponse<T> {

    @Schema(description = "页码", example = "1")
    private int pageNo;

    @Schema(description = "分页大小", example = "20")
    private int pageSize;

    @Schema(description = "总记录数", example = "1")
    private long total;

    @Schema(description = "分页记录")
    private List<T> records;
}