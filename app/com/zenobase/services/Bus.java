package com.zenobase.services;

public interface Bus {

	boolean isMaster();

	boolean isReadOnly();

	void setReadOnly(boolean readOnly);

	int count();

	void close();

}