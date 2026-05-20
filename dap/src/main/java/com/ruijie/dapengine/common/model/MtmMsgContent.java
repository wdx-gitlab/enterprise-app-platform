package com.ruijie.dapengine.common.model;

import lombok.Data;

import java.util.List;

@Data
public class MtmMsgContent {
	/**
	 * 事件类型
	 */
	private String eventType;

	private List<MtmMsgContentValue> parameter;
}