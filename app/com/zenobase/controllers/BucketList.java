package com.zenobase.controllers;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import com.google.common.primitives.Ints;

import com.zenobase.common.PartialList;
import com.zenobase.json.LongField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.services.BucketManager;

public class BucketList extends PartialList<Bucket> {

	private static final LongField SIZE = new LongField("size");

	private final BucketManager manager;

	public BucketList(Iterable<Bucket> elements, long size, BucketManager manager) {
		super(elements, size);
		this.manager = manager;
	}

    public ObjectNode toJson() {
    	ObjectNode resultNode = Nodes.newObject();
    	TOTAL.setValue(resultNode, Ints.checkedCast(size()));
    	ArrayNode bucketsNode = resultNode.putArray("buckets");
    	for (Bucket bucket : getElements()) {
    		ObjectNode bucketNode = bucket.toJson();
    		SIZE.setValue(bucketNode, manager.getSize(bucket.getId()));
    		bucketsNode.add(bucketNode);
    	}
    	return resultNode;
    }
}
