package com.zenobase.services;

import java.util.List;

import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.joda.time.DateTime;
import play.Logger;
import com.google.common.collect.Lists;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.PartialList;

public class SnapshotManager {

	private final Client client;
	private final String repositoryName;

	public SnapshotManager(Client client, String repositoryName) {
		this.client = client;
		this.repositoryName = repositoryName;
	}

	public PartialList<Snapshot> findAll(int offset, int limit) {
		List<Snapshot> snapshots = Lists.newArrayListWithExpectedSize(limit);
		GetSnapshotsResponse response = client.admin().cluster().prepareGetSnapshots(repositoryName).get();
		List<SnapshotInfo> infos = response.getSnapshots().reverse();
		for (int i = offset; i < offset + limit && i < infos.size(); ++i) {
			snapshots.add(new Snapshot(infos.get(i)));
		}
		return DefaultPartialList.of(snapshots, response.getSnapshots().size());
	}

	public void snapshot() {
		String snapshotId = String.valueOf(DateTime.now().getMillis() / 1000);
		Logger.info("Creating snapshot: " + snapshotId);
		client.admin().cluster().prepareCreateSnapshot(repositoryName, snapshotId).setWaitForCompletion(true).get();
	}

	public boolean delete(String snapshotId) {
		Logger.info("Deleting snapshot: " + snapshotId);
		return client.admin().cluster().prepareDeleteSnapshot(repositoryName, snapshotId).get().isAcknowledged();
	}
}
