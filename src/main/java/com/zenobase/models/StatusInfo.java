package com.zenobase.models;

import com.zenobase.json.BooleanField;
import com.zenobase.json.DomainNode;

public class StatusInfo extends DomainNode {

	private static final BooleanField READ_ONLY = new BooleanField("read_only");
	private static final BooleanField SCHEDULER_DISABLED = new BooleanField("scheduler_disabled");

	public StatusInfo(boolean readOnly, boolean schedulerDisabled) {
		if (readOnly) {
			setValue(READ_ONLY, true);
		}
		if (schedulerDisabled) {
			setValue(SCHEDULER_DISABLED, true);
		}
	}

	public boolean isReadOnly() {
		return getValue(READ_ONLY, Boolean.FALSE);
	}

	public boolean isSchedulerDisabled() {
		return getValue(SCHEDULER_DISABLED, Boolean.FALSE);
	}
}
