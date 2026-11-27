package com.zenobase.io;

import com.google.common.net.InetAddresses;
import com.google.common.primitives.Ints;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * Allows only publicly routable addresses, so that server-side fetches of user-supplied URLs can't reach loopback,
 * link-local (e.g. the ECS/EC2 metadata endpoints), private or other reserved networks.
 */
public class PublicAddressPolicy {

	/** 64:ff9b::/96 */
	private static final byte[] NAT64_PREFIX = { 0x00, 0x64, (byte) 0xff, (byte) 0x9b, 0, 0, 0, 0, 0, 0, 0, 0 };

	public boolean isAllowed(InetAddress address) {
		if (
			address.isAnyLocalAddress() ||
			address.isLoopbackAddress() ||
			address.isLinkLocalAddress() ||
			address.isSiteLocalAddress() ||
			address.isMulticastAddress()
		) {
			return false;
		}
		return address instanceof Inet6Address ipv6 ? isPublicIpv6(ipv6) : isPublicIpv4(address.getAddress());
	}

	private static boolean isPublicIpv4(byte[] b) {
		int first = b[0] & 0xff;
		int second = b[1] & 0xff;
		return !(
			first == 0 || // "this" network
			(first == 100 && second >= 64 && second < 128) || // carrier-grade NAT
			(first == 192 && second == 0 && (b[2] & 0xff) == 0) || // IETF protocol assignments
			(first == 198 && (second == 18 || second == 19)) || // benchmarking
			first >= 240 // reserved and broadcast
		);
	}

	private boolean isPublicIpv6(Inet6Address address) {
		// IPv4-compatible, 6to4 and Teredo addresses
		if (InetAddresses.hasEmbeddedIPv4ClientAddress(address)) {
			return isAllowed(InetAddresses.getEmbeddedIPv4ClientAddress(address));
		}
		byte[] b = address.getAddress();
		int first = b[0] & 0xff;
		if ((first & 0xfe) == 0xfc) {
			// unique local
			return false;
		}
		if (first != 0x00) {
			return true;
		}
		// In the reserved ::/8 block, only the NAT64 well-known prefix (not covered by Guava) can map to a public address
		return (
			Arrays.equals(b, 0, NAT64_PREFIX.length, NAT64_PREFIX, 0, NAT64_PREFIX.length) &&
			isAllowed(InetAddresses.fromInteger(Ints.fromBytes(b[12], b[13], b[14], b[15])))
		);
	}
}
