package com.zenobase.services;

public class LocalBus implements Bus {

	private boolean readOnly;
	private boolean schedulerDisabled;

	@Override
	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	@Override
	public boolean isSchedulerDisabled() {
		return schedulerDisabled;
	}

	@Override
	public void setSchedulerDisabled(boolean schedulerDisabled) {
		this.schedulerDisabled = schedulerDisabled;
	}

	@Override
	public void close() {}
}
