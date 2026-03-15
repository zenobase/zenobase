package com.zenobase.services;

public interface Bus {

	boolean isReadOnly();

	void setReadOnly(boolean readOnly);

	boolean isSchedulerDisabled();

	void setSchedulerDisabled(boolean schedulerDisabled);

	void close();

}