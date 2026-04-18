package com.zenobase.tasks.google;

import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Resource;

public record DataStream(String id, String dataType, @Nullable Resource source) {
	public DataStream(String id, String dataType, @Nullable Resource source) {
		this.id = Preconditions.checkNotNull(id);
		this.dataType = Preconditions.checkNotNull(dataType);
		this.source = source;
	}

	@Override
	public String toString() {
		return id;
	}
}
