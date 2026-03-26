package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketRepository;

public class CreateDeleteAndRestoreBucketCommandTest {

	private final BucketRepository repository = mock(BucketRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
			new CreateBucketCommand.Handler(repository),
			new DeleteBucketCommand.Handler(repository),
			new RestoreBucketCommand.Handler(repository));

	@Test
	public void test() {

		Identity principal = new Identity();
		Bucket bucket = new Bucket();
		bucket.setLabel("Test Bucket");

		Command command = new CreateBucketCommand(principal, bucket);
		registry.execute(command);
		verify(repository).store(bucket, command.getTimestamp());
		reset(repository);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(repository).delete(bucket.getId());
		reset(repository);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(repository).store(bucket, redo.getTimestamp());
		reset(repository);

		Command unredo = redo.reverse(principal);
		registry.execute(unredo);
		verify(repository).delete(bucket.getId());
		reset(repository);
	}
}
