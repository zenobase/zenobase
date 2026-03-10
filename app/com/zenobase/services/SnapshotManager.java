package com.zenobase.services;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import org.opensearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest;
import org.opensearch.action.admin.cluster.snapshots.delete.DeleteSnapshotRequest;
import org.opensearch.action.admin.cluster.snapshots.get.GetSnapshotsRequest;
import org.opensearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.snapshots.SnapshotInfo;
import org.joda.time.DateTime;
import play.Logger;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.PartialList;

public class SnapshotManager {

	private final RestHighLevelClient client;
	private final String repositoryName;

	public SnapshotManager(RestHighLevelClient client, String repositoryName) {
		this.client = client;
		this.repositoryName = repositoryName;
	}

	public PartialList<Snapshot> findAll(int offset, int limit) {
		try {
			List<Snapshot> snapshots = Lists.newArrayListWithExpectedSize(limit);
			GetSnapshotsRequest request = new GetSnapshotsRequest(repositoryName);
			GetSnapshotsResponse response = client.snapshot().get(request, TypeInjectingInterceptor.OPTIONS);
			List<SnapshotInfo> infos = Lists.reverse(response.getSnapshots());
			for (int i = offset; i < offset + limit && i < infos.size(); ++i) {
				snapshots.add(new Snapshot(infos.get(i)));
			}
			return DefaultPartialList.of(snapshots, response.getSnapshots().size());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void snapshot() {
		String snapshotId = String.valueOf(DateTime.now().getMillis() / 1000);
		Logger.info("Creating snapshot {}...", snapshotId);
		try {
			CreateSnapshotRequest request = new CreateSnapshotRequest(repositoryName, snapshotId)
				.waitForCompletion(true);
			client.snapshot().create(request, TypeInjectingInterceptor.OPTIONS);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean delete(String snapshotId) {
		Logger.info("Deleting snapshot {}...", snapshotId);
		try {
			DeleteSnapshotRequest request = new DeleteSnapshotRequest(repositoryName, snapshotId);
			return client.snapshot().delete(request, TypeInjectingInterceptor.OPTIONS).isAcknowledged();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
