package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;

import com.zenobase.models.Alias;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketRepository;

public class UpdateBucketCommandTest {

	private final BucketRepository repository = mock(BucketRepository.class);
	private final CommandHandlerRegistry registry =
			CommandHandlerRegistry.containing(new UpdateBucketCommand.Handler(repository));
	private final Identity principal = new Identity();
	private final Bucket from = new Bucket();
	private final Bucket to = from.copy();

	@Before
	public void setUp() {
		from.setLabel("Test Bucket");
		from.setVersion(2);
		to.setLabel("Real Bucket");
		to.setVersion(2);
	}

	@Test
	public void test() {

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

		from.addAlias(new Alias("foo"));
		from.addAlias(new Alias("bar"));
		to.addAlias(new Alias("foo"));
		to.addAlias(new Alias("baz"));

		Command command = new UpdateBucketCommand(principal, from, to);
		registry.execute(command);
		verify(repository).update(from, to, command.getTimestamp());
	}

	@Test
	public void testRecoverFromVersionConflict() {

		Bucket from2 = from.copy();
		from2.setVersion(1);
		Bucket to2 = to.copy();
		to2.setVersion(1);

		Command command = new UpdateBucketCommand(principal, from, to);
		Exception e = new OpenSearchException(ErrorResponse.of(r -> r.status(409)
				.error(e2 -> e2.type("version_conflict_engine_exception").reason("version conflict"))));
		doThrow(e).when(repository).update(from, to, command.getTimestamp());
		Bucket current = from.copy();
		current.setVersion(1);
		when(repository.find(to.getId())).thenReturn(current);
		registry.execute(command);
		verify(repository).update(from2, to2, command.getTimestamp());
	}

	@Test(expected = OpenSearchException.class)
	public void testUnrecoverableVersionConflict() {

		Command command = new UpdateBucketCommand(principal, from, to);
		Exception e = new OpenSearchException(ErrorResponse.of(r -> r.status(409)
				.error(e2 -> e2.type("version_conflict_engine_exception").reason("version conflict"))));
		doThrow(e).when(repository).update(from, to, command.getTimestamp());
		Bucket current = from.copy();
		current.setVersion(3);
		when(repository.find(to.getId())).thenReturn(current);
		registry.execute(command);
	}
}
