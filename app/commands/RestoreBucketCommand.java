package commands;

import models.Bucket;
import models.Identity;

public class RestoreBucketCommand extends CommandSupport {

	public static final String TYPE = "restore bucket";

	private final Bucket bucket;

	public RestoreBucketCommand(Identity identity, Bucket bucket) {
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
		return String.format("restored bucket '%s'", bucket);
	}
}
