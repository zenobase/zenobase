package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketManager;

public class CreateDeleteAndRestoreBucketCommandTest {

	private final BucketManager buckets = mock(BucketManager.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new CreateBucketCommand.Handler(buckets),
		new DeleteBucketCommand.Handler(buckets),
		new RestoreBucketCommand.Handler(buckets));

	@Test
	public void test() {

		Identity principal = new Identity();
		Bucket bucket = new Bucket();
		bucket.setLabel("Test Bucket");

		Command command = new CreateBucketCommand(principal, bucket);
		registry.execute(command);
		verify(buckets).store(bucket, true);
		reset(buckets);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(buckets).deleteBucket(bucket.getId());
		reset(buckets);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(buckets).store(bucket, false);
		reset(buckets);

		Command unredo = redo.reverse(principal);
		registry.execute(unredo);
		verify(buckets).deleteBucket(bucket.getId());
		reset(buckets);
	}
}
