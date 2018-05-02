package com.zenobase.services;

public interface Bus {

	boolean isMaster();

	boolean isReadOnly();

	void setReadOnly(boolean readOnly);

	boolean isSchedulerDisabled();

	void setSchedulerDisabled(boolean schedulerDisabled);

	int count();

	void close();

}