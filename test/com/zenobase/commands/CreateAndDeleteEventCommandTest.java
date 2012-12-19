package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventRepository;

public class CreateAndDeleteEventCommandTest {

	private final EventRepository events = mock(EventRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new CreateEventCommand.Handler(events),
		new DeleteEventCommand.Handler(events));

	@Test
	public void test() {

		Identity principal = new Identity();
		String bucketId = Generator.id();
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC));

		Command command = new CreateEventCommand(principal, bucketId, event);
		registry.execute(command);
		verify(events).add(bucketId, event);
		reset(events);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(events).delete(bucketId, event.getId());
		reset(events);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(events).add(bucketId, event);
		reset(events);
	}
}
