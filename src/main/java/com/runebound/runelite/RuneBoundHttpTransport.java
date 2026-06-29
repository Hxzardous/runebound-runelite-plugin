package com.runebound.runelite;

import java.util.function.Consumer;

interface RuneBoundHttpTransport
{
	void getJson(String url, Consumer<RuneBoundSummaryResult> callback);
}
