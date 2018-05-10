package com.zenobase.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import org.joda.time.DateTime;
import play.Logger;

import com.zenobase.commands.CreateAuthorizationCommand;
import com.zenobase.commands.CreateBucketCommand;
import com.zenobase.commands.CreateCredentialsCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.CreateTaskCommand;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;

public class CommandRebuild {

	private final String sourceCluster;
	private final NodeFactory nodeFactory;
	private final CommandDispatcher dispatcher;

	@Inject
	public CommandRebuild(@Named("es.rebuild") String sourceCluster, NodeFactory nodeFactory, CommandDispatcher dispatcher) {
		this.sourceCluster = sourceCluster;
		this.nodeFactory = nodeFactory;
		this.dispatcher = dispatcher;
	}

	public void rebuild() {
		if (!sourceCluster.isEmpty()) {
			IndexManager indexManager = new IndexManager(nodeFactory, sourceCluster);
			rebuild(indexManager);
			indexManager.close();
		}
	}

	void rebuild(IndexManager indexManager) {
		Logger.info("Rebuilding history from {}...", sourceCluster);
		Stopwatch timer = Stopwatch.createStarted();
		rebuildUsers(indexManager);
		rebuildAuthorizations(indexManager);
		rebuildCredentials(indexManager);
		rebuildBuckets(indexManager);
		rebuildTasks(indexManager);
		Logger.warn("Rebuilt history in {} s", timer.elapsed(TimeUnit.SECONDS));
	}

	private void rebuildUsers(IndexManager indexManager) {
		new UserRepository(indexManager).findAll(user ->
			dispatcher.dispatch(new CreateUserCommand(user.asIdentity(), user)));
	}

	private void rebuildAuthorizations(IndexManager indexManager) {
		new AuthorizationRepository(indexManager).findAll(authorization ->
			dispatcher.dispatch(new CreateAuthorizationCommand(authorization.getPrincipal(), authorization)));
	}

	private void rebuildCredentials(IndexManager indexManager) {
		new CredentialsRepository(indexManager).findAll(credential ->
			dispatcher.dispatch(new CreateCredentialsCommand(credential.getPrincipal(), credential)));
	}

	private void rebuildBuckets(IndexManager indexManager) {
		EventRepository events = new EventRepository(indexManager);
		BucketRepository buckets = new BucketRepository(indexManager);
		buckets.findAll(bucket -> {
			if (!events.exists(bucket.getId())) {
				Logger.warn("Fixing missing aliases for bucket {}", bucket.getId());
				buckets.realias(bucket);
			}
			Identity owner = Iterables.getOnlyElement(bucket.getPrincipals(Role.OWNER));
			dispatcher.dispatch(new CreateBucketCommand(owner, bucket));
			if (!bucket.isVirtual()) {
				rebuildEvents(events, owner, bucket.getId(), bucket.getCreated());
			}
		});
	}

	private void rebuildEvents(EventRepository events, Identity owner, String bucketId, DateTime timestamp) {
		List<Event> batch = new ArrayList<>();
		AtomicInteger batchNum = new AtomicInteger(1);
		events.findAll(bucketId, event -> {
			batch.add(event);
			if (batch.size() == 1000) {
				dispatcher.dispatch(new CreateEventsCommand(owner, bucketId, batch, timestamp.plusMillis(batchNum.get())));
				batch.clear();
				batchNum.incrementAndGet();
			}
		});
		if (!batch.isEmpty()) {
			dispatcher.dispatch(new CreateEventsCommand(owner, bucketId, batch, timestamp.plusMillis(batchNum.get())));
		}
	}

	private void rebuildTasks(IndexManager indexManager) {
		new TaskRepository(indexManager).findAll(task ->
			dispatcher.dispatch(new CreateTaskCommand(task.getPrincipal(), task)));
	}
}
