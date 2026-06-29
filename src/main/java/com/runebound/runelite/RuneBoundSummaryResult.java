package com.runebound.runelite;

import com.google.gson.Gson;

final class RuneBoundSummaryResult
{
	private final boolean networkAttempted;
	private final int httpStatusCode;
	private final RuneBoundSummaryStatus status;
	private final String message;
	private final String body;
	private final RuneBoundSummaryResponse response;

	private RuneBoundSummaryResult(
		boolean networkAttempted,
		int httpStatusCode,
		RuneBoundSummaryStatus status,
		String message,
		String body,
		RuneBoundSummaryResponse response
	)
	{
		this.networkAttempted = networkAttempted;
		this.httpStatusCode = httpStatusCode;
		this.status = status;
		this.message = message;
		this.body = body;
		this.response = response;
	}

	static RuneBoundSummaryResult fromHttpStatus(int statusCode, String body, Gson gson)
	{
		if (gson == null)
		{
			throw new IllegalArgumentException("gson is required");
		}

		final RuneBoundSummaryResponse parsed = RuneBoundSummaryResponse.parse(body, gson);
		if (statusCode == 200)
		{
			if (parsed == null)
			{
				return new RuneBoundSummaryResult(true, statusCode, RuneBoundSummaryStatus.MALFORMED_CACHE, "RuneBound summary unavailable", body, null);
			}

			return new RuneBoundSummaryResult(true, statusCode, parsed.mappedStatus(), parsed.statusMessage(), body, parsed);
		}

		if (statusCode == 400)
		{
			return new RuneBoundSummaryResult(true, statusCode, RuneBoundSummaryStatus.INVALID_USERNAME, "Invalid username", body, parsed);
		}

		if (statusCode == 404)
		{
			if (parsed == null)
			{
				return new RuneBoundSummaryResult(true, statusCode, RuneBoundSummaryStatus.SERVER_ERROR, "RuneBound summary endpoint unavailable", body, null);
			}

			return new RuneBoundSummaryResult(true, statusCode, RuneBoundSummaryStatus.NOT_CACHED, "No cached RuneBound summary", body, parsed);
		}

		if (statusCode == 429)
		{
			return new RuneBoundSummaryResult(true, statusCode, RuneBoundSummaryStatus.RATE_LIMITED, "RuneBound rate limit active", body, parsed);
		}

		if (statusCode >= 500)
		{
			return new RuneBoundSummaryResult(true, statusCode, RuneBoundSummaryStatus.SERVER_ERROR, "RuneBound summary service unavailable", body, parsed);
		}

		return new RuneBoundSummaryResult(true, statusCode, RuneBoundSummaryStatus.SERVER_ERROR, "RuneBound summary request failed", body, parsed);
	}

	static RuneBoundSummaryResult failedBeforeNetwork()
	{
		return new RuneBoundSummaryResult(false, 0, RuneBoundSummaryStatus.OFFLINE, "Summary lookup could not reach RuneBound", "", null);
	}

	static RuneBoundSummaryResult responseTooLarge(int statusCode)
	{
		return new RuneBoundSummaryResult(true, statusCode, RuneBoundSummaryStatus.MALFORMED_CACHE, "RuneBound summary response was too large", "", null);
	}

	boolean isNetworkAttempted()
	{
		return networkAttempted;
	}

	int getHttpStatusCode()
	{
		return httpStatusCode;
	}

	RuneBoundSummaryStatus getStatus()
	{
		return status;
	}

	String getMessage()
	{
		return message;
	}

	String getBody()
	{
		return body;
	}

	RuneBoundSummaryResponse getResponse()
	{
		return response;
	}
}
