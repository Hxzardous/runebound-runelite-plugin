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
		assertEquals("Unavailable", panel.displayedStatus());
		assertTrue(panel.isOpenProfileEnabled());
		assertTrue(panel.isRefreshEnabled());
		assertEquals("Refresh", panel.refreshButtonText());
		assertEquals("Open Profile", panel.openProfileButtonText());
		assertTrue(panel.manualLookupPreferredWidth() > panel.refreshPreferredWidth());
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
			.freshness("Fresh")
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
		assertFalse(panel.isFreshnessVisible());
		assertEquals("10 HP Paragon", panel.displayedCurrentTitle());
		assertEquals("Ascendant Bound", panel.displayedTier());
		assertEquals("35,570", panel.displayedBoundPoints());
		assertEquals("2,277", panel.displayedTotalLevel());
		assertEquals("460,000,000", panel.displayedTotalXp());
		assertEquals("Example Achievement", panel.displayedRecentAchievements());
		assertEquals("Fresh", panel.displayedFreshness());
		assertEquals("Fresh", panel.displayedStatus());
		assertEquals("Cached summary ready.", panel.statusTooltip());
		assertEquals("RuneBound progression score for tracked profile data.", panel.boundPointsTooltip());
		assertEquals("Refresh is limited per player to avoid unnecessary RuneBound reads.", panel.cooldownTooltip());
		assertTrue(panel.refreshTooltip().contains("safe GET request"));
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

		assertEquals("Unavailable", panel.displayedStatus());
		assertEquals("Try again when cooldown ends.", panel.displayedStatusNote());
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

		assertEquals("Unavailable", panel.displayedStatus());
		assertEquals("Cached summary ready.", panel.displayedStatusNote());
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

		assertEquals("Unavailable", panel.displayedStatus());
		assertEquals("Enable lookups to use Refresh.", panel.displayedStatusNote());
		assertTrue(panel.isOpenProfileEnabled());
		assertFalse(panel.isRefreshEnabled());
	}

	@Test
	public void noUsernameShowsNotLoggedInButAllowsManualLookupActions() throws Exception
	{
		final RuneBoundPanel panel = updatedPanel(null, "Ready", null, Duration.ZERO);

		assertEquals("Not detected", panel.displayedPlayer());
		assertEquals("Unavailable", panel.displayedStatus());
		assertEquals("Enter a username or log in.", panel.displayedStatusNote());
		assertEquals("Ready", panel.displayedCooldown());
		assertFalse(panel.isSummaryHeaderVisible());
		assertTrue(panel.isOpenProfileEnabled());
		assertTrue(panel.isRefreshEnabled());
	}

	@Test
	public void manualLookupUsernameCanBeEntered() throws Exception
	{
		final RuneBoundPanel panel = new RuneBoundPanel();

		SwingUtilities.invokeAndWait(() -> panel.setManualLookupUsername(" RuneBounder "));

		assertEquals(" RuneBounder ", panel.manualLookupUsername());
	}

	@Test
	public void statusDotChangesBetweenUnavailableAndFresh() throws Exception
	{
		final RuneBoundPanel panel = updatedPanel(null, "Not logged in", null, Duration.ZERO);
		final java.awt.Color unavailableColor = panel.statusDotColor();

		SwingUtilities.invokeAndWait(() -> panel.showModel(RuneBoundPanelModel.builder()
			.player("RuneBounder")
			.status("Cached RuneBound profile")
			.freshness("Fresh")
			.networkLookupsEnabled(true)
			.openProfileEnabled(true)
			.build()));

		assertNotEquals(unavailableColor, panel.statusDotColor());
		assertEquals("Fresh", panel.displayedStatus());
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

		assertEquals("Stale", panel.displayedStatus());
		assertEquals("RuneBound says this cached summary may be old.", panel.displayedStatusNote());
		assertEquals("RuneBound says this cached summary may be old.", panel.statusTooltip());

		panel = updatedPanel(
			"RuneBounder",
			"Cached RuneBound profile needs freshness review",
			null,
			Duration.ZERO
		);

		assertEquals("Stale", panel.displayedStatus());
		assertEquals("RuneBound marked this cached summary for review.", panel.displayedStatusNote());

		panel = updatedPanel(
			"RuneBounder",
			"No cached RuneBound summary",
			null,
			Duration.ZERO
		);

		assertEquals("Not cached", panel.displayedStatus());
		assertEquals("Open Profile to begin tracking.", panel.displayedStatusNote());
		assertTrue(panel.isSummaryHeaderVisible());
		assertTrue(panel.isSummaryMessageVisible());
		assertEquals(
			"No cached summary yet. Open Profile on RuneBound to begin tracking.",
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

		assertEquals("Unavailable", panel.displayedStatus());
		assertEquals("Open Profile is unavailable.", panel.displayedStatusNote());
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

		assertEquals("Error", panel.displayedStatus());
		assertEquals("RuneBound unreachable.", panel.displayedStatusNote());

		panel = updatedPanel(
			"RuneBounder",
			"RuneBound rate limit active",
			null,
			Duration.ZERO
		);

		assertEquals("Error", panel.displayedStatus());
		assertEquals("Try again later.", panel.displayedStatusNote());

		panel = updatedPanel(
			"RuneBounder",
			"RuneBound summary service unavailable",
			null,
			Duration.ZERO
		);

		assertEquals("Error", panel.displayedStatus());
		assertEquals("Try again later.", panel.displayedStatusNote());
	}

	@Test
	public void lastLookupUsesRelativeTimeInsteadOfRawTimestamp() throws Exception
	{
		assertEquals("Never", RuneBoundPanel.formatLastLookup(null, Instant.parse("2026-06-29T16:00:00Z")));
		assertEquals(
			"Just now",
			RuneBoundPanel.formatLastLookup(Instant.parse("2026-06-29T15:59:45Z"), Instant.parse("2026-06-29T16:00:00Z"))
		);
		assertEquals(
			"5m ago",
			RuneBoundPanel.formatLastLookup(Instant.parse("2026-06-29T15:55:00Z"), Instant.parse("2026-06-29T16:00:00Z"))
		);
		assertEquals(
			"3h ago",
			RuneBoundPanel.formatLastLookup(Instant.parse("2026-06-29T13:00:00Z"), Instant.parse("2026-06-29T16:00:00Z"))
		);
		assertEquals(
			"2d ago",
			RuneBoundPanel.formatLastLookup(Instant.parse("2026-06-27T16:00:00Z"), Instant.parse("2026-06-29T16:00:00Z"))
		);
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
