package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.Ints;

import com.zenobase.common.PartialList;
import com.zenobase.json.LazyList;
import com.zenobase.json.Nodes;
import com.zenobase.services.Snapshot;

public class SnapshotList extends LazyList<Snapshot> {

	public SnapshotList(PartialList<ObjectNode> nodes) {
		super(nodes);
	}

	@Override
	protected Snapshot toObject(ObjectNode node) {
		return new Snapshot(node);
	}

	public static ObjectNode toJson(PartialList<Snapshot> snapshots) {
		ObjectNode resultNode = Nodes.newObject();
		TOTAL.setValue(resultNode, Ints.checkedCast(snapshots.getTotal()));
		ArrayNode snapshotsNode = resultNode.putArray("snapshots");
		for (Snapshot snapshot : snapshots) {
			snapshotsNode.add(snapshot.toJson());
		}
		return resultNode;
	}
}
