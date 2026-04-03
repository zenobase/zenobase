package com.zenobase.commands;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;

import com.zenobase.common.Generator;
import com.zenobase.json.OptimisticLock;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventRepository;

public class UpdateEventCommandTest {

	private final EventRepository repository = mock(EventRepository.class);
	private final CommandHandlerRegistry registry =
			CommandHandlerRegistry.containing(new UpdateEventCommand.Handler(repository));
	private final String bucketId = Generator.id();
	private final Identity principal = new Identity();
	private final Event from = new Event();
	private final Event to = from.copy();

	@BeforeEach
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
		verify(repository).update(eq(bucketId), any(Event.class), any(Event.class), eq(command.getTimestamp()));
		reset(repository);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(repository).update(eq(bucketId), any(Event.class), any(Event.class), eq(undo.getTimestamp()));
		reset(repository);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(repository).update(eq(bucketId), any(Event.class), any(Event.class), eq(redo.getTimestamp()));
		reset(repository);
	}

	@Test
	public void testRecoverFromVersionConflict() {

		Event to2 = to.copy();
		to2.setVersion(1);

		Command command = new UpdateEventCommand(principal, bucketId, from, to);
		Exception e = new OpenSearchException(ErrorResponse.of(r -> r.status(409)
				.error(e2 -> e2.type("version_conflict_engine_exception").reason("version conflict"))));
		doThrow(e)
				.doNothing()
				.when(repository)
				.update(eq(bucketId), any(Event.class), any(Event.class), eq(command.getTimestamp()));
		Event current = from.copy();
		current.setVersion(1);
		current.setOptimisticLock(new OptimisticLock(1, 1));
		when(repository.find(bucketId, to.getId())).thenReturn(current);
		registry.execute(command);
		verify(repository, times(2))
				.update(eq(bucketId), any(Event.class), any(Event.class), eq(command.getTimestamp()));
	}

	@Test
	public void testUnrecoverableVersionConflict() {

		Command command = new UpdateEventCommand(principal, bucketId, from, to);
		Exception e = new OpenSearchException(ErrorResponse.of(r -> r.status(409)
				.error(e2 -> e2.type("version_conflict_engine_exception").reason("version conflict"))));
		doThrow(e)
				.when(repository)
				.update(eq(bucketId), any(Event.class), any(Event.class), eq(command.getTimestamp()));
		Event current = from.copy();
		current.setVersion(3);
		when(repository.find(bucketId, to.getId())).thenReturn(current);
		assertThatThrownBy(() -> registry.execute(command)).isInstanceOf(OpenSearchException.class);
	}

	@Test
	public void testRecoverFromMissingEvent() {

		Command command = new UpdateEventCommand(principal, bucketId, from, to);
		Exception e = new OpenSearchException(ErrorResponse.of(r -> r.status(409)
				.error(e2 -> e2.type("version_conflict_engine_exception").reason("version conflict"))));
		doThrow(e)
				.when(repository)
				.update(eq(bucketId), any(Event.class), any(Event.class), eq(command.getTimestamp()));
		when(repository.find(bucketId, to.getId())).thenReturn(null);
		registry.execute(command);
		verify(repository).add(bucketId, to, command.getTimestamp());
	}
}
