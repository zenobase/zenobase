package controllers;

import javax.inject.Inject;

import models.Bucket;
import play.mvc.Controller;
import play.mvc.With;
import services.BucketManager;
import services.CommandQueue;
import services.NodeManager;

import com.google.common.collect.ImmutableList;
import commands.Command;
import commands.CreateBucketCommand;
import common.Generator;

@With(UserController.class)
public class DashboardController extends Controller {

	@Inject
	static CommandQueue queue;

	@Inject
	static NodeManager node;

	@Inject
	static BucketManager buckets;

    public static void get() {
    	if (Security.isConnected()) {
    		ImmutableList<Bucket> buckets = DashboardController.buckets.findBuckets();
    		ImmutableList<Command> history = queue.getHistory(10);
            renderTemplate("dashboard.html", buckets, history);
    	}
        renderTemplate("index.html");
    }

    public static void post() {
    	if (Security.isConnected()) {
    		Bucket bucket = createBucket();
			queue.execute(new CreateBucketCommand(buckets, bucket, true));
            BucketController.get(bucket.getId());
    	}
        renderTemplate("index.html");
    }

	private static Bucket createBucket() {
		String id = Generator.id();
		Bucket bucket = new Bucket(node.getIndex(id), id);
		bucket.setLabel(params.get("label"));
		bucket.setUser(Security.connected());
		bucket.setRole("owner");
		return bucket;
	}
}
