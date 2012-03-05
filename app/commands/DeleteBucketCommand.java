package commands;

import models.Bucket;
import secure.Identity;
import services.BucketManager;

public class DeleteBucketCommand extends CommandSupport {

	private final BucketManager manager;
	private final Bucket bucket;

	public DeleteBucketCommand(BucketManager manager, Identity identity, Bucket bucket) {
		super(identity);
		this.manager = manager;
		this.bucket = bucket;
	}

	public void execute() {
		manager.deleteBucket(bucket.getId());
	}

	public CreateBucketCommand reverse() {
		return new CreateBucketCommand(manager, getIdentity(), bucket, false);
	}

	@Override
	public String toString() {
		return String.format("deleted '%s'", bucket);
	}
}
