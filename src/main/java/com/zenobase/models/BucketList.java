package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.Ints;
import com.zenobase.common.PartialList;
import com.zenobase.json.LazyList;
import com.zenobase.json.Nodes;

public class BucketList extends LazyList<Bucket> {

	public BucketList(PartialList<ObjectNode> nodes) {
		super(nodes);
	}

	@Override
	protected Bucket toObject(ObjectNode node) {
		return new Bucket(node);
	}

	public static ObjectNode toJson(PartialList<Bucket> buckets) {
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
