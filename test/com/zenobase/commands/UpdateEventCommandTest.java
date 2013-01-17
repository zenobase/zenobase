package com.zenobase.commands;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventRepository;

public class UpdateEventCommandTest {

	private final EventRepository events = mock(EventRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new UpdateEventCommand.Handler(events));

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
		verify(events).update(bucketId, to, command.getTimestamp());
		reset(events);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(events).update(bucketId, from, undo.getTimestamp());
		reset(events);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(events).update(bucketId, to, redo.getTimestamp());
		reset(events);
	}
}
