package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.Ints;

import com.zenobase.common.PartialList;
import com.zenobase.json.LazyList;
import com.zenobase.json.LongField;
import com.zenobase.json.Nodes;
import com.zenobase.services.EventRepository;

public class BucketList extends LazyList<Bucket> {

	public static final LongField SIZE = new LongField("size");

	public BucketList(PartialList<ObjectNode> nodes) {
		super(nodes);
	}

	@Override
	protected Bucket toObject(ObjectNode node) {
		return new Bucket(node);
	}

    public static ObjectNode toJson(PartialList<Bucket> buckets, EventRepository repository) {
    	ObjectNode resultNode = Nodes.newObject();
    	TOTAL.setValue(resultNode, Ints.checkedCast(buckets.getTotal()));
    	ArrayNode bucketsNode = resultNode.putArray("buckets");
    	for (Bucket bucket : buckets) {
    		ObjectNode bucketNode = bucket.toJson();
    		SIZE.setValue(bucketNode, repository.size(bucket.getId()));
    		bucketsNode.add(bucketNode);
    	}
    	return resultNode;
    }

    public static ObjectNode toJsonLabelsOnly(PartialList<Bucket> buckets) {
    	ObjectNode resultNode = Nodes.newObject();
    	TOTAL.setValue(resultNode, Ints.checkedCast(buckets.getTotal()));
    	ArrayNode bucketsNode = resultNode.putArray("buckets");
    	for (Bucket bucket : buckets) {
    		ObjectNode bucketNode = Nodes.newObject();
    		bucketNode.put("@id", bucket.getId());
    		bucketNode.put("label", bucket.getLabel());
    		bucketNode.put("aliases", bucket.getAliases().size());
    		bucketsNode.add(bucketNode);
    	}
    	return resultNode;
    }
}
