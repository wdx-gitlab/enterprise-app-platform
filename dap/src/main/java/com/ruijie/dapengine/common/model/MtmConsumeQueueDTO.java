package com.ruijie.dapengine.common.model;

import lombok.Data;

/**
 * 消费者接收消息对象
 */
@Data
public class MtmConsumeQueueDTO {

	private String messageCode;
	private String topicCode;
	private String topicName;
	private String channelCode;
	private String msgContent;
	private String subscriberCode;
}