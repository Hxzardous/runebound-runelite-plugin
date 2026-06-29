package com.runebound.runelite;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

final class RuneBoundOkHttpTransport implements RuneBoundHttpTransport
{
	private static final long MAX_RESPONSE_BYTES = 128L * 1024L;
	private static final String USER_AGENT = "RuneBound RuneLite Plugin/0.1.0";

	private final OkHttpClient httpClient;
	private final Gson gson;

	RuneBoundOkHttpTransport(OkHttpClient httpClient, Gson gson)
	{
		if (httpClient == null)
		{
			throw new IllegalArgumentException("httpClient is required");
		}

		if (gson == null)
		{
			throw new IllegalArgumentException("gson is required");
		}

		this.httpClient = httpClient.newBuilder()
			.connectTimeout(10, TimeUnit.SECONDS)
			.readTimeout(10, TimeUnit.SECONDS)
			.writeTimeout(10, TimeUnit.SECONDS)
			.build();
		this.gson = gson;
	}

	@Override
	public void getJson(String url, Consumer<RuneBoundSummaryResult> callback)
	{
		final Request request = new Request.Builder()
			.url(url)
			.header("Accept", "application/json")
			.header("User-Agent", USER_AGENT)
			.get()
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException exception)
			{
				callback.accept(RuneBoundSummaryResult.failedBeforeNetwork());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (Response ignored = response)
				{
					final ResponseBody body = response.body();
					final String bodyText = readBoundedBody(body);
					if (bodyText == null)
					{
						callback.accept(RuneBoundSummaryResult.responseTooLarge(response.code()));
						return;
					}

					callback.accept(RuneBoundSummaryResult.fromHttpStatus(response.code(), bodyText, gson));
				}
			}
		});
	}

	private static String readBoundedBody(ResponseBody body) throws IOException
	{
		if (body == null)
		{
			return "";
		}

		if (body.contentLength() > MAX_RESPONSE_BYTES)
		{
			return null;
		}

		final BufferedSource source = body.source();
		source.request(MAX_RESPONSE_BYTES + 1L);
		if (source.getBuffer().size() > MAX_RESPONSE_BYTES)
		{
			return null;
		}

		return source.getBuffer().clone().readString(StandardCharsets.UTF_8);
	}
}
