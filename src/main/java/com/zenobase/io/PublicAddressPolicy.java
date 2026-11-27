package com.zenobase.io;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Allows only publicly routable addresses, so that server-side fetches of user-supplied URLs can't reach loopback,
 * link-local (e.g. the ECS/EC2 metadata endpoints), private or other reserved networks.
 */
public class PublicAddressPolicy {

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
		byte[] b = address.getAddress();
		return address instanceof Inet6Address ? isPublicIpv6(b) : isPublicIpv4(b);
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

	private boolean isPublicIpv6(byte[] b) {
		int first = b[0] & 0xff;
		if ((first & 0xfe) == 0xfc) {
			// unique local
			return false;
		}
		if (first == 0x20 && b[1] == 0x02) {
			// 6to4
			return isAllowed(ipv4(b, 2));
		}
		if (first == 0x00 && b[1] == 0x64 && (b[2] & 0xff) == 0xff && (b[3] & 0xff) == 0x9b) {
			// NAT64
			return isAllowed(ipv4(b, 12));
		}
		// Reject IPv4-compatible (::/96) and other addresses in the reserved ::/8 block.
		return first != 0x00;
	}

	private static InetAddress ipv4(byte[] b, int offset) {
		try {
			return Inet4Address.getByAddress(Arrays.copyOfRange(b, offset, offset + 4));
		} catch (UnknownHostException e) {
			throw new IllegalStateException(e);
		}
	}
}
