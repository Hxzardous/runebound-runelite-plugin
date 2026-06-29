package com.runebound.runelite;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class RuneBoundUrls
{
	private static final String HOST = "rune-bound.net";
	private static final String PROFILE_BASE_URL = "https://rune-bound.net/player/";
	private static final String SUMMARY_BASE_URL = "https://rune-bound.net/api/runelite/v1/players/";
	private static final String SEARCH_URL = "https://rune-bound.net/search";

	private RuneBoundUrls()
	{
	}

	static String profileUrl(String username)
	{
		return PROFILE_BASE_URL + encodePathSegment(username);
	}

	static String summaryUrl(String username)
	{
		return SUMMARY_BASE_URL + encodePathSegment(username) + "/summary";
	}

	static String searchUrl()
	{
		return SEARCH_URL;
	}

	static boolean isSafeProfileUrl(String url)
	{
		try
		{
			final URI uri = new URI(url == null ? "" : url);
			final String path = uri.getPath();
			return "https".equals(uri.getScheme()) &&
				HOST.equals(uri.getHost()) &&
				path != null &&
				path.startsWith("/player/") &&
				path.length() > "/player/".length() &&
				path.equals(uri.normalize().getPath()) &&
				uri.getQuery() == null &&
				uri.getFragment() == null;
		}
		catch (URISyntaxException exception)
		{
			return false;
		}
	}

	private static String encodePathSegment(String username)
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

		return URLEncoder.encode(normalizedUsername, StandardCharsets.UTF_8).replace("+", "%20");
	}
}
