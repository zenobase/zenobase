package com.zenobase.services;

public class LocalBus implements Bus {

	private boolean readOnly = false;

	@Override
	public boolean isMaster() {
		return true;
	}

	@Override
	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	@Override
	public int count() {
		return 1;
	}

	@Override
	public void close() {

	}
}
