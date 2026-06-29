package com.runebound.runelite;

import com.google.gson.Gson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;

public class RuneBoundSafetyTest
{
	private static final Gson GSON = new Gson();

	@Test
	public void blankUsernameIsIgnored()
	{
		assertNull(RuneBoundUsername.normalize(" "));
		assertNull(RuneBoundUsername.normalize(null));
		assertEquals("Name With Space", RuneBoundUsername.normalize("  Name With Space  "));
		assertTrue(RuneBoundUsername.isLookupCandidate("Name With Space"));
		assertFalse(RuneBoundUsername.isLookupCandidate(repeat("x", 65)));
	}

	@Test
	public void cleanDisplayNameStripsRuneLiteTags()
	{
		assertEquals("RuneBounder", RuneBoundUsername.cleanDisplayName("<col=ffffff>RuneBounder</col>"));
	}

	@Test
	public void cooldownBlocksRepeatedSummaryLookups()
	{
		final RuneBoundCooldown cooldown = new RuneBoundCooldown(Duration.ofMinutes(30));
		final Instant now = Instant.parse("2026-06-22T12:00:00Z");

		cooldown.markRequested("SampleUser", now);

		assertEquals(Duration.ofMinutes(30), cooldown.remaining("sampleuser", now));
		assertEquals(Duration.ofMinutes(15), cooldown.remaining("SampleUser", now.plus(Duration.ofMinutes(15))));
		assertEquals(Duration.ZERO, cooldown.remaining("SampleUser", now.plus(Duration.ofMinutes(31))));
	}

	@Test
	public void openProfileUrlEncodesUsernameSafely()
	{
		assertEquals("https://rune-bound.net/player/Name%20With%20Space", RuneBoundUrls.profileUrl("Name With Space"));
		assertEquals("https://rune-bound.net/player/Name%2FSlash", RuneBoundUrls.profileUrl("Name/Slash"));
	}

	@Test
	public void onlyRuneBoundProfileUrlsAreTrustedForBrowserOpen()
	{
		assertTrue(RuneBoundUrls.isSafeProfileUrl("https://rune-bound.net/player/RuneBounder"));
		assertTrue(RuneBoundUrls.isSafeProfileUrl("https://rune-bound.net/player/Name%20With%20Space"));
		assertFalse(RuneBoundUrls.isSafeProfileUrl("http://rune-bound.net/player/RuneBounder"));
		assertFalse(RuneBoundUrls.isSafeProfileUrl("https://evil.example/player/RuneBounder"));
		assertFalse(RuneBoundUrls.isSafeProfileUrl("https://rune-bound.net/search"));
		assertFalse(RuneBoundUrls.isSafeProfileUrl("https://rune-bound.net/player/"));
		assertFalse(RuneBoundUrls.isSafeProfileUrl("https://rune-bound.net/player/../search"));
		assertFalse(RuneBoundUrls.isSafeProfileUrl("https://rune-bound.net/player/RuneBounder?x=1"));
		assertFalse(RuneBoundUrls.isSafeProfileUrl("https://rune-bound.net/player/RuneBounder#fragment"));
	}

	@Test
	public void blankProfileUrlIsBlocked()
	{
		try
		{
			RuneBoundUrls.profileUrl(" ");
		}
		catch (IllegalArgumentException expected)
		{
			return;
		}

		throw new AssertionError("Expected blank profile usernames to be rejected");
	}

	@Test
	public void summaryUrlUsesReadOnlyRuneBoundEndpoint()
	{
		assertEquals(
			"https://rune-bound.net/api/runelite/v1/players/Name%20With%20Space/summary",
			RuneBoundUrls.summaryUrl("Name With Space")
		);
		assertEquals(
			"https://rune-bound.net/api/runelite/v1/players/Name%2FSlash/summary",
			RuneBoundUrls.summaryUrl("Name/Slash")
		);
	}

	@Test
	public void summaryClientBuildsReadOnlyGetRequest()
	{
		final AtomicReference<String> capturedUrl = new AtomicReference<>();
		final RuneBoundHttpTransport transport = (url, callback) ->
		{
			capturedUrl.set(url);
			callback.accept(resultFromHttpStatus(200, okSummaryJson()));
		};

		final AtomicReference<RuneBoundSummaryResult> result = new AtomicReference<>();
		new RuneBoundSummaryClient(transport).requestSummary("SampleUser", result::set);

		assertEquals("https://rune-bound.net/api/runelite/v1/players/SampleUser/summary", capturedUrl.get());
		assertNotNull(result.get());
		assertTrue(result.get().isNetworkAttempted());
		assertEquals(RuneBoundSummaryStatus.OK, result.get().getStatus());
	}

	@Test
	public void summaryClientRejectsOversizedLookupUsername()
	{
		final AtomicReference<String> capturedUrl = new AtomicReference<>();
		final RuneBoundHttpTransport transport = (url, callback) -> capturedUrl.set(url);

		try
		{
			new RuneBoundSummaryClient(transport).requestSummary(repeat("x", 65), result -> { });
		}
		catch (IllegalArgumentException expected)
		{
			assertNull(capturedUrl.get());
			return;
		}

		throw new AssertionError("Expected oversized lookup username to be rejected");
	}

	@Test
	public void summaryResultMapsSafeStatuses()
	{
		assertEquals(RuneBoundSummaryStatus.OK, resultFromHttpStatus(200, okSummaryJson()).getStatus());
		assertEquals(RuneBoundSummaryStatus.STALE, resultFromHttpStatus(200, summaryJsonWithStatus("stale")).getStatus());
		assertEquals(RuneBoundSummaryStatus.DIRTY, resultFromHttpStatus(200, summaryJsonWithStatus("dirty")).getStatus());
		assertEquals(RuneBoundSummaryStatus.MALFORMED_CACHE, resultFromHttpStatus(200, summaryJsonWithStatus("malformed_cache")).getStatus());
		assertEquals(RuneBoundSummaryStatus.INVALID_USERNAME, resultFromHttpStatus(400, invalidUsernameJson()).getStatus());
		assertEquals(RuneBoundSummaryStatus.NOT_CACHED, resultFromHttpStatus(404, notCachedJson()).getStatus());
		assertEquals(RuneBoundSummaryStatus.RATE_LIMITED, resultFromHttpStatus(429, "{}").getStatus());
		assertEquals(RuneBoundSummaryStatus.SERVER_ERROR, resultFromHttpStatus(503, "{}").getStatus());
		assertEquals(RuneBoundSummaryStatus.OFFLINE, RuneBoundSummaryResult.failedBeforeNetwork().getStatus());
		assertEquals(RuneBoundSummaryStatus.MALFORMED_CACHE, RuneBoundSummaryResult.responseTooLarge(200).getStatus());
	}

	@Test
	public void successfulSummaryParseExposesProfileFields()
	{
		final RuneBoundSummaryResult result = resultFromHttpStatus(200, okSummaryJson());
		final RuneBoundSummaryResponse response = result.getResponse();

		assertNotNull(response);
		assertEquals("ok", response.getStatusCode());
		assertEquals("Cached RuneBound profile", response.getStatusLabel());
		assertTrue(response.isSafeToDisplay());
		assertEquals("RuneBounder", response.getDisplayName());
		assertEquals("runebounder", response.getNormalizedUsername());
		assertEquals("https://rune-bound.net/player/RuneBounder", response.getProfileUrl());
		assertEquals(RuneBoundSummaryStatus.OK, response.mappedStatus());
		assertEquals("Cached RuneBound profile", response.statusMessage());
	}

	@Test
	public void summaryPanelModelUsesParsedProfileFields()
	{
		final RuneBoundSummaryResponse response = resultFromHttpStatus(200, okSummaryJson()).getResponse();
		final RuneBoundPanelModel model = response.toPanelModel(
			"RuneBounder",
			response.statusMessage(),
			Instant.parse("2026-06-24T12:02:00Z"),
			Duration.ZERO,
			true
		);

		assertEquals("RuneBounder", model.getPlayer());
		assertEquals("runebounder", model.getNormalizedUsername());
		assertEquals("MAIN", model.getAccountType());
		assertEquals("main", model.getBuildType());
		assertEquals("10 HP Paragon", model.getCurrentTitle());
		assertEquals("Ascendant Bound", model.getTier());
		assertEquals("Legendary", model.getBadge());
		assertEquals("35,570", model.getBoundPoints());
		assertEquals("2,277", model.getTotalLevel());
		assertEquals("460,000,000", model.getTotalXp());
		assertEquals("Example Achievement", model.getRecentAchievements());
		assertEquals("trusted_cached / Source Freshness / RuneBound Freshness", model.getFreshness());
		assertTrue(model.canOpenProfile());
	}

	@Test
	public void recentAchievementsAreCappedForPanelDisplay()
	{
		final RuneBoundSummaryResponse response = resultFromHttpStatus(200, summaryJsonWithRecentAchievements(12)).getResponse();
		final RuneBoundPanelModel model = response.toPanelModel(
			"RuneBounder",
			response.statusMessage(),
			Instant.parse("2026-06-24T12:02:00Z"),
			Duration.ZERO,
			true
		);

		assertEquals(
			"Achievement 1\nAchievement 2\nAchievement 3\nAchievement 4\nAchievement 5",
			model.getRecentAchievements()
		);
	}

	@Test
	public void oversizedSummaryStringsAreIgnored()
	{
		final String oversizedJson = okSummaryJson().replace("Example Achievement", repeat("x", 161));
		final RuneBoundSummaryResponse response = resultFromHttpStatus(200, oversizedJson).getResponse();
		final RuneBoundPanelModel model = response.toPanelModel(
			"RuneBounder",
			response.statusMessage(),
			Instant.parse("2026-06-24T12:02:00Z"),
			Duration.ZERO,
			true
		);

		assertNull(model.getRecentAchievements());
	}

	@Test
	public void invalidUsernameResponseDisablesProfileOpen()
	{
		final RuneBoundSummaryResponse response = resultFromHttpStatus(400, invalidUsernameJson()).getResponse();
		final RuneBoundPanelModel model = response.toPanelModel(
			"Bad/Name",
			response.statusMessage(),
			Instant.parse("2026-06-24T12:02:00Z"),
			Duration.ZERO,
			true
		);

		assertFalse(model.canOpenProfile());
		assertEquals(RuneBoundSummaryStatus.INVALID_USERNAME, response.mappedStatus());
	}

	@Test
	public void unsafeSummaryProfileUrlDisablesProfileOpen()
	{
		final String unsafeJson = okSummaryJson().replace(
			"https://rune-bound.net/player/RuneBounder",
			"https://evil.example/player/RuneBounder"
		);
		final RuneBoundSummaryResponse response = resultFromHttpStatus(200, unsafeJson).getResponse();
		final RuneBoundPanelModel model = response.toPanelModel(
			"RuneBounder",
			response.statusMessage(),
			Instant.parse("2026-06-24T12:02:00Z"),
			Duration.ZERO,
			true
		);

		assertFalse(model.canOpenProfile());
	}

	@Test
	public void unsafeToDisplayResponseSuppressesProfileDetails()
	{
		final String unsafeJson = okSummaryJson().replace("\"safeToDisplay\":true", "\"safeToDisplay\":false");
		final RuneBoundSummaryResponse response = resultFromHttpStatus(200, unsafeJson).getResponse();
		final RuneBoundPanelModel model = response.toPanelModel(
			"RuneBounder",
			response.statusMessage(),
			Instant.parse("2026-06-24T12:02:00Z"),
			Duration.ZERO,
			true
		);

		assertEquals("RuneBounder", model.getPlayer());
		assertEquals("runebounder", model.getNormalizedUsername());
		assertNull(model.getCurrentTitle());
		assertNull(model.getTier());
		assertNull(model.getBadge());
		assertNull(model.getBoundPoints());
		assertNull(model.getTotalLevel());
		assertNull(model.getTotalXp());
		assertNull(model.getRecentAchievements());
	}

	@Test
	public void clientProfileOpenDirectiveIsHonored()
	{
		final String disallowedJson = okSummaryJson().replace("\"mayOpenProfileUrl\":true", "\"mayOpenProfileUrl\":false");
		final RuneBoundSummaryResponse response = resultFromHttpStatus(200, disallowedJson).getResponse();
		final RuneBoundPanelModel model = response.toPanelModel(
			"RuneBounder",
			response.statusMessage(),
			Instant.parse("2026-06-24T12:02:00Z"),
			Duration.ZERO,
			true
		);

		assertFalse(model.canOpenProfile());
	}

	@Test
	public void summaryCacheUsesFifteenMinuteTtlAndThirtyMinutePollFloor()
	{
		final RuneBoundSummaryCache cache = new RuneBoundSummaryCache();
		final Instant now = Instant.parse("2026-06-22T12:00:00Z");
		final RuneBoundSummaryResult result = resultFromHttpStatus(200, okSummaryJson());

		cache.markAttempt("SampleUser", now);
		cache.put("SampleUser", result, now);

		assertEquals(result, cache.freshResult("sampleuser", now.plus(Duration.ofMinutes(14))));
		assertNull(cache.freshResult("sampleuser", now.plus(Duration.ofMinutes(15))));
		assertEquals(result, cache.latestResult("sampleuser"));
		assertEquals(Duration.ofMinutes(30), cache.remainingBeforeNextAttempt("SampleUser", now));
		assertEquals(Duration.ZERO, cache.remainingBeforeNextAttempt("SampleUser", now.plus(Duration.ofMinutes(31))));
	}

	@Test
	public void backoffPolicyCapsAtOneHour()
	{
		assertEquals(Duration.ZERO, RuneBoundBackoffPolicy.delayForFailureCount(0));
		assertEquals(Duration.ofMinutes(5), RuneBoundBackoffPolicy.delayForFailureCount(1));
		assertEquals(Duration.ofMinutes(10), RuneBoundBackoffPolicy.delayForFailureCount(2));
		assertEquals(Duration.ofMinutes(60), RuneBoundBackoffPolicy.delayForFailureCount(10));
	}

	@Test
	public void okHttpTransportUsesAsyncGetOnly() throws IOException
	{
		final String source = read(Paths.get("src", "main", "java", "com", "runebound", "runelite", "RuneBoundOkHttpTransport.java"));
		final List<String> disallowedTerms = Arrays.asList(
			"openConnection",
			"java.net.http",
			".execute(",
			".post(",
			".string()",
			"Request" + "Body",
			"Socket"
		);

		assertTrue(source.contains(".get()"));
		assertTrue(source.contains(".enqueue("));
		assertTrue(source.contains(".request(MAX_RESPONSE_BYTES + 1L)"));
		assertTrue(source.contains(".connectTimeout(10, TimeUnit.SECONDS)"));
		assertTrue(source.contains(".readTimeout(10, TimeUnit.SECONDS)"));
		for (String term : disallowedTerms)
		{
			assertFalse("Unexpected blocking, POST, or non-OkHttp network marker: " + term, source.contains(term));
		}
	}

	@Test
	public void okHttpTransportRejectsOversizedResponses() throws Exception
	{
		final String oversizedBody = repeat("x", 128 * 1024 + 1);
		final OkHttpClient client = new OkHttpClient.Builder()
			.addInterceptor(chain -> new Response.Builder()
				.request(chain.request())
				.protocol(Protocol.HTTP_1_1)
				.code(200)
				.message("OK")
				.body(ResponseBody.create(MediaType.parse("application/json"), oversizedBody))
				.build())
			.build();
		final CompletableFuture<RuneBoundSummaryResult> result = new CompletableFuture<>();

		new RuneBoundOkHttpTransport(client, GSON).getJson(
			"https://rune-bound.net/api/runelite/v1/players/RuneBounder/summary",
			result::complete
		);

		final RuneBoundSummaryResult summaryResult = result.get(5, TimeUnit.SECONDS);
		assertEquals(RuneBoundSummaryStatus.MALFORMED_CACHE, summaryResult.getStatus());
		assertEquals("RuneBound summary response was too large", summaryResult.getMessage());
	}

	@Test
	public void sourceDoesNotContainPrivateCredentialMarkers() throws IOException
	{
		final List<String> disallowedTerms = Arrays.asList(
			"firebaseConfig",
			"service" + "_account",
			"serviceAccount",
			"private" + "_key",
			"wom" + "ApiKey",
			"WO" + "M_API_KEY",
			"AI" + "za"
		);

		try (Stream<Path> files = Files.walk(Paths.get("src", "main")))
		{
			final String source = files
				.filter(Files::isRegularFile)
				.map(RuneBoundSafetyTest::read)
				.collect(Collectors.joining("\n"));

			for (String term : disallowedTerms)
			{
				assertFalse("Unexpected private credential marker: " + term, source.contains(term));
			}
		}
	}

	@Test
	public void sourceAndDocsDoNotReferenceRemovedOverride() throws IOException
	{
		final List<String> disallowedTerms = Arrays.asList(
			"debug" + "UsernameOverride",
			"RuneBound" + "Debug" + "Username",
			"Debug " + "username override",
			"Using " + "debug " + "username override",
			"debug-" + "mode banner",
			"without " + "Jagex login claims"
		);
		final List<Path> roots = Arrays.asList(
			Paths.get("src", "main"),
			Paths.get("src", "test"),
			Paths.get("README.md"),
			Paths.get("CONTRIBUTING.md"),
			Paths.get("SECURITY.md"),
			Paths.get("runelite-plugin.properties")
		);

		final String text = readAllRegularFiles(roots);
		for (String term : disallowedTerms)
		{
			assertFalse("Removed override reference remains: " + term, text.contains(term));
		}
	}

	@Test
	public void readmeContainsRequiredPrivacyDisclosure() throws IOException
	{
		final String readme = read(Paths.get("README.md"));

		assertTrue(readme.contains("If enabled, the plugin may send the selected public RuneScape display name and normal HTTPS request metadata to RuneBound when the user clicks `Lookup` or `Refresh`."));
		assertTrue(readme.contains("GET https://rune-bound.net/api/runelite/v1/players/{username}/summary"));
		assertTrue(readme.contains("The plugin does not:"));
		assertTrue(readme.contains("Collect passwords or Jagex account data."));
		assertTrue(readme.contains("Collect private chat."));
		assertTrue(readme.contains("Collect inventory, bank, location, combat state, world, or gameplay actions."));
		assertTrue(readme.contains("Send hidden telemetry."));
	}

	@Test
	public void docsDescribeReadOnlySummaryEndpointOnly() throws IOException
	{
		final String readme = read(Paths.get("README.md"));

		assertTrue(readme.contains("Network summary lookups are disabled by default"));
		assertTrue(readme.contains("GET https://rune-bound.net/api/runelite/v1/players/{username}/summary"));
		assertTrue(readme.contains("Trigger profile updates directly."));
		assertFalse(readme.contains("https://rune-bound.net/api/players/{username}/update"));
	}

	@Test
	public void licenseExistsAndUsesBsdTwoClauseForRuneBoundContributors() throws IOException
	{
		final String license = read(Paths.get("LICENSE"));

		assertTrue(license.contains("BSD 2-Clause License"));
		assertTrue(license.contains("Copyright (c) 2026, RuneBound contributors"));
		assertTrue(license.contains("Redistribution and use in source and binary forms"));
	}

	@Test
	public void gradleKeepsJavaElevenToolchain() throws IOException
	{
		final String buildGradle = read(Paths.get("build.gradle"));

		assertTrue(buildGradle.contains("languageVersion = JavaLanguageVersion.of(11)"));
		assertTrue(buildGradle.contains("options.release.set(11)"));
	}

	@Test
	public void pluginPropertiesAreReadyForStandalonePlugin() throws IOException
	{
		final Properties properties = new Properties();
		try (java.io.Reader reader = Files.newBufferedReader(Paths.get("runelite-plugin.properties"), StandardCharsets.UTF_8))
		{
			properties.load(reader);
		}

		assertEquals("RuneBound", properties.getProperty("displayName"));
		assertEquals("RuneBound", properties.getProperty("author"));
		assertEquals("com.runebound.runelite.RuneBoundPlugin", properties.getProperty("plugins"));
		assertEquals("standard", properties.getProperty("build"));
		assertTrue(properties.getProperty("description").contains("read-only cache endpoint"));
	}

	@Test
	public void rootIconExistsAndFitsPluginHubSizeGuidance() throws IOException
	{
		final Path iconPath = Paths.get("icon.png");
		assertTrue("Root icon.png should exist", Files.isRegularFile(iconPath));

		final BufferedImage icon = ImageIO.read(iconPath.toFile());
		assertNotNull("Root icon.png should be readable", icon);
		assertTrue("Root icon.png width should be no larger than 48 px", icon.getWidth() <= 48);
		assertTrue("Root icon.png height should be no larger than 72 px", icon.getHeight() <= 72);
	}

	@Test
	public void repositoryDoesNotContainCredentialFiles() throws IOException
	{
		try (Stream<Path> files = Files.walk(Paths.get(".")))
		{
			final List<String> credentialFiles = files
				.filter(Files::isRegularFile)
				.map(path -> path.normalize().toString().replace('\\', '/'))
				.filter(path -> !path.startsWith("./.git/"))
				.filter(path -> !path.startsWith(".git/"))
				.filter(path -> !path.startsWith("./.gradle/"))
				.filter(path -> !path.startsWith(".gradle/"))
				.filter(path -> !path.startsWith("./build/"))
				.filter(path -> !path.startsWith("build/"))
				.filter(RuneBoundSafetyTest::isCredentialFileName)
				.collect(Collectors.toList());

			assertTrue("Credential-like files should not be present: " + credentialFiles, credentialFiles.isEmpty());
		}
	}

	@Test
	public void mainSourceDoesNotUseRestrictedPluginHubApis() throws IOException
	{
		final List<String> disallowedTerms = Arrays.asList(
			"java.lang.reflect",
			"Class.forName",
			"getDeclaredField",
			"getDeclaredMethod",
			"ProcessBuilder",
			"Runtime.getRuntime",
			"Unsafe",
			"System.load",
			"System.loadLibrary",
			"MenuEntry",
			"setMenuEntries",
			"invokeMenuAction",
			"GameTick",
			"ChatMessage",
			"openConnection",
			"java.net.http",
			".execute(",
			"Socket"
		);

		try (Stream<Path> files = Files.walk(Paths.get("src", "main", "java")))
		{
			final String source = files
				.filter(Files::isRegularFile)
				.map(RuneBoundSafetyTest::read)
				.collect(Collectors.joining("\n"));

			for (String term : disallowedTerms)
			{
				assertFalse("Unexpected restricted Plugin Hub/API marker: " + term, source.contains(term));
			}
		}
	}

	@Test
	public void mainSourceDoesNotCallExternalPrivilegedOrUpdateEndpoints() throws IOException
	{
		try (Stream<Path> files = Files.walk(Paths.get("src", "main", "java")))
		{
			final String source = files
				.filter(Files::isRegularFile)
				.map(RuneBoundSafetyTest::read)
				.collect(Collectors.joining("\n"))
				.toLowerCase();

			final List<String> disallowedTerms = Arrays.asList(
				"wise" + "oldman.net",
				"api.wise" + "oldman",
				"/api/ad" + "min",
				"ad" + "min_to" + "ken",
				"wom" + "_api",
				"/api/players/",
				".post("
			);

			for (String term : disallowedTerms)
			{
				assertFalse("Unexpected direct privileged or update endpoint marker: " + term, source.contains(term));
			}
		}
	}

	private static String read(Path path)
	{
		try
		{
			return Files.readString(path, StandardCharsets.UTF_8);
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Unable to read " + path, exception);
		}
	}

	private static RuneBoundSummaryResult resultFromHttpStatus(int statusCode, String body)
	{
		return RuneBoundSummaryResult.fromHttpStatus(statusCode, body, GSON);
	}

	private static String readAllRegularFiles(List<Path> roots) throws IOException
	{
		final StringBuilder text = new StringBuilder();
		for (Path root : roots)
		{
			if (Files.isRegularFile(root))
			{
				text.append(read(root)).append('\n');
				continue;
			}

			try (Stream<Path> files = Files.walk(root))
			{
				for (Path file : files.filter(Files::isRegularFile).collect(Collectors.toList()))
				{
					text.append(read(file)).append('\n');
				}
			}
		}

		return text.toString();
	}

	private static String repeat(String value, int count)
	{
		final StringBuilder builder = new StringBuilder(value.length() * count);
		for (int index = 0; index < count; index++)
		{
			builder.append(value);
		}
		return builder.toString();
	}

	private static boolean isCredentialFileName(String path)
	{
		final String lower = path.toLowerCase();
		return lower.endsWith(".env") ||
			lower.contains("/.env.") ||
			lower.endsWith(".pem") ||
			lower.endsWith(".p12") ||
			lower.endsWith(".pfx") ||
			lower.endsWith(".jks") ||
			lower.endsWith(".keystore") ||
			lower.contains("service-account") ||
			lower.contains("service" + "_account") ||
			lower.contains("serviceaccount");
	}

	private static String okSummaryJson()
	{
		return summaryJsonWithStatus("ok");
	}

	private static String summaryJsonWithStatus(String status)
	{
		return "{"
			+ "\"schemaVersion\":\"runebound.runelite.profile.summary.v1\","
			+ "\"source\":\"public_profile_cache\","
			+ "\"safeToDisplay\":true,"
			+ "\"status\":{\"code\":\"" + status + "\",\"label\":\"Cached RuneBound profile\",\"reason\":null},"
			+ "\"displayName\":\"RuneBounder\","
			+ "\"normalizedUsername\":\"runebounder\","
			+ "\"profileUrl\":\"https://rune-bound.net/player/RuneBounder\","
			+ "\"accountType\":\"MAIN\","
			+ "\"buildType\":\"main\","
			+ "\"player\":{"
			+ "\"username\":\"RuneBounder\","
			+ "\"displayName\":\"RuneBounder\","
			+ "\"normalizedUsername\":\"runebounder\","
			+ "\"profileUrl\":\"https://rune-bound.net/player/RuneBounder\","
			+ "\"account\":{\"selectedAccountType\":\"MAIN\",\"womType\":\"regular\",\"womBuild\":\"main\",\"combatLevel\":126}"
			+ "},"
			+ "\"summary\":{"
			+ "\"currentTitle\":{\"id\":\"title-ten-hp-paragon\",\"name\":\"10 HP Paragon\",\"rarity\":\"legendary\"},"
			+ "\"tier\":{\"name\":\"Ascendant Bound\",\"rank\":6},"
			+ "\"badge\":{\"name\":\"Legendary\"},"
			+ "\"boundPoints\":{\"lifetimeEarned\":35570,\"available\":35570},"
			+ "\"stats\":{\"totalLevel\":2277,\"totalXp\":460000000,\"totalEhp\":1500.5},"
			+ "\"recentAchievements\":{\"available\":true,\"countInResponse\":1,\"items\":[{\"id\":\"achievement-id\",\"title\":\"Example Achievement\",\"difficulty\":\"elite\",\"verifiedAt\":\"2026-06-24T12:00:00.000Z\"}]}"
			+ "},"
			+ "\"freshness\":{"
			+ "\"trustLabel\":\"trusted_cached\","
			+ "\"wom\":{\"source\":\"wise_old_man\",\"updatedAt\":\"2026-06-24T12:00:00.000Z\",\"label\":\"Source Freshness\",\"ageSeconds\":86400},"
			+ "\"runebound\":{\"cachedAt\":\"2026-06-24T12:02:00.000Z\",\"label\":\"RuneBound Freshness\",\"ageSeconds\":120}"
			+ "},"
			+ "\"client\":{\"recommendedTtlSeconds\":900,\"minimumPollIntervalSeconds\":1800,\"mayOpenProfileUrl\":true,\"mayRequestRefreshFromThisEndpoint\":false}"
			+ "}";
	}

	private static String summaryJsonWithRecentAchievements(int count)
	{
		final StringBuilder items = new StringBuilder();
		for (int index = 1; index <= count; index++)
		{
			if (items.length() > 0)
			{
				items.append(',');
			}
			items.append("{\"id\":\"achievement-")
				.append(index)
				.append("\",\"title\":\"Achievement ")
				.append(index)
				.append("\",\"difficulty\":\"elite\",\"verifiedAt\":\"2026-06-24T12:00:00.000Z\"}");
		}

		return okSummaryJson().replace(
			"{\"id\":\"achievement-id\",\"title\":\"Example Achievement\",\"difficulty\":\"elite\",\"verifiedAt\":\"2026-06-24T12:00:00.000Z\"}",
			items.toString()
		);
	}

	private static String notCachedJson()
	{
		return "{"
			+ "\"schemaVersion\":\"runebound.runelite.profile.summary.v1\","
			+ "\"source\":\"public_profile_cache\","
			+ "\"safeToDisplay\":false,"
			+ "\"status\":{\"code\":\"not_cached\",\"label\":\"RuneBound does not have a cached profile summary for this player yet.\",\"reason\":\"missing_public_profile_response_cache\"},"
			+ "\"displayName\":\"RuneBounder\","
			+ "\"normalizedUsername\":\"runebounder\","
			+ "\"profileUrl\":\"https://rune-bound.net/player/RuneBounder\","
			+ "\"player\":{\"username\":\"RuneBounder\",\"displayName\":\"RuneBounder\",\"normalizedUsername\":\"runebounder\",\"profileUrl\":\"https://rune-bound.net/player/RuneBounder\",\"account\":{\"selectedAccountType\":null,\"womType\":null,\"womBuild\":null,\"combatLevel\":null}},"
			+ "\"summary\":null,"
			+ "\"freshness\":{\"trustLabel\":\"not_cached\"},"
			+ "\"client\":{\"recommendedTtlSeconds\":900,\"minimumPollIntervalSeconds\":3600,\"mayOpenProfileUrl\":true,\"mayRequestRefreshFromThisEndpoint\":false}"
			+ "}";
	}

	private static String invalidUsernameJson()
	{
		return "{"
			+ "\"schemaVersion\":\"runebound.runelite.profile.summary.v1\","
			+ "\"source\":\"none\","
			+ "\"safeToDisplay\":false,"
			+ "\"status\":{\"code\":\"invalid_username\",\"label\":\"OSRS username is required.\",\"reason\":\"blank_username\"},"
			+ "\"displayName\":null,"
			+ "\"normalizedUsername\":null,"
			+ "\"profileUrl\":null,"
			+ "\"player\":null,"
			+ "\"summary\":null,"
			+ "\"freshness\":{\"trustLabel\":\"unavailable\"},"
			+ "\"client\":{\"recommendedTtlSeconds\":300,\"minimumPollIntervalSeconds\":1800,\"mayOpenProfileUrl\":false,\"mayRequestRefreshFromThisEndpoint\":false}"
			+ "}";
	}
}
