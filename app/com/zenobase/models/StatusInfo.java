package com.zenobase.models;

import org.opensearch.client.opensearch._types.HealthStatus;

import com.zenobase.json.BooleanField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.IntegerField;
import com.zenobase.json.LongField;
import com.zenobase.json.TokenField;

public class StatusInfo extends DomainNode {

	private static final LongField COUNT = new LongField("count");
	private static final TokenField HEALTH = new TokenField("health");
	private static final IntegerField NODES_DATA = new IntegerField("data_nodes");
	private static final IntegerField NODES_WEB = new IntegerField("web_nodes");
	private static final BooleanField READ_ONLY = new BooleanField("read_only");
	private static final BooleanField SCHEDULER_DISABLED = new BooleanField("scheduler_disabled");

	public StatusInfo(boolean readOnly) {
		setValue(READ_ONLY, readOnly);
	}

	public StatusInfo(long count, HealthStatus health, int dataNodes, int webNodes, boolean readOnly, boolean schedularDisabled) {
		setValue(COUNT, count);
		setValue(HEALTH, health.jsonValue());
		setValue(NODES_DATA, dataNodes);
		setValue(NODES_WEB, webNodes);
		if (readOnly) {
			setValue(READ_ONLY, true);
		}
		if (schedularDisabled) {
			setValue(SCHEDULER_DISABLED, true);
		}
	}

	public long getCount() {
		return getValue(COUNT);
	}

	public HealthStatus getHealth() {
		String value = getValue(HEALTH);
		for (HealthStatus status : HealthStatus.values()) {
			if (status.jsonValue().equals(value)) {
				return status;
			}
		}
		return HealthStatus.Red;
	}

	public int getNodes() {
		return getValue(NODES_DATA);
	}

	public boolean isReadOnly() {
		return getValue(READ_ONLY, Boolean.FALSE);
	}

	public boolean isSchedulerDisabled() {
		return getValue(SCHEDULER_DISABLED, Boolean.FALSE);
	}
}
