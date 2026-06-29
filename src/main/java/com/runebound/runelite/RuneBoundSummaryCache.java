package com.runebound.runelite;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class RuneBoundSummaryCache
{
	static final Duration SUCCESS_TTL = Duration.ofMinutes(15);
	static final Duration MIN_POLL_INTERVAL = Duration.ofMinutes(30);

	private final Map<String, RuneBoundCacheEntry> entries = new ConcurrentHashMap<>();
	private final Map<String, Instant> lastAttempts = new ConcurrentHashMap<>();

	void markAttempt(String username, Instant now)
	{
		lastAttempts.put(RuneBoundUsername.cacheKey(username), now);
	}

	Instant lastAttempt(String username)
	{
		final String normalized = RuneBoundUsername.normalize(username);
		return normalized == null ? null : lastAttempts.get(RuneBoundUsername.cacheKey(normalized));
	}

	Duration remainingBeforeNextAttempt(String username, Instant now)
	{
		final Instant lastAttempt = lastAttempt(username);
		if (lastAttempt == null)
		{
			return Duration.ZERO;
		}

		final Duration elapsed = Duration.between(lastAttempt, now);
		if (!elapsed.minus(MIN_POLL_INTERVAL).isNegative())
		{
			return Duration.ZERO;
		}

		return MIN_POLL_INTERVAL.minus(elapsed);
	}

	void put(String username, RuneBoundSummaryResult result, Instant now)
	{
		if (result == null)
		{
			throw new IllegalArgumentException("result is required");
		}

		entries.put(RuneBoundUsername.cacheKey(username), new RuneBoundCacheEntry(result, now, now.plus(SUCCESS_TTL)));
	}

	RuneBoundSummaryResult freshResult(String username, Instant now)
	{
		final String normalized = RuneBoundUsername.normalize(username);
		if (normalized == null)
		{
			return null;
		}

		final RuneBoundCacheEntry entry = entries.get(RuneBoundUsername.cacheKey(normalized));
		if (entry == null || entry.expiresAt.isBefore(now) || entry.expiresAt.equals(now))
		{
			return null;
		}

		return entry.result;
	}

	RuneBoundSummaryResult latestResult(String username)
	{
		final String normalized = RuneBoundUsername.normalize(username);
		if (normalized == null)
		{
			return null;
		}

		final RuneBoundCacheEntry entry = entries.get(RuneBoundUsername.cacheKey(normalized));
		return entry == null ? null : entry.result;
	}

	private static final class RuneBoundCacheEntry
	{
		private final RuneBoundSummaryResult result;
		private final Instant expiresAt;

		private RuneBoundCacheEntry(RuneBoundSummaryResult result, Instant fetchedAt, Instant expiresAt)
		{
			this.result = result;
			this.expiresAt = expiresAt;
		}
	}
}
