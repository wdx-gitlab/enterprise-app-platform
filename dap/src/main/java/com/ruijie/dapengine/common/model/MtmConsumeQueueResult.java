package com.ruijie.dapengine.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MtmConsumeQueueResult 用于封装从消息队列消费结果的描述、状态和数据。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MtmConsumeQueueResult {
	private String desc;
	private String status;
	private String data;
}