package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.common.PartialList;
import com.zenobase.json.LazyList;

public class ExternalBucketGrantList extends LazyList<ExternalBucketGrant> {

	public ExternalBucketGrantList(PartialList<ObjectNode> nodes) {
		super(nodes);
	}

	@Override
	protected ExternalBucketGrant toObject(ObjectNode node) {
		return new ExternalBucketGrant(node);
	}
}
