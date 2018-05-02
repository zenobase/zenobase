package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.models.Alias;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketRepository;

public class UpdateBucketCommandTest {

	private final BucketRepository repository = mock(BucketRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new UpdateBucketCommand.Handler(repository));

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
		verify(repository).update(from, to, command.getTimestamp());
		reset(repository);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(repository).update(to, from, undo.getTimestamp());
		reset(repository);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(repository).update(from, to, redo.getTimestamp());
		reset(repository);
	}

	@Test
	public void testReplaceAliases() {

		Identity principal = new Identity();
		Bucket from = new Bucket();
		from.setVersion(1L);
		Bucket to = from.copy();
		from.addAlias(new Alias("foo"));
		from.addAlias(new Alias("bar"));
		to.addAlias(new Alias("foo"));
		to.addAlias(new Alias("baz"));

		Command command = new UpdateBucketCommand(principal, from, to);
		registry.execute(command);
		verify(repository).update(from, to, command.getTimestamp());
	}
}
