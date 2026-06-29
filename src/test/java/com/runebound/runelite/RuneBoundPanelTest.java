package com.runebound.runelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import java.time.Duration;
import java.time.Instant;
import javax.swing.SwingUtilities;
import org.junit.Test;

public class RuneBoundPanelTest
{
	@Test
	public void detectedUsernameDisplaysAsActivePlayer() throws Exception
	{
		final RuneBoundPanel panel = updatedPanel(
			"RuneBounder",
			"Ready",
			null,
			Duration.ZERO
		);

		assertEquals("RuneBounder", panel.displayedPlayer());
		assertEquals("Ready", panel.displayedStatus());
		assertTrue(panel.isOpenSearchEnabled());
		assertTrue(panel.isOpenProfileEnabled());
		assertTrue(panel.isRefreshEnabled());
		assertEquals("Refresh", panel.refreshButtonText());
	}

	@Test
	public void unavailableProfileDetailsAreHidden() throws Exception
	{
		final RuneBoundPanel panel = updatedPanel("RuneBounder", "Ready", null, Duration.ZERO);

		assertFalse(panel.isSummaryHeaderVisible());
		assertFalse(panel.isCurrentTitleVisible());
		assertFalse(panel.isTierVisible());
		assertFalse(panel.isBoundPointsVisible());
		assertFalse(panel.isTotalLevelVisible());
		assertFalse(panel.isTotalXpVisible());
		assertFalse(panel.isRecentAchievementsVisible());
		assertFalse(panel.isFreshnessVisible());
		assertFalse(panel.isSummaryMessageVisible());
		assertEquals("", panel.displayedCurrentTitle());
		assertEquals("", panel.displayedTier());
		assertEquals("", panel.displayedBoundPoints());
		assertEquals("", panel.displayedRecentAchievements());
	}

	@Test
	public void summaryModelDisplaysProfileFields() throws Exception
	{
		final RuneBoundPanel panel = new RuneBoundPanel();
		SwingUtilities.invokeAndWait(() -> panel.showModel(RuneBoundPanelModel.builder()
			.player("RuneBounder")
			.normalizedUsername("runebounder")
			.profileUrl("https://rune-bound.net/player/RuneBounder")
			.accountType("MAIN")
			.buildType("main")
			.currentTitle("10 HP Paragon")
			.tier("Ascendant Bound")
			.badge("Legendary")
			.boundPoints("35,570")
			.totalLevel("2,277")
			.totalXp("460,000,000")
			.recentAchievements("Example Achievement")
			.freshness("trusted_cached / Source Freshness / RuneBound Freshness")
			.status("Cached RuneBound profile")
			.networkLookupsEnabled(true)
			.openProfileEnabled(true)
			.build()));

		assertEquals("RuneBounder", panel.displayedPlayer());
		assertTrue(panel.isSummaryHeaderVisible());
		assertTrue(panel.isCurrentTitleVisible());
		assertTrue(panel.isTierVisible());
		assertTrue(panel.isBoundPointsVisible());
		assertTrue(panel.isTotalLevelVisible());
		assertTrue(panel.isTotalXpVisible());
		assertTrue(panel.isRecentAchievementsVisible());
		assertTrue(panel.isFreshnessVisible());
		assertEquals("10 HP Paragon", panel.displayedCurrentTitle());
		assertEquals("Ascendant Bound", panel.displayedTier());
		assertEquals("35,570", panel.displayedBoundPoints());
		assertEquals("2,277", panel.displayedTotalLevel());
		assertEquals("460,000,000", panel.displayedTotalXp());
		assertEquals("Example Achievement", panel.displayedRecentAchievements());
		assertEquals("trusted_cached / Source Freshness / RuneBound Freshness", panel.displayedFreshness());
		assertTrue(panel.isOpenProfileEnabled());
	}

	@Test
	public void cooldownDisplayUsesCleanStatusAndRemainingTime() throws Exception
	{
		final RuneBoundPanel panel = updatedPanel(
			"RuneBounder",
			"Cooldown active",
			Instant.parse("2026-06-22T23:27:00Z"),
			Duration.ofMinutes(42).plusSeconds(9)
		);

		assertEquals("Cooldown active", panel.displayedStatus());
		assertEquals("Wait before another summary lookup for this player.", panel.displayedStatusNote());
		assertEquals("42m 9s", panel.displayedCooldown());
		assertTrue(panel.isRefreshEnabled());
	}

	@Test
	public void loadedSummaryStatusIsDisplayedWithCooldown() throws Exception
	{
		final RuneBoundPanel panel = updatedPanel(
			"RuneBounder",
			"Summary response loaded from RuneBound",
			Instant.parse("2026-06-22T23:27:00Z"),
			Duration.ofMinutes(60),
			true
		);

		assertEquals("Cached summary loaded", panel.displayedStatus());
		assertEquals("Read-only summary returned by RuneBound cache.", panel.displayedStatusNote());
		assertEquals("60m 0s", panel.displayedCooldown());
	}

	@Test
	public void networkDisabledKeepsSearchAndProfileUsableButDisablesRefresh() throws Exception
	{
		final RuneBoundPanel panel = updatedPanel(
			"RuneBounder",
			"Network lookups disabled",
			null,
			Duration.ZERO,
			false
		);

		assertEquals("Lookups disabled", panel.displayedStatus());
		assertEquals("Enable summary lookups in config to request cached data.", panel.displayedStatusNote());
		assertTrue(panel.isOpenSearchEnabled());
		assertTrue(panel.isOpenProfileEnabled());
		assertFalse(panel.isRefreshEnabled());
	}

	@Test
	public void noUsernameShowsNotLoggedInAndDisablesButtons() throws Exception
	{
		final RuneBoundPanel panel = updatedPanel(null, "Ready", null, Duration.ZERO);

		assertEquals("Not detected", panel.displayedPlayer());
		assertEquals("Not logged in", panel.displayedStatus());
		assertEquals("Log in or use manual lookup.", panel.displayedStatusNote());
		assertEquals("Ready", panel.displayedCooldown());
		assertFalse(panel.isSummaryHeaderVisible());
		assertTrue(panel.isOpenSearchEnabled());
		assertFalse(panel.isOpenProfileEnabled());
		assertFalse(panel.isRefreshEnabled());
	}

	@Test
	public void manualLookupUsernameCanBeEntered() throws Exception
	{
		final RuneBoundPanel panel = new RuneBoundPanel();

		SwingUtilities.invokeAndWait(() -> panel.setManualLookupUsername(" RuneBounder "));

		assertEquals(" RuneBounder ", panel.manualLookupUsername());
	}

	@Test
	public void statusDotChangesBetweenNotLoggedInAndReady() throws Exception
	{
		final RuneBoundPanel panel = updatedPanel(null, "Not logged in", null, Duration.ZERO);
		final java.awt.Color notLoggedInColor = panel.statusDotColor();

		SwingUtilities.invokeAndWait(() -> panel.showModel("RuneBounder", "Ready", null, Duration.ZERO, true));

		assertNotEquals(notLoggedInColor, panel.statusDotColor());
		assertEquals("Ready", panel.displayedStatus());
	}

	@Test
	public void staleDirtyAndNotCachedStatesUseClearPanelCopy() throws Exception
	{
		RuneBoundPanel panel = updatedPanel(
			"RuneBounder",
			"Cached RuneBound profile may be stale",
			null,
			Duration.ZERO
		);

		assertEquals("Stale cache", panel.displayedStatus());
		assertEquals("Freshness labels show age and trust.", panel.displayedStatusNote());

		panel = updatedPanel(
			"RuneBounder",
			"Cached RuneBound profile needs freshness review",
			null,
			Duration.ZERO
		);

		assertEquals("Dirty cache", panel.displayedStatus());
		assertEquals("RuneBound marked this cached summary for review.", panel.displayedStatusNote());

		panel = updatedPanel(
			"RuneBounder",
			"No cached RuneBound summary",
			null,
			Duration.ZERO
		);

		assertEquals("Not cached", panel.displayedStatus());
		assertEquals("No cached profile summary yet.", panel.displayedStatusNote());
		assertTrue(panel.isSummaryHeaderVisible());
		assertTrue(panel.isSummaryMessageVisible());
		assertEquals(
			"RuneBound has no cached summary for this player yet. Open RuneBound to begin tracking or update this profile.",
			panel.displayedSummaryMessage()
		);
		assertFalse(panel.isCurrentTitleVisible());
		assertFalse(panel.isTierVisible());
		assertFalse(panel.isBoundPointsVisible());
	}

	@Test
	public void unsafeProfileLinkStateUsesClearCopy() throws Exception
	{
		final RuneBoundPanel panel = updatedPanel(
			"RuneBounder",
			"Profile link unavailable",
			null,
			Duration.ZERO
		);

		assertEquals("Profile unavailable", panel.displayedStatus());
		assertEquals("RuneBound did not provide a safe profile link.", panel.displayedStatusNote());
	}

	@Test
	public void errorStatesUseSafeReadableCopy() throws Exception
	{
		RuneBoundPanel panel = updatedPanel(
			"RuneBounder",
			"Summary lookup could not reach RuneBound",
			null,
			Duration.ZERO
		);

		assertEquals("Network unavailable", panel.displayedStatus());
		assertEquals("Check connection; no background retry is running.", panel.displayedStatusNote());

		panel = updatedPanel(
			"RuneBounder",
			"RuneBound rate limit active",
			null,
			Duration.ZERO
		);

		assertEquals("Rate limited", panel.displayedStatus());
		assertEquals("Wait before requesting another summary.", panel.displayedStatusNote());

		panel = updatedPanel(
			"RuneBounder",
			"RuneBound summary service unavailable",
			null,
			Duration.ZERO
		);

		assertEquals("Server error", panel.displayedStatus());
		assertEquals("RuneBound could not serve the summary right now.", panel.displayedStatusNote());
	}

	private static RuneBoundPanel updatedPanel(
		String username,
		String status,
		Instant lastRefresh,
		Duration cooldownRemaining
	) throws Exception
	{
		return updatedPanel(username, status, lastRefresh, cooldownRemaining, true);
	}

	private static RuneBoundPanel updatedPanel(
		String username,
		String status,
		Instant lastRefresh,
		Duration cooldownRemaining,
		boolean networkLookupsEnabled
	) throws Exception
	{
		final RuneBoundPanel panel = new RuneBoundPanel();
		SwingUtilities.invokeAndWait(() -> panel.showModel(
			username,
			status,
			lastRefresh,
			cooldownRemaining,
			networkLookupsEnabled
		));
		return panel;
	}
}
