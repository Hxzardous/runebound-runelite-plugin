package com.runebound.runelite;

import java.time.Duration;
import java.time.Instant;

final class RuneBoundPanelModel
{
	private final String player;
	private final String normalizedUsername;
	private final String profileUrl;
	private final String accountType;
	private final String buildType;
	private final String currentTitle;
	private final String tier;
	private final String badge;
	private final String boundPoints;
	private final String totalLevel;
	private final String totalXp;
	private final String recentAchievements;
	private final String freshness;
	private final String status;
	private final Instant lastLookup;
	private final Duration cooldownRemaining;
	private final boolean networkLookupsEnabled;
	private final boolean openProfileEnabled;

	private RuneBoundPanelModel(Builder builder)
	{
		this.player = builder.player;
		this.normalizedUsername = builder.normalizedUsername;
		this.profileUrl = builder.profileUrl;
		this.accountType = builder.accountType;
		this.buildType = builder.buildType;
		this.currentTitle = builder.currentTitle;
		this.tier = builder.tier;
		this.badge = builder.badge;
		this.boundPoints = builder.boundPoints;
		this.totalLevel = builder.totalLevel;
		this.totalXp = builder.totalXp;
		this.recentAchievements = builder.recentAchievements;
		this.freshness = builder.freshness;
		this.status = builder.status;
		this.lastLookup = builder.lastLookup;
		this.cooldownRemaining = builder.cooldownRemaining;
		this.networkLookupsEnabled = builder.networkLookupsEnabled;
		this.openProfileEnabled = builder.openProfileEnabled;
	}

	static Builder builder()
	{
		return new Builder();
	}

	String getPlayer()
	{
		return player;
	}

	String getNormalizedUsername()
	{
		return normalizedUsername;
	}

	String getProfileUrl()
	{
		return profileUrl;
	}

	String getAccountType()
	{
		return accountType;
	}

	String getBuildType()
	{
		return buildType;
	}

	String getCurrentTitle()
	{
		return currentTitle;
	}

	String getTier()
	{
		return tier;
	}

	String getBadge()
	{
		return badge;
	}

	String getBoundPoints()
	{
		return boundPoints;
	}

	String getTotalLevel()
	{
		return totalLevel;
	}

	String getTotalXp()
	{
		return totalXp;
	}

	String getRecentAchievements()
	{
		return recentAchievements;
	}

	String getFreshness()
	{
		return freshness;
	}

	String getStatus()
	{
		return status;
	}

	Instant getLastLookup()
	{
		return lastLookup;
	}

	Duration getCooldownRemaining()
	{
		return cooldownRemaining;
	}

	boolean isNetworkLookupsEnabled()
	{
		return networkLookupsEnabled;
	}

	boolean hasProfile()
	{
		return RuneBoundUsername.normalize(player) != null;
	}

	boolean canOpenProfile()
	{
		return openProfileEnabled;
	}

	static final class Builder
	{
		private String player;
		private String normalizedUsername;
		private String profileUrl;
		private String accountType;
		private String buildType;
		private String currentTitle;
		private String tier;
		private String badge;
		private String boundPoints;
		private String totalLevel;
		private String totalXp;
		private String recentAchievements;
		private String freshness;
		private String status;
		private Instant lastLookup;
		private Duration cooldownRemaining = Duration.ZERO;
		private boolean networkLookupsEnabled;
		private boolean openProfileEnabled;

		Builder player(String player)
		{
			this.player = player;
			return this;
		}

		Builder normalizedUsername(String normalizedUsername)
		{
			this.normalizedUsername = normalizedUsername;
			return this;
		}

		Builder profileUrl(String profileUrl)
		{
			this.profileUrl = profileUrl;
			return this;
		}

		Builder accountType(String accountType)
		{
			this.accountType = accountType;
			return this;
		}

		Builder buildType(String buildType)
		{
			this.buildType = buildType;
			return this;
		}

		Builder currentTitle(String currentTitle)
		{
			this.currentTitle = currentTitle;
			return this;
		}

		Builder tier(String tier)
		{
			this.tier = tier;
			return this;
		}

		Builder badge(String badge)
		{
			this.badge = badge;
			return this;
		}

		Builder boundPoints(String boundPoints)
		{
			this.boundPoints = boundPoints;
			return this;
		}

		Builder totalLevel(String totalLevel)
		{
			this.totalLevel = totalLevel;
			return this;
		}

		Builder totalXp(String totalXp)
		{
			this.totalXp = totalXp;
			return this;
		}

		Builder recentAchievements(String recentAchievements)
		{
			this.recentAchievements = recentAchievements;
			return this;
		}

		Builder freshness(String freshness)
		{
			this.freshness = freshness;
			return this;
		}

		Builder status(String status)
		{
			this.status = status;
			return this;
		}

		Builder lastLookup(Instant lastLookup)
		{
			this.lastLookup = lastLookup;
			return this;
		}

		Builder cooldownRemaining(Duration cooldownRemaining)
		{
			this.cooldownRemaining = cooldownRemaining;
			return this;
		}

		Builder networkLookupsEnabled(boolean networkLookupsEnabled)
		{
			this.networkLookupsEnabled = networkLookupsEnabled;
			return this;
		}

		Builder openProfileEnabled(boolean openProfileEnabled)
		{
			this.openProfileEnabled = openProfileEnabled;
			return this;
		}

		RuneBoundPanelModel build()
		{
			return new RuneBoundPanelModel(this);
		}
	}
}
