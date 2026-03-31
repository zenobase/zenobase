package com.zenobase;

import io.helidon.http.HttpException;
import io.helidon.webserver.http.HttpRouting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.controllers.*;

class Routing {

	private static final Logger logger = LoggerFactory.getLogger(Routing.class);

	static void buildRouting(HttpRouting.Builder routing, Wiring wiring) {
		// Filters
		routing.addFilter(wiring.sentryFilter());
		routing.addFilter(wiring.gatekeeperFilter());
		routing.addFilter(wiring.quotaExceptionFilter());

		// Error handlers
		routing.error(HttpException.class, (req, res, e) -> {
			ControllerSupport.sendError(res, e.status(), String.valueOf(e.getMessage()));
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
		var status = wiring.statusController();
		routing.get("/status", status::get);
		routing.post("/status", status::post);

		// Who
		var who = wiring.whoController();
		routing.get("/who", who::who);

		// Password reset
		var reset = wiring.passwordResetController();
		routing.post("/reset", reset::requestReset);

		// Quota
		var quota = wiring.quotaController();
		routing.get("/users/{userId}/quota", quota::get);
		routing.post("/users/{userId}/quota", quota::post);

		// Users
		var userList = wiring.userListController();
		routing.get("/users/", userList::find);

		var user = wiring.userController();
		routing.get("/users/{userId}", user::get);
		routing.post("/users/{userId}", user::update);

		// Accounts
		var account = wiring.accountController();
		routing.post("/users/", account::open);
		routing.delete("/users/{userId}", account::close);

		// Buckets
		var bucketList = wiring.bucketListController();
		routing.get("/buckets/", bucketList::findAll);
		routing.post("/buckets/", bucketList::post);

		var userBuckets = wiring.bucketListController();
		routing.get("/users/{userId}/buckets/", userBuckets::findByUser);

		var bucket = wiring.bucketController();
		routing.get("/buckets/{bucketId}", bucket::get);
		routing.get("/buckets/{bucketId}/label", bucket::getLabel);
		routing.put("/buckets/{bucketId}", bucket::update);
		routing.delete("/buckets/{bucketId}", bucket::delete);

		// Events
		var eventList = wiring.eventListController();
		routing.get("/buckets/{bucketId}/", eventList::find);
		routing.post("/buckets/{bucketId}/", eventList::post);

		routing.get("/events/", eventList::countAll);
		routing.get("/users/{userId}/events/", eventList::countByUser);

		var event = wiring.eventController();
		routing.get("/buckets/{bucketId}/{eventId}", event::get);
		routing.put("/buckets/{bucketId}/{eventId}", event::update);
		routing.delete("/buckets/{bucketId}/{eventId}", event::delete);

		// Tags
		var tags = wiring.tagController();
		routing.get("/buckets/{bucketId}/tags/", tags::get);

		// Journal
		var journal = wiring.journalController();
		routing.get("/journal/", journal::findAll);
		routing.get("/users/{userId}/journal/", journal::findByUser);
		routing.post("/journal/", journal::post);

		// Credentials
		var credentialsList = wiring.credentialsListController();
		routing.get("/credentials/", credentialsList::findAll);
		routing.get("/users/{userId}/credentials/", credentialsList::findByUser);
		routing.post("/credentials/", credentialsList::post);

		var credentials = wiring.credentialsController();
		routing.get("/credentials/{credentialsId}", credentials::get);
		routing.post("/credentials/{credentialsId}", credentials::update);
		routing.delete("/credentials/{credentialsId}", credentials::delete);

		// Tasks
		var taskList = wiring.taskListController();
		routing.get("/tasks/", taskList::findAll);
		routing.get("/buckets/{bucketId}/tasks/", taskList::findByBucket);
		routing.get("/users/{userId}/tasks/", taskList::findByUser);
		routing.post("/tasks/", taskList::post);

		var task = wiring.taskController();
		routing.get("/tasks/{taskId}", task::get);
		routing.post("/tasks/{taskId}", task::update);
		routing.delete("/tasks/{taskId}", task::delete);

		// OAuth
		var oauth = wiring.oauthController();
		routing.post("/oauth/authorize", oauth::authorize);
		routing.post("/oauth/token", oauth::token);
		routing.get("/oauth/callback/{id}", oauth::callback);

		// Authorizations
		var authList = wiring.authorizationListController();
		routing.get("/authorizations/", authList::findAll);
		routing.get("/users/{userId}/authorizations/", authList::findByUser);

		var auth = wiring.authorizationController();
		routing.get("/authorizations/{authId}", auth::get);
		routing.delete("/authorizations/{authId}", auth::delete);

		// Snapshots
		var snapshot = wiring.snapshotController();
		routing.get("/snapshots/", snapshot::findAll);
		routing.delete("/snapshots/{snapshotId}", snapshot::delete);
		routing.post("/snapshots/", snapshot::snapshot);

		// Jobs
		var scheduler = wiring.schedulerController();
		routing.get("/jobs/", scheduler::findAll);

		// Utility
		var redirect = wiring.redirectController();
		routing.get("/to", redirect::get);

		var og = wiring.openGraphController();
		routing.get("/og", og::get);
	}
}
