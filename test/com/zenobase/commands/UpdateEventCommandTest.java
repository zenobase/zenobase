package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventRepository;

public class UpdateEventCommandTest {

	private final EventRepository repository = mock(EventRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new UpdateEventCommand.Handler(repository));

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
		verify(repository).update(bucketId, to, command.getTimestamp());
		reset(repository);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(repository).update(bucketId, from, undo.getTimestamp());
		reset(repository);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(repository).update(bucketId, to, redo.getTimestamp());
		reset(repository);
	}

	@Test
	public void testRecoverMissing() {

		String bucketId = Generator.id();
		Identity principal = new Identity();
		Event from = new Event();
		from.setValue(Event.TAG, "foo");
		from.setVersion(1L);
		Event to = from.copy();
		from.setValue(Event.TAG, "bar");

		Command command = new UpdateEventCommand(principal, bucketId, from, to);
		Exception e = new VersionConflictEngineException(null, null, null, -1, 2L);
		doThrow(e).when(repository).update(bucketId, to, command.getTimestamp());
		registry.execute(command);
		verify(repository).add(bucketId, to, command.getTimestamp());
		reset(repository);
	}
}
