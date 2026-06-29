package com.runebound.runelite;

import java.util.function.Consumer;

final class RuneBoundSummaryClient
{
	private final RuneBoundHttpTransport transport;

	RuneBoundSummaryClient(RuneBoundHttpTransport transport)
	{
		if (transport == null)
		{
			throw new IllegalArgumentException("transport is required");
		}

		this.transport = transport;
	}

	void requestSummary(String username, Consumer<RuneBoundSummaryResult> callback)
	{
		final String normalizedUsername = RuneBoundUsername.normalize(username);
		if (normalizedUsername == null)
		{
			throw new IllegalArgumentException("username is required");
		}

		if (!RuneBoundUsername.isLookupCandidate(normalizedUsername))
		{
			throw new IllegalArgumentException("username is too long");
		}

		if (callback == null)
		{
			throw new IllegalArgumentException("callback is required");
		}

		transport.getJson(RuneBoundUrls.summaryUrl(normalizedUsername), callback);
	}
}
