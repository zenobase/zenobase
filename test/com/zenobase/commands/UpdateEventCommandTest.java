package com.zenobase.commands;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventRepository;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class UpdateEventCommandTest {

	private final EventRepository repository = mock(EventRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new UpdateEventCommand.Handler(repository));
	private final String bucketId = Generator.id();
	private final Identity principal = new Identity();
	private final Event from = new Event();
	private final Event to = from.copy();

	@Before
	public void setUp() {
		from.setValue(Event.TAG, "foo");
		from.setVersion(2);
		to.setValue(Event.TAG, "bar");
		to.setVersion(2);
	}

	@Test
	public void test() {

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
	public void testRecoverFromVersionConflict() {

		Event to2 = to.copy();
		to2.setVersion(1);

		Command command = new UpdateEventCommand(principal, bucketId, from, to);
		Exception e = new VersionConflictEngineException(null, null, null, 1, 2);
		doThrow(e).when(repository).update(bucketId, to, command.getTimestamp());
		registry.execute(command);
		verify(repository).update(bucketId, to2, command.getTimestamp());
	}

	@Test(expected = VersionConflictEngineException.class)
	public void testUnrecoverableVersionConflict() {

		Command command = new UpdateEventCommand(principal, bucketId, from, to);
		Exception e = new VersionConflictEngineException(null, null, null, 3, 2);
		doThrow(e).when(repository).update(bucketId, to, command.getTimestamp());
		registry.execute(command);
	}

	@Test
	public void testRecoverFromMissingEvent() {

		Command command = new UpdateEventCommand(principal, bucketId, from, to);
		Exception e = new VersionConflictEngineException(null, null, null, -1, 2);
		doThrow(e).when(repository).update(bucketId, to, command.getTimestamp());
		registry.execute(command);
		verify(repository).add(bucketId, to, command.getTimestamp());
	}
}
