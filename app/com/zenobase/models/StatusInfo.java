package com.zenobase.models;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

	public StatusInfo(ObjectNode node) {
		super(node);
	}

	public StatusInfo(boolean readOnly) {
		setValue(READ_ONLY, readOnly);
	}

	public StatusInfo(long count, ClusterHealthStatus health, int dataNodes, int webNodes, boolean readOnly, boolean schedularDisabled) {
		setValue(COUNT, count);
		setValue(HEALTH, health.toString());
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

	public ClusterHealthStatus getHealth() {
		return ClusterHealthStatus.valueOf(getValue(HEALTH));
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
