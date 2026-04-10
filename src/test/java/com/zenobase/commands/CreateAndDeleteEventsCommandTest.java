package com.zenobase.commands;

import static org.mockito.Mockito.*;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventRepository;

public class CreateAndDeleteEventsCommandTest {

	private final EventRepository repository = mock(EventRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
			new CreateEventsCommand.Handler(repository), new DeleteEventsCommand.Handler(repository));

	@Test
	public void test() {

		Identity principal = new Identity();
		String bucketId = Generator.id();
		Event e1 = new Event();
		e1.setValue(Event.TIMESTAMP, DateTime.now(DateTimeZone.UTC).minusHours(1));
		Event e2 = new Event();
		e1.setValue(Event.TIMESTAMP, DateTime.now(DateTimeZone.UTC));

		Command command = new CreateEventsCommand(principal, bucketId, Lists.newArrayList(e1, e2));
		registry.execute(command);
		verify(repository).add(bucketId, Lists.newArrayList(e1, e2));
		reset(repository);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(repository).delete(bucketId, Lists.newArrayList(e1.getId(), e2.getId()));
		reset(repository);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(repository).add(bucketId, Lists.newArrayList(e1, e2));
		reset(repository);
	}
}
