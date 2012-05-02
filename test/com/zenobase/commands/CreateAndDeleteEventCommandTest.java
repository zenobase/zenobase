package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketManager;

public class CreateAndDeleteEventCommandTest {

	private final BucketManager buckets = mock(BucketManager.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new CreateEventCommand.Handler(buckets),
		new DeleteEventCommand.Handler(buckets));

	@Test
	public void test() {

		Identity principal = new Identity();
		String bucketId = Generator.id();
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC));

		Command command = new CreateEventCommand(principal, bucketId, event);
		registry.execute(command);
		verify(buckets).add(bucketId, event);
		reset(buckets);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(buckets).delete(bucketId, event.getId());
		reset(buckets);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(buckets).add(bucketId, event);
		reset(buckets);
	}
}
