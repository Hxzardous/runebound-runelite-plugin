package com.runebound.runelite;

import java.time.Duration;

final class RuneBoundBackoffPolicy
{
	private static final Duration INITIAL_FAILURE_BACKOFF = Duration.ofMinutes(5);
	private static final Duration MAX_FAILURE_BACKOFF = Duration.ofMinutes(60);

	private RuneBoundBackoffPolicy()
	{
	}

	static Duration delayForFailureCount(int failureCount)
	{
		if (failureCount <= 0)
		{
			return Duration.ZERO;
		}

		long minutes = INITIAL_FAILURE_BACKOFF.toMinutes();
		for (int index = 1; index < failureCount; index++)
		{
			minutes *= 2;
			if (minutes >= MAX_FAILURE_BACKOFF.toMinutes())
			{
				return MAX_FAILURE_BACKOFF;
			}
		}

		return Duration.ofMinutes(minutes);
	}
}
