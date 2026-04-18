package com.zenobase.jobs;

import static org.mockito.Mockito.*;

import com.zenobase.repositories.IndexManager;
import org.junit.jupiter.api.Test;

public class SnapshotJobTest {

	private final IndexManager indexManager = mock(IndexManager.class);
	private final SnapshotManager snapshots = mock(SnapshotManager.class);
	private final SnapshotJob job = new SnapshotJob(indexManager);

	@Test
	public void test() {
		when(indexManager.getSnapshotManager()).thenReturn(snapshots);

		job.run();

		verify(snapshots).snapshot();
	}
}
