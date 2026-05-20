package com.ruijie.uspportal.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
/**
 * OrgSnapshot 类。
 */
public class OrgSnapshot {

    private String orgCode;

    private String orgName;
}
