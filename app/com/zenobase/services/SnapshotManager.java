package com.zenobase.services;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.snapshot.GetSnapshotResponse;
import org.opensearch.client.opensearch.snapshot.SnapshotInfo;
import org.joda.time.DateTime;
import play.Logger;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.PartialList;

public class SnapshotManager {

	private final OpenSearchClient client;
	private final String repositoryName;

	public SnapshotManager(OpenSearchClient client, String repositoryName) {
		this.client = client;
		this.repositoryName = repositoryName;
	}

	public PartialList<Snapshot> findAll(int offset, int limit) {
		try {
			List<Snapshot> snapshots = Lists.newArrayListWithExpectedSize(limit);
			GetSnapshotResponse response = client.snapshot().get(g -> g
				.repository(repositoryName)
				.snapshot("*")
			);
			List<SnapshotInfo> infos = Lists.reverse(response.snapshots());
			for (int i = offset; i < offset + limit && i < infos.size(); ++i) {
				snapshots.add(new Snapshot(infos.get(i)));
			}
			return DefaultPartialList.of(snapshots, response.snapshots().size());
		} catch (OpenSearchException e) {
			if (e.error().type().equals("snapshot_missing_exception")) {
				return DefaultPartialList.of(Lists.newArrayList(), 0);
			}
			throw e;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void snapshot() {
		String snapshotId = String.valueOf(DateTime.now().getMillis() / 1000);
		Logger.info("Creating snapshot {}...", snapshotId);
		try {
			client.snapshot().create(c -> c
				.repository(repositoryName)
				.snapshot(snapshotId)
				.waitForCompletion(true)
			);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean delete(String snapshotId) {
		Logger.info("Deleting snapshot {}...", snapshotId);
		try {
			return client.snapshot().delete(d -> d
				.repository(repositoryName)
				.snapshot(snapshotId)
			).acknowledged();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
