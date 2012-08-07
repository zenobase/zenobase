package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketRepository;

public class UpdateEventCommandTest {

	private final BucketRepository buckets = mock(BucketRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new UpdateEventCommand.Handler(buckets));

	@Test
	public void test() {

		String bucketId = Generator.id();
		Identity principal = new Identity();
		Event from = new Event();
		from.setValue(Event.TAG, "foo");
		from.setVersion(1L);
		Event to = from.copy();
		from.setValue(Event.TAG, "bar");

		Command command = new UpdateEventCommand(principal, bucketId, from, to);
		registry.execute(command);
		verify(buckets).update(bucketId, to);
		reset(buckets);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(buckets).update(bucketId, from);
		reset(buckets);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(buckets).update(bucketId, to);
		reset(buckets);
	}
}
