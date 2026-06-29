package com.runebound.runelite;

import java.util.Locale;

final class RuneBoundUsername
{
	private static final int MAX_LOOKUP_USERNAME_LENGTH = 64;

	private RuneBoundUsername()
	{
	}

	static String normalize(String username)
	{
		if (username == null)
		{
			return null;
		}

		final String trimmed = username.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	static String cleanDisplayName(String name)
	{
		final String normalized = normalize(name);
		if (normalized == null)
		{
			return null;
		}

		return normalize(normalized.replaceAll("<[^>]*>", ""));
	}

	static boolean isLookupCandidate(String username)
	{
		final String normalized = normalize(username);
		return normalized != null && normalized.length() <= MAX_LOOKUP_USERNAME_LENGTH;
	}

	static String cacheKey(String username)
	{
		final String normalized = normalize(username);
		if (normalized == null)
		{
			throw new IllegalArgumentException("username is required");
		}

		return normalized.toLowerCase(Locale.ROOT);
	}
}
