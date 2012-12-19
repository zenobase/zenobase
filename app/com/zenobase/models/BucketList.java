package com.zenobase.models;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import com.google.common.primitives.Ints;

import com.zenobase.common.PartialList;
import com.zenobase.json.LongField;
import com.zenobase.json.Nodes;
import com.zenobase.services.EventRepository;

public class BucketList extends PartialList<Bucket> {

	private static final LongField SIZE = new LongField("size");

	private final EventRepository repository;

	public BucketList(Iterable<Bucket> elements, long size, EventRepository repository) {
		super(elements, size);
		this.repository = repository;
	}

    public ObjectNode toJson() {
    	ObjectNode resultNode = Nodes.newObject();
    	TOTAL.setValue(resultNode, Ints.checkedCast(size()));
    	ArrayNode bucketsNode = resultNode.putArray("buckets");
    	for (Bucket bucket : getElements()) {
    		ObjectNode bucketNode = bucket.toJson();
    		SIZE.setValue(bucketNode, repository.getSize(bucket.getId()));
    		bucketsNode.add(bucketNode);
    	}
    	return resultNode;
    }
}
