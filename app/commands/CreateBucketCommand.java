package commands;

import models.Bucket;
import models.Identity;

public class CreateBucketCommand extends CommandSupport {

	public static final String TYPE = "create bucket";

	private final Bucket bucket;

	public CreateBucketCommand(Identity identity, Bucket bucket) {
		super(TYPE, identity);
		this.bucket = bucket;
	}

	public Bucket getBucket() {
		return bucket;
	}

	@Override
	public Command reverse(Identity identity) {
		return new DeleteBucketCommand(identity, bucket);
	}

	@Override
	public String toString() {
		return String.format("create '%s'", bucket);
	}
}
