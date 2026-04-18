package com.zenobase.services;

import static org.mockito.ArgumentMatchers.isA;

import com.google.common.util.concurrent.Uninterruptibles;
import com.zenobase.commands.CreateBucketCommand;
import com.zenobase.commands.CreateCredentialsCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.CreateTaskCommand;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.models.Alias;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Role;
import com.zenobase.models.User;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.repositories.EventRepository;
import com.zenobase.repositories.OpenSearchTestSupport;
import com.zenobase.repositories.TaskRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.Task;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class CommandRebuildTest extends OpenSearchTestSupport {

	@Test
	public void test() {
		CommandDispatcher dispatcher = Mockito.mock(CommandDispatcher.class);
		UserRepository users = new UserRepository(getManager());
		CredentialsRepository credentials = new CredentialsRepository(getManager());
		BucketRepository buckets = new BucketRepository(getManager());
		EventRepository events = new EventRepository(getManager());
		TaskRepository tasks = new TaskRepository(getManager());

		User user = new User("jdoe");
		Credentials credential = new Credentials("test", user.asIdentity());
		Bucket bucket = new Bucket();
		bucket.addRole(user.asIdentity(), Role.OWNER);
		Uninterruptibles.sleepUninterruptibly(2, TimeUnit.MILLISECONDS); // ensure that the view has a later created timestamp
		Bucket view = new Bucket();
		view.addRole(user.asIdentity(), Role.OWNER);
		view.addAlias(new Alias(bucket.getId()));
		Event event = new Event();
		Task task = new Task("test", bucket.getId(), user.asIdentity());

		users.store(user);
		credentials.store(credential);
		buckets.store(bucket);
		buckets.store(view);
		events.add(bucket.getId(), event);
		events.refresh(bucket.getId());
		tasks.store(task);
		tasks.refresh();

		new CommandRebuild("", 1, dispatcher, users, credentials, buckets, tasks).rebuild(getManager());

		InOrder ordered = Mockito.inOrder(dispatcher);
		ordered.verify(dispatcher).dispatch(isA(CreateUserCommand.class));
		ordered.verify(dispatcher).dispatch(isA(CreateCredentialsCommand.class));
		ordered.verify(dispatcher).dispatch(isA(CreateBucketCommand.class));
		ordered.verify(dispatcher).dispatch(isA(CreateEventsCommand.class));
		ordered.verify(dispatcher).dispatch(isA(CreateBucketCommand.class));
		ordered.verify(dispatcher).dispatch(isA(CreateTaskCommand.class));
		ordered.verifyNoMoreInteractions();
	}
}
