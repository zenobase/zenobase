package com.zenobase;

import com.google.inject.Injector;
import io.helidon.http.HeaderNames;
import io.helidon.http.HttpException;
import io.helidon.webserver.http.HttpRouting;
import io.sentry.Sentry;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.actions.GatekeeperFilter;
import com.zenobase.actions.LogContextFilter;
import com.zenobase.actions.QuotaExceptionFilter;
import com.zenobase.actions.SecurityHeadersFilter;
import com.zenobase.actions.SentryFilter;
import com.zenobase.controllers.*;

class Routing {

	private static final Logger logger = LoggerFactory.getLogger(Routing.class);

	static void buildRouting(HttpRouting.Builder routing, Injector injector) {
		// Filters
		routing.addFilter(new SecurityHeadersFilter());
		routing.addFilter(injector.getInstance(LogContextFilter.class));
		routing.addFilter(injector.getInstance(SentryFilter.class));
		routing.addFilter(injector.getInstance(GatekeeperFilter.class));
		routing.addFilter(injector.getInstance(QuotaExceptionFilter.class));

		// Error handlers
		routing.error(HttpException.class, (req, res, e) -> {
			ControllerSupport.sendError(res, e.status(), String.valueOf(e.getMessage()));
		});
		routing.error(OpenSearchException.class, (req, res, e) -> {
			logger.warn("Unhandled OpenSearch exception", e);
			Sentry.captureException(e, scope -> {
				scope.setContexts("opensearch.status", e.status());
				scope.setContexts("opensearch.type", e.error().type());
				scope.setContexts("opensearch.reason", e.error().reason());
				scope.setContexts(
						"opensearch.root_causes",
						e.error().rootCause().stream()
								.map(rc -> rc.type() + ": " + rc.reason())
								.toList());
			});
			ControllerSupport.sendInternalServerError(res, "internal error");
		});
		routing.error(Exception.class, (req, res, e) -> {
			logger.warn("Unhandled exception", e);
			Sentry.captureException(e);
			ControllerSupport.sendInternalServerError(res, "internal error");
		});

		// Robots
		routing.get("/robots.txt", (req, res) -> {
			res.header(HeaderNames.CONTENT_TYPE, "text/plain");
			res.send("User-agent: *\nDisallow: /\n");
		});

		// Status
		var status = injector.getInstance(StatusController.class);
		routing.get("/status", status::get);
		routing.post("/status", status::post);

		// Who
		var who = injector.getInstance(WhoController.class);
		routing.get("/who", who::who);

		// Quota
		var quota = injector.getInstance(QuotaController.class);
		routing.get("/users/{userId}/quota", quota::get);
		routing.post("/users/{userId}/quota", quota::post);

		// Users
		var userList = injector.getInstance(UserListController.class);
		routing.get("/users/", userList::find);

		var user = injector.getInstance(UserController.class);
		routing.get("/users/{userId}", user::get);
		routing.post("/users/{userId}", user::update);

		// Accounts
		var account = injector.getInstance(AccountController.class);
		routing.delete("/users/{userId}", account::close);

		// Buckets
		var bucketList = injector.getInstance(BucketListController.class);
		routing.get("/buckets/", bucketList::findAll);
		routing.post("/buckets/", bucketList::post);

		var userBuckets = injector.getInstance(BucketListController.class);
		routing.get("/users/{userId}/buckets/", userBuckets::findByUser);

		var bucket = injector.getInstance(BucketController.class);
		routing.get("/buckets/{bucketId}", bucket::get);
		routing.get("/buckets/{bucketId}/label", bucket::getLabel);
		routing.put("/buckets/{bucketId}", bucket::update);
		routing.delete("/buckets/{bucketId}", bucket::delete);

		// Schema
		var schema = injector.getInstance(BucketSchemaController.class);
		routing.get("/buckets/{bucketId}/schema", schema::get);

		// Events
		var eventList = injector.getInstance(EventListController.class);
		routing.get("/buckets/{bucketId}/", eventList::find);
		routing.post("/buckets/{bucketId}/", eventList::post);

		routing.get("/events/", eventList::countAll);
		routing.get("/users/{userId}/events/", eventList::countByUser);

		var event = injector.getInstance(EventController.class);
		routing.get("/buckets/{bucketId}/{eventId}", event::get);
		routing.put("/buckets/{bucketId}/{eventId}", event::update);
		routing.delete("/buckets/{bucketId}/{eventId}", event::delete);

		// Tags
		var tags = injector.getInstance(TagController.class);
		routing.get("/buckets/{bucketId}/tags/", tags::get);

		// Journal
		var journal = injector.getInstance(JournalController.class);
		routing.get("/journal/", journal::findAll);
		routing.get("/users/{userId}/journal/", journal::findByUser);
		routing.post("/journal/", journal::post);

		// Credentials
		var credentialsList = injector.getInstance(CredentialsListController.class);
		routing.get("/credentials/", credentialsList::findAll);
		routing.get("/users/{userId}/credentials/", credentialsList::findByUser);
		routing.post("/credentials/", credentialsList::post);

		var credentials = injector.getInstance(CredentialsController.class);
		routing.get("/credentials/{credentialsId}", credentials::get);
		routing.post("/credentials/{credentialsId}", credentials::update);
		routing.delete("/credentials/{credentialsId}", credentials::delete);

		// Tasks
		var taskList = injector.getInstance(TaskListController.class);
		routing.get("/tasks/", taskList::findAll);
		routing.get("/buckets/{bucketId}/tasks/", taskList::findByBucket);
		routing.get("/users/{userId}/tasks/", taskList::findByUser);
		routing.post("/tasks/", taskList::post);

		var task = injector.getInstance(TaskController.class);
		routing.get("/tasks/{taskId}", task::get);
		routing.post("/tasks/{taskId}", task::update);
		routing.delete("/tasks/{taskId}", task::delete);

		// Credentials callback (third-party OAuth provider redirect target)
		var credentialsCallback = injector.getInstance(CredentialsCallbackController.class);
		routing.get("/oauth/callback/{id}", credentialsCallback::callback);

		// Snapshots
		var snapshot = injector.getInstance(SnapshotController.class);
		routing.get("/snapshots/", snapshot::findAll);
		routing.delete("/snapshots/{snapshotId}", snapshot::delete);
		routing.post("/snapshots/", snapshot::snapshot);

		// Jobs
		var scheduler = injector.getInstance(SchedulerController.class);
		routing.get("/jobs/", scheduler::findAll);

		// Utility
		var redirect = injector.getInstance(RedirectController.class);
		routing.get("/to", redirect::get);

		var og = injector.getInstance(OpenGraphController.class);
		routing.get("/og", og::get);
	}
}
