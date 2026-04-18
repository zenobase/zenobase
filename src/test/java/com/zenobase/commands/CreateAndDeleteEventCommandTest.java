package com.zenobase.commands;

import static org.mockito.Mockito.*;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.repositories.EventRepository;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class CreateAndDeleteEventCommandTest {

	private final EventRepository repository = mock(EventRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new CreateEventCommand.Handler(repository),
		new DeleteEventCommand.Handler(repository)
	);

	@Test
	public void test() {
		Identity principal = new Identity();
		String bucketId = Generator.id();
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.now(DateTimeZone.UTC));

		Command command = new CreateEventCommand(principal, bucketId, event);
		registry.execute(command);
		verify(repository).add(bucketId, event);
		reset(repository);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(repository).delete(bucketId, event.getId());
		reset(repository);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(repository).add(bucketId, event);
		reset(repository);
	}
}
