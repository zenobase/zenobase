package com.zenobase;

import com.google.inject.Injector;
import io.helidon.http.HttpException;
import io.helidon.webserver.http.HttpRouting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.actions.GatekeeperFilter;
import com.zenobase.actions.QuotaExceptionFilter;
import com.zenobase.controllers.*;

class Routing {

	private static final Logger logger = LoggerFactory.getLogger(Routing.class);

	static void buildRouting(HttpRouting.Builder routing, Injector injector) {
		// Filters
		routing.addFilter(injector.getInstance(GatekeeperFilter.class));
		routing.addFilter(injector.getInstance(QuotaExceptionFilter.class));

		// Error handlers
		routing.error(HttpException.class, (req, res, e) -> {
			ControllerSupport.sendError(res, e.status(), e.getMessage());
		});
		routing.error(Exception.class, (req, res, e) -> {
			logger.error(
					"Unhandled exception: {} {}",
					req.prologue().method(),
					req.prologue().uriPath().rawPath(),
					e);
			ControllerSupport.sendInternalServerError(res, "internal error");
		});

		// Status
		var status = injector.getInstance(StatusController.class);
		routing.get("/status", status::get);
		routing.head("/status", status::get);
		routing.post("/status", status::post);

		// Who
		var who = injector.getInstance(WhoController.class);
		routing.get("/who", who::who);

		// Password reset
		var reset = injector.getInstance(PasswordResetController.class);
		routing.post("/reset", reset::requestReset);

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
		routing.post("/users/", account::open);
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

		// OAuth
		var oauth = injector.getInstance(OAuthController.class);
		routing.post("/oauth/authorize", oauth::authorize);
		routing.post("/oauth/token", oauth::token);
		routing.get("/oauth/callback/{id}", oauth::callback);

		// Authorizations
		var authList = injector.getInstance(AuthorizationListController.class);
		routing.get("/authorizations/", authList::findAll);
		routing.get("/users/{userId}/authorizations/", authList::findByUser);

		var auth = injector.getInstance(AuthorizationController.class);
		routing.get("/authorizations/{authId}", auth::get);
		routing.delete("/authorizations/{authId}", auth::delete);

		// Payments
		var payment = injector.getInstance(PaymentController.class);
		routing.delete("/users/{userId}/payment", payment::cancel);
		routing.post("/payments/token", payment::token);
		routing.post("/payments/", payment::pay);

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
