package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketManager;

public class UpdateBucketCommandTest {

	private final BucketManager buckets = mock(BucketManager.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new UpdateBucketCommand.Handler(buckets));

	@Test
	public void test() {

		Identity principal = new Identity();
		Bucket from = new Bucket();
		from.setLabel("Test Bucket");
		from.setVersion(1L);
		Bucket to = from.copy();
		to.setLabel("Real Bucket");

		Command command = new UpdateBucketCommand(principal, from, to);
		registry.execute(command);
		verify(buckets).update(to);
		reset(buckets);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(buckets).update(from);
		reset(buckets);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(buckets).update(to);
		reset(buckets);
	}
}
