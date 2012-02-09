package commands;

import models.Bucket;
import services.BucketManager;

public class DeleteBucketCommand extends CommandSupport {

	private final BucketManager manager;
	private final Bucket bucket;

	public DeleteBucketCommand(BucketManager manager, Bucket bucket) {
		super(bucket.getUser());
		this.manager = manager;
		this.bucket = bucket;
	}

	public void execute() {
		manager.deleteBucket(bucket.getId(), bucket.getUser());
	}

	public CreateBucketCommand reverse() {
		return new CreateBucketCommand(manager, bucket, false);
	}

	@Override
	public String toString() {
		return String.format("deleted '%s'", bucket);
	}
}
