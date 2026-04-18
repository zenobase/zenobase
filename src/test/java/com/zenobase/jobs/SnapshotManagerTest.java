package com.zenobase.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.common.PartialList;
import com.zenobase.repositories.IndexManager;
import com.zenobase.repositories.OpenSearchTestSupport;
import org.junit.jupiter.api.Test;

public class SnapshotManagerTest extends OpenSearchTestSupport {

	@Test
	public void testLocalSnapshotLifecycle() {
		IndexManager local = new IndexManager(getClientFactory(), "", "us-east-1", "");
		SnapshotManager snapshots = local.getSnapshotManager();

		assertThat(snapshots.findAll(0, 10)).as("no snapshots initially").isEmpty();

		snapshots.snapshot();

		PartialList<Snapshot> after = snapshots.findAll(0, 10);
		assertThat(after).hasSize(1);
		assertThat(after.getTotal()).isEqualTo(1L);
		Snapshot created = after.get(0);
		assertThat(created.toJson().get("state").asText()).isEqualTo("success");

		assertThat(snapshots.delete(created.getId())).isTrue();
		assertThat(snapshots.findAll(0, 10)).as("snapshot cleaned up").isEmpty();
	}
}
