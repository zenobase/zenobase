package com.zenobase.auth.auth0;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the pure decision logic in {@link Auth0ManagementService#isExternalApplication}. The rest of the
 * service requires a live Auth0 tenant or a mock {@code ManagementApi} and is exercised end-to-end in staging.
 *
 * <p>A regression here is how the SPA, M2M, or manually-registered partner Auth0 Applications would get silently
 * nuked, so the safe-default-when-absent behavior is pinned down explicitly.
 */
public class Auth0ManagementServiceTest {

	@Test
	public void thirdPartyApplicationsCanBeDeleted() {
		assertThat(Auth0ManagementService.isExternalApplication(Optional.of(false))).isTrue();
	}

	@Test
	public void firstPartyApplicationsAreRefused() {
		assertThat(Auth0ManagementService.isExternalApplication(Optional.of(true))).isFalse();
	}

	@Test
	public void absentFlagIsTreatedAsFirstParty() {
		// Fail safe: if Auth0 doesn't return is_first_party (SDK regression, unexpected response shape), refuse the
		// delete rather than risk taking down a production Application.
		assertThat(Auth0ManagementService.isExternalApplication(Optional.empty())).isFalse();
	}
}
