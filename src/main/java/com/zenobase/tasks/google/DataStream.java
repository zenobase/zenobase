package com.zenobase.tasks.google;

import java.util.Objects;

import com.google.common.base.Preconditions;

import com.zenobase.models.Resource;

public class DataStream {

	private final String id;
	private final String dataType;
	private final Resource source;

	public DataStream(String id, String dataType, Resource source) {
		this.id = Preconditions.checkNotNull(id);
		this.dataType = Preconditions.checkNotNull(dataType);
		this.source = source;
	}

	public String getId() {
		return id;
	}

	public String getDataType() {
		return dataType;
	}

	public Resource getSource() {
		return source;
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof DataStream && equals((DataStream) that);
	}

	private boolean equals(DataStream that) {
		return Objects.equals(that.id, id)
				&& Objects.equals(that.dataType, dataType)
				&& Objects.equals(that.source, source);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, dataType, source);
	}

	@Override
	public String toString() {
		return id;
	}
}
