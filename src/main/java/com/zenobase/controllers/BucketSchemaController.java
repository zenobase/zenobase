package com.zenobase.controllers;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;

import com.zenobase.json.JsonSchema;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.EventRepository;

public class BucketSchemaController extends ControllerSupport {

	private final BucketRepository buckets;
	private final EventRepository events;

	@Inject
	public BucketSchemaController(AuthorizationContext security, BucketRepository buckets, EventRepository events) {
		super(security);
		this.buckets = buckets;
		this.events = events;
	}

	public void get(ServerRequest req, ServerResponse res) {
		String bucketId = req.path().pathParameters().get("bucketId");
		Authorization auth = getCurrentAuthorization(req);
		Bucket bucket = buckets.find(bucketId);
		if (bucket == null) {
			sendNotFound(res);
			return;
		}
		if (!bucket.hasRole(auth, Role.VIEWER)) {
			if (auth == null) {
				sendUnauthorized(res);
			} else {
				sendForbidden(res);
			}
			return;
		}
		sendOk(
				res,
				JsonSchema.forFields(events.fields(bucketId), Event.READ_ONLY_FIELDS)
						.toJson());
	}
}
