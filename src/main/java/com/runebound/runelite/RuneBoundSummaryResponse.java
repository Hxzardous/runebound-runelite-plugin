package com.runebound.runelite;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class RuneBoundSummaryResponse
{
	static final String SCHEMA_VERSION = "runebound.runelite.profile.summary.v1";
	private static final int MAX_RECENT_ACHIEVEMENTS = 3;
	private static final int MAX_STRING_LENGTH = 160;
	private static final int MAX_URL_LENGTH = 512;

	private final String statusCode;
	private final String statusLabel;
	private final boolean safeToDisplay;
	private final String displayName;
	private final String normalizedUsername;
	private final String profileUrl;
	private final String accountType;
	private final String buildType;
	private final String currentTitle;
	private final String tier;
	private final String badge;
	private final Long boundPoints;
	private final Long totalLevel;
	private final Long totalXp;
	private final List<String> recentAchievements;
	private final String freshnessLabel;
	private final String womFreshnessLabel;
	private final String runeboundFreshnessLabel;
	private final boolean mayOpenProfileUrl;

	private RuneBoundSummaryResponse(
		String statusCode,
		String statusLabel,
		boolean safeToDisplay,
		String displayName,
		String normalizedUsername,
		String profileUrl,
		String accountType,
		String buildType,
		String currentTitle,
		String tier,
		String badge,
		Long boundPoints,
		Long totalLevel,
		Long totalXp,
		List<String> recentAchievements,
		String freshnessLabel,
		String womFreshnessLabel,
		String runeboundFreshnessLabel,
		boolean mayOpenProfileUrl
	)
	{
		this.statusCode = statusCode;
		this.statusLabel = statusLabel;
		this.safeToDisplay = safeToDisplay;
		this.displayName = displayName;
		this.normalizedUsername = normalizedUsername;
		this.profileUrl = profileUrl;
		this.accountType = accountType;
		this.buildType = buildType;
		this.currentTitle = currentTitle;
		this.tier = tier;
		this.badge = badge;
		this.boundPoints = boundPoints;
		this.totalLevel = totalLevel;
		this.totalXp = totalXp;
		this.recentAchievements = Collections.unmodifiableList(new ArrayList<>(recentAchievements));
		this.freshnessLabel = freshnessLabel;
		this.womFreshnessLabel = womFreshnessLabel;
		this.runeboundFreshnessLabel = runeboundFreshnessLabel;
		this.mayOpenProfileUrl = mayOpenProfileUrl;
	}

	static RuneBoundSummaryResponse parse(String json, Gson gson)
	{
		if (gson == null)
		{
			throw new IllegalArgumentException("gson is required");
		}

		try
		{
			final JsonElement element = gson.fromJson(json == null ? "" : json, JsonElement.class);
			if (!element.isJsonObject())
			{
				return null;
			}

			final JsonObject root = element.getAsJsonObject();
			if (!SCHEMA_VERSION.equals(text(root, "schemaVersion")))
			{
				return null;
			}

			final JsonObject status = object(root, "status");
			final JsonObject player = object(root, "player");
			final JsonObject account = object(player, "account");
			final JsonObject summary = object(root, "summary");
			final JsonObject currentTitle = object(summary, "currentTitle");
			final JsonObject tier = object(summary, "tier");
			final JsonObject boundPoints = object(summary, "boundPoints");
			final JsonObject stats = object(summary, "stats");
			final JsonObject recentAchievements = object(summary, "recentAchievements");
			final JsonObject freshness = object(root, "freshness");
			final JsonObject wom = object(freshness, "wom");
			final JsonObject runebound = object(freshness, "runebound");
			final JsonObject client = object(root, "client");

			return new RuneBoundSummaryResponse(
				coalesce(text(status, "code"), "server_error"),
				coalesce(text(status, "label"), "RuneBound summary unavailable"),
				booleanValue(root, "safeToDisplay"),
				coalesce(text(root, "displayName"), text(player, "displayName")),
				coalesce(text(root, "normalizedUsername"), text(player, "normalizedUsername")),
				coalesce(urlText(root, "profileUrl"), urlText(player, "profileUrl")),
				coalesce(text(root, "accountType"), text(account, "selectedAccountType")),
				coalesce(text(root, "buildType"), text(account, "womBuild")),
				text(currentTitle, "name"),
				text(tier, "name"),
				coalesce(text(object(summary, "badge"), "name"), text(object(summary, "currentBadge"), "name")),
				coalesce(number(boundPoints, "lifetimeEarned"), number(boundPoints, "available")),
				number(stats, "totalLevel"),
				number(stats, "totalXp"),
				achievementTitles(recentAchievements),
				text(freshness, "trustLabel"),
				text(wom, "label"),
				text(runebound, "label"),
				booleanValue(client, "mayOpenProfileUrl")
			);
		}
		catch (IllegalStateException | JsonParseException exception)
		{
			return null;
		}
	}

	RuneBoundSummaryStatus mappedStatus()
	{
		switch (statusCode)
		{
			case "ok":
				return RuneBoundSummaryStatus.OK;
			case "not_cached":
				return RuneBoundSummaryStatus.NOT_CACHED;
			case "invalid_username":
				return RuneBoundSummaryStatus.INVALID_USERNAME;
			case "stale":
				return RuneBoundSummaryStatus.STALE;
			case "dirty":
				return RuneBoundSummaryStatus.DIRTY;
			case "malformed_cache":
				return RuneBoundSummaryStatus.MALFORMED_CACHE;
			default:
				return RuneBoundSummaryStatus.SERVER_ERROR;
		}
	}

	String statusMessage()
	{
		switch (mappedStatus())
		{
			case OK:
				return "Cached RuneBound profile";
			case STALE:
				return "Cached RuneBound profile may be stale";
			case DIRTY:
				return "Cached RuneBound profile needs freshness review";
			case NOT_CACHED:
				return "No cached RuneBound summary";
			case INVALID_USERNAME:
				return "Invalid username";
			case MALFORMED_CACHE:
				return "RuneBound summary unavailable";
			default:
				return statusLabel;
		}
	}

	RuneBoundPanelModel toPanelModel(
		String fallbackUsername,
		String status,
		java.time.Instant lastLookup,
		java.time.Duration cooldownRemaining,
		boolean networkLookupsEnabled
	)
	{
		final boolean displayDetails = safeToDisplay;
		return RuneBoundPanelModel.builder()
			.player(coalesce(displayName, fallbackUsername))
			.normalizedUsername(normalizedUsername)
			.profileUrl(profileUrl)
			.accountType(displayDetails ? accountType : null)
			.buildType(displayDetails ? buildType : null)
			.currentTitle(displayDetails ? currentTitle : null)
			.tier(displayDetails ? tier : null)
			.badge(displayDetails ? badge : null)
			.boundPoints(displayDetails ? formatNumber(boundPoints) : null)
			.totalLevel(displayDetails ? formatNumber(totalLevel) : null)
			.totalXp(displayDetails ? formatNumber(totalXp) : null)
			.recentAchievements(displayDetails && !recentAchievements.isEmpty() ? String.join("\n", recentAchievements) : null)
			.freshness(formatFreshness())
			.status(status)
			.lastLookup(lastLookup)
			.cooldownRemaining(cooldownRemaining)
			.networkLookupsEnabled(networkLookupsEnabled)
			.openProfileEnabled(canOpenProfile())
			.build();
	}

	boolean canOpenProfile()
	{
		return mayOpenProfileUrl && RuneBoundUrls.isSafeProfileUrl(profileUrl) && mappedStatus() != RuneBoundSummaryStatus.INVALID_USERNAME;
	}

	String getDisplayName()
	{
		return displayName;
	}

	String getNormalizedUsername()
	{
		return normalizedUsername;
	}

	String getProfileUrl()
	{
		return profileUrl;
	}

	String getStatusCode()
	{
		return statusCode;
	}

	String getStatusLabel()
	{
		return statusLabel;
	}

	boolean isSafeToDisplay()
	{
		return safeToDisplay;
	}

	private String formatFreshness()
	{
		final String primary = cleanFreshnessLabel(freshnessLabel);
		if (primary != null)
		{
			return primary;
		}

		final String wom = cleanFreshnessLabel(womFreshnessLabel);
		final String runebound = cleanFreshnessLabel(runeboundFreshnessLabel);
		if ("Stale".equals(wom) || "Stale".equals(runebound))
		{
			return "Stale";
		}
		if ("Fresh".equals(wom) || "Fresh".equals(runebound))
		{
			return "Fresh";
		}
		if ("Not cached".equals(wom) || "Not cached".equals(runebound))
		{
			return "Not cached";
		}
		if ("Unavailable".equals(wom) || "Unavailable".equals(runebound))
		{
			return "Unavailable";
		}
		return null;
	}

	private static String cleanFreshnessLabel(String label)
	{
		if (label == null)
		{
			return null;
		}
		if ("trusted_cached".equals(label))
		{
			return "Fresh";
		}
		if ("stale_cached".equals(label))
		{
			return "Stale";
		}
		if ("dirty_unknown".equals(label))
		{
			return "Stale";
		}
		if ("not_cached".equals(label))
		{
			return "Not cached";
		}
		if ("unavailable".equals(label))
		{
			return "Unavailable";
		}
		if ("WOM Fresh".equals(label))
		{
			return "Fresh";
		}
		if ("WOM Aging".equals(label))
		{
			return "Stale";
		}
		if ("WOM Outdated".equals(label))
		{
			return "Stale";
		}
		if ("WOM Unknown".equals(label))
		{
			return "Unavailable";
		}
		if ("RB Synced".equals(label))
		{
			return "Fresh";
		}
		if ("RB Aging".equals(label))
		{
			return "Stale";
		}
		if ("RB Outdated".equals(label))
		{
			return "Stale";
		}
		if ("RB Unknown".equals(label))
		{
			return "Unavailable";
		}
		return null;
	}

	private static List<String> achievementTitles(JsonObject recentAchievements)
	{
		if (recentAchievements == null)
		{
			return Collections.emptyList();
		}

		final JsonElement itemsElement = recentAchievements.get("items");
		if (itemsElement == null || !itemsElement.isJsonArray())
		{
			return Collections.emptyList();
		}

		final JsonArray items = itemsElement.getAsJsonArray();
		final List<String> titles = new ArrayList<>();
		for (JsonElement itemElement : items)
		{
			if (!itemElement.isJsonObject())
			{
				continue;
			}

			final String title = text(itemElement.getAsJsonObject(), "title");
			if (title != null)
			{
				titles.add(title);
			}

			if (titles.size() >= MAX_RECENT_ACHIEVEMENTS)
			{
				break;
			}
		}
		return titles;
	}

	private static JsonObject object(JsonObject object, String field)
	{
		if (object == null)
		{
			return null;
		}

		final JsonElement element = object.get(field);
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
	}

	private static String text(JsonObject object, String field)
	{
		return boundedText(object, field, MAX_STRING_LENGTH);
	}

	private static String urlText(JsonObject object, String field)
	{
		return boundedText(object, field, MAX_URL_LENGTH);
	}

	private static String boundedText(JsonObject object, String field, int maxLength)
	{
		if (object == null)
		{
			return null;
		}

		final JsonElement element = object.get(field);
		if (element == null || element.isJsonNull() || !element.isJsonPrimitive())
		{
			return null;
		}

		final String value = element.getAsString();
		final String normalized = RuneBoundUsername.normalize(value);
		if (normalized == null || normalized.length() > maxLength)
		{
			return null;
		}

		return normalized;
	}

	private static Long number(JsonObject object, String field)
	{
		if (object == null)
		{
			return null;
		}

		final JsonElement element = object.get(field);
		if (element == null || element.isJsonNull() || !element.isJsonPrimitive())
		{
			return null;
		}

		try
		{
			return element.getAsLong();
		}
		catch (NumberFormatException exception)
		{
			return null;
		}
	}

	private static boolean booleanValue(JsonObject object, String field)
	{
		if (object == null)
		{
			return false;
		}

		final JsonElement element = object.get(field);
		return element != null && element.isJsonPrimitive() && element.getAsBoolean();
	}

	private static String coalesce(String first, String second)
	{
		return first != null ? first : second;
	}

	private static Long coalesce(Long first, Long second)
	{
		return first != null ? first : second;
	}

	private static String formatNumber(Long value)
	{
		return value == null ? null : NumberFormat.getIntegerInstance(Locale.US).format(value);
	}
}
