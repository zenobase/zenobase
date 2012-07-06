package com.zenobase.models;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;

import com.zenobase.json.DomainNode;
import com.zenobase.json.LongField;
import com.zenobase.json.TokenField;

public class StatusInfo extends DomainNode {

	private static final LongField COUNT = new LongField("count");
	private static final TokenField HEALTH = new TokenField("health");

	public StatusInfo(long count, ClusterHealthStatus status) {
		setValue(COUNT, count);
		setValue(HEALTH, status.toString());
	}

	public long getCount() {
		return getValue(COUNT);
	}

	public ClusterHealthStatus getHealth() {
		return ClusterHealthStatus.valueOf(getValue(HEALTH));
	}
}
