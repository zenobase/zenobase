package commands;

import models.Bucket;
import secure.Identity;
import services.BucketManager;

public class CreateBucketCommand extends CommandSupport {

	private final BucketManager manager;
	private final Bucket bucket;
	private final boolean createIndex;

	public CreateBucketCommand(BucketManager manager, Identity identity, Bucket bucket, boolean createIndex) {
		super(identity);
		this.manager = manager;
		this.bucket = bucket;
		this.createIndex = createIndex;
	}

	public void execute() {
		manager.store(bucket, createIndex);
	}

	public DeleteBucketCommand reverse() {
		return new DeleteBucketCommand(manager, getIdentity(), bucket);
	}

	@Override
	public String toString() {
		return String.format("%screated '%s'", createIndex ? "" : "re", bucket);
	}
}
