package secure;

import junit.framework.Assert;

import org.junit.Test;

import common.BCrypt;

public class BCryptTest {

	@Test
	public void test() {
		String password = "123";
		String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
		String rehashed = BCrypt.hashpw(password, BCrypt.gensalt());
		Assert.assertTrue("Password matches first hash", BCrypt.checkpw(password, hashed));
		Assert.assertTrue("Password matches second hash", BCrypt.checkpw(password, rehashed));
		Assert.assertTrue("First and second hashes differ", !hashed.equals(rehashed));
	}
}
