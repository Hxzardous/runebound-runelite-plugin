package com.runebound.runelite;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class RuneBoundCooldown
{
	private final Duration cooldown;
	private final Map<String, Instant> lastRequests = new ConcurrentHashMap<>();

	RuneBoundCooldown(Duration cooldown)
	{
		if (cooldown == null || cooldown.isNegative() || cooldown.isZero())
		{
			throw new IllegalArgumentException("cooldown must be positive");
		}

		this.cooldown = cooldown;
	}

	void markRequested(String username, Instant now)
	{
		lastRequests.put(key(username), now);
	}

	Instant lastRequest(String username)
	{
		return lastRequests.get(key(username));
	}

	Duration remaining(String username, Instant now)
	{
		final Instant lastRequest = lastRequest(username);
		if (lastRequest == null)
		{
			return Duration.ZERO;
		}

		final Duration elapsed = Duration.between(lastRequest, now);
		if (!elapsed.minus(cooldown).isNegative())
		{
			return Duration.ZERO;
		}

		return cooldown.minus(elapsed);
	}

	private static String key(String username)
	{
		final String normalized = RuneBoundUsername.normalize(username);
		if (normalized == null)
		{
			throw new IllegalArgumentException("username is required");
		}

		return normalized.toLowerCase(Locale.ROOT);
	}
}
