package com.runebound.runelite;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.LinkBrowser;
import okhttp3.OkHttpClient;

@Slf4j
@PluginDescriptor(
	name = "RuneBound",
	description = "Display RuneBound profile summaries from a read-only cache endpoint.",
	tags = {"runebound", "profile", "summary"}
)
public class RuneBoundPlugin extends Plugin
{
	static final String PLUGIN_VERSION = "0.1.0";

	@Inject
	private Client client;

	@Inject
	private RuneBoundConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	private final Clock clock = Clock.systemUTC();
	private final RuneBoundSummaryCache summaryCache = new RuneBoundSummaryCache();

	private NavigationButton navButton;
	private RuneBoundPanel panel;
	private RuneBoundSummaryClient summaryClient;
	private String detectedUsername;
	private String activeLookupUsername;
	private String activeProfileUrl;
	private volatile boolean started;

	@Override
	protected void startUp()
	{
		started = true;
		summaryClient = new RuneBoundSummaryClient(new RuneBoundOkHttpTransport(okHttpClient, gson));
		panel = new RuneBoundPanel();
		panel.setOpenProfileAction(event -> openProfile());
		panel.setManualLookupAction(event -> requestSummary(selectedTargetUsername()));
		panel.setRefreshAction(event -> requestSummary(selectedTargetUsername()));

		navButton = NavigationButton.builder()
			.tooltip("RuneBound")
			.icon(createIcon())
			.priority(10)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		refreshCurrentUsername();
		renderPanel(defaultStatus());
		log.debug("RuneBound started");
	}

	@Override
	protected void shutDown()
	{
		started = false;
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}

		panel = null;
		summaryClient = null;
		detectedUsername = null;
		activeLookupUsername = null;
		activeProfileUrl = null;
		log.debug("RuneBound stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			refreshCurrentUsername();
			renderPanel(defaultStatus());
			return;
		}

		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			detectedUsername = null;
			renderPanel(defaultStatus());
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (RuneBoundConfig.CONFIG_GROUP.equals(event.getGroup()))
		{
			renderPanel(defaultStatus());
		}
	}

	private void openProfile()
	{
		final String username = selectedTargetUsername();
		if (!RuneBoundUsername.isLookupCandidate(username))
		{
			renderPanel(username == null ? "No logged-in player detected" : "Invalid username");
			return;
		}

		if (RuneBoundUrls.isSafeProfileUrl(activeProfileUrl) && username.equals(activeLookupUsername))
		{
			LinkBrowser.browse(activeProfileUrl);
			renderPanel(defaultStatus());
			return;
		}

		LinkBrowser.browse(RuneBoundUrls.profileUrl(username));
		renderPanel(defaultStatus());
	}

	private void requestSummary(String username)
	{
		final String normalizedUsername = RuneBoundUsername.normalize(username);
		if (!RuneBoundUsername.isLookupCandidate(normalizedUsername))
		{
			renderPanel(normalizedUsername == null ? "No logged-in player detected" : "Invalid username");
			return;
		}

		activeLookupUsername = normalizedUsername;
		activeProfileUrl = null;
		if (!config.enableNetworkLookups())
		{
			renderPanel("Network lookups disabled");
			return;
		}

		final Instant now = clock.instant();
		final RuneBoundSummaryResult cachedResult = summaryCache.freshResult(normalizedUsername, now);
		if (cachedResult != null)
		{
			applySummaryResult(normalizedUsername, cachedResult, "Using local summary cache");
			return;
		}

		final Duration remaining = summaryCache.remainingBeforeNextAttempt(normalizedUsername, now);
		if (!remaining.isZero())
		{
			final RuneBoundSummaryResult latestResult = summaryCache.latestResult(normalizedUsername);
			if (latestResult != null)
			{
				applySummaryResult(normalizedUsername, latestResult, "Cooldown active");
				return;
			}

			renderPanel("Cooldown active");
			return;
		}

		final RuneBoundSummaryResult latestResult = summaryCache.latestResult(normalizedUsername);
		if (latestResult != null)
		{
			applySummaryResult(normalizedUsername, latestResult, "Showing cached summary while refreshing");
		}
		else
		{
			renderPanel("Loading RuneBound summary");
		}

		summaryCache.markAttempt(normalizedUsername, now);
		summaryClient.requestSummary(normalizedUsername, result ->
		{
			if (!started)
			{
				return;
			}

			log.debug(
				"RuneBound summary request completed for {} with networkAttempted={}",
				normalizedUsername,
				result.isNetworkAttempted()
			);

			final RuneBoundSummaryResult latestCachedResult = summaryCache.latestResult(normalizedUsername);
			if (shouldKeepCachedResultAfterFailure(latestCachedResult, result))
			{
				applySummaryResult(normalizedUsername, latestCachedResult, result.getMessage());
				return;
			}

			summaryCache.put(normalizedUsername, result, clock.instant());
			applySummaryResult(normalizedUsername, result, result.getMessage());
		});
	}

	static boolean shouldKeepCachedResultAfterFailure(RuneBoundSummaryResult cachedResult, RuneBoundSummaryResult refreshResult)
	{
		if (cachedResult == null || cachedResult.getResponse() == null || refreshResult == null)
		{
			return false;
		}

		switch (refreshResult.getStatus())
		{
			case OFFLINE:
			case RATE_LIMITED:
			case SERVER_ERROR:
				return true;
			default:
				return false;
		}
	}

	private void refreshCurrentUsername()
	{
		final Player player = client.getLocalPlayer();
		detectedUsername = player == null ? null : RuneBoundUsername.cleanDisplayName(player.getName());
	}

	private void renderPanel(String status)
	{
		if (panel == null)
		{
			return;
		}

		final String username = activeUsername();
		final String targetUsername = activeTargetUsername();
		final Instant now = clock.instant();
		final Instant lastRefresh = targetUsername == null ? null : summaryCache.lastAttempt(targetUsername);
		final Duration remaining = targetUsername == null ? Duration.ZERO : summaryCache.remainingBeforeNextAttempt(targetUsername, now);
		panel.showModel(RuneBoundPanelModel.builder()
			.player(targetUsername == null ? username : targetUsername)
			.status(status)
			.lastLookup(lastRefresh)
			.cooldownRemaining(remaining)
			.networkLookupsEnabled(config.enableNetworkLookups())
			.openProfileEnabled(RuneBoundUsername.normalize(targetUsername == null ? username : targetUsername) != null)
			.build());
	}

	private String activeUsername()
	{
		return RuneBoundUsername.normalize(detectedUsername);
	}

	private String activeTargetUsername()
	{
		final String activeLookup = RuneBoundUsername.normalize(activeLookupUsername);
		return activeLookup == null ? activeUsername() : activeLookup;
	}

	private String selectedTargetUsername()
	{
		if (panel != null)
		{
			final String manual = RuneBoundUsername.normalize(panel.manualLookupUsername());
			if (manual != null)
			{
				return manual;
			}
		}

		return activeTargetUsername();
	}

	private void applySummaryResult(String username, RuneBoundSummaryResult result, String status)
	{
		if (panel == null)
		{
			return;
		}

		final Instant now = clock.instant();
		final Instant lastLookup = summaryCache.lastAttempt(username);
		final Duration remaining = summaryCache.remainingBeforeNextAttempt(username, now);
		final RuneBoundSummaryResponse response = result.getResponse();
		if (response != null)
		{
			activeProfileUrl = response.getProfileUrl();
			panel.showModel(response.toPanelModel(
				username,
				status,
				lastLookup,
				remaining,
				config.enableNetworkLookups()
			));
			return;
		}

		panel.showModel(RuneBoundPanelModel.builder()
			.player(username)
			.status(status)
			.lastLookup(lastLookup)
			.cooldownRemaining(remaining)
			.networkLookupsEnabled(config.enableNetworkLookups())
			.openProfileEnabled(RuneBoundUsername.normalize(username) != null)
			.build());
	}

	private String defaultStatus()
	{
		if (RuneBoundUsername.normalize(detectedUsername) == null)
		{
			return "Not logged in";
		}

		return config.enableNetworkLookups() ? "Ready" : "Network lookups disabled";
	}

	private static BufferedImage createIcon()
	{
		final int size = 16;
		final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(new Color(20, 24, 31));
		graphics.fillRoundRect(0, 0, size, size, 4, 4);
		graphics.setColor(new Color(77, 194, 255));
		graphics.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawLine(4, 12, 4, 4);
		graphics.drawLine(4, 4, 10, 4);
		graphics.drawLine(10, 4, 12, 6);
		graphics.drawLine(12, 6, 10, 8);
		graphics.drawLine(4, 8, 10, 8);
		graphics.drawLine(8, 8, 12, 12);
		graphics.setColor(new Color(245, 178, 66));
		graphics.fillOval(10, 10, 4, 4);
		graphics.dispose();
		return image;
	}

	@Provides
	RuneBoundConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneBoundConfig.class);
	}
}
