package commands;

import models.Bucket;
import services.BucketManager;

public class CreateBucketCommand extends CommandSupport {

	private final BucketManager manager;
	private final Bucket bucket;
	private final boolean createIndex;

	public CreateBucketCommand(BucketManager manager, Bucket bucket, boolean createIndex) {
		super(bucket.getUser());
		this.manager = manager;
		this.bucket = bucket;
		this.createIndex = createIndex;
	}

	public void execute() {
		// Logger.info("Creating bucket: %s", bucket);
		manager.store(bucket, createIndex);
	}

	public DeleteBucketCommand reverse() {
		return new DeleteBucketCommand(manager, bucket);
	}

	@Override
	public String toString() {
		return String.format("%screated '%s'", createIndex ? "" : "re", bucket);
	}
}
