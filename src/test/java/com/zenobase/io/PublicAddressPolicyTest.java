package com.zenobase.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PublicAddressPolicyTest {

	private final PublicAddressPolicy policy = new PublicAddressPolicy();

	@ParameterizedTest
	@ValueSource(strings = { "93.184.215.14", "8.8.8.8", "2606:4700:4700::1111", "2002:808:808::", "64:ff9b::808:808" })
	public void testPublic(String address) throws UnknownHostException {
		assertThat(policy.isAllowed(InetAddress.getByName(address))).isTrue();
	}

	@ParameterizedTest
	@ValueSource(
		strings = {
			"0.0.0.0",
			"127.0.0.1",
			"10.0.0.1",
			"172.16.0.1",
			"192.168.1.1",
			"169.254.169.254",
			"169.254.170.2",
			"100.64.0.1",
			"192.0.0.1",
			"198.18.0.1",
			"224.0.0.1",
			"255.255.255.255",
			"::",
			"::1",
			"::ffff:127.0.0.1",
			"::127.0.0.1",
			"fe80::1",
			"fd00:ec2::254",
			"ff02::1",
			"2002:a9fe:a9fe::",
			"64:ff9b::a9fe:a9fe",
		}
	)
	public void testNonPublic(String address) throws UnknownHostException {
		assertThat(policy.isAllowed(InetAddress.getByName(address))).isFalse();
	}
}
