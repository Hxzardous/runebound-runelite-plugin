package com.runebound.runelite;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(RuneBoundConfig.CONFIG_GROUP)
public interface RuneBoundConfig extends Config
{
	String CONFIG_GROUP = "runebound";
	String THIRD_PARTY_WARNING = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers";

	@ConfigItem(
		keyName = "enableNetworkLookups",
		name = "Enable RuneBound summary lookups",
		description = "Default off. Allows explicit read-only summary lookups from rune-bound.net for a selected public display name. The plugin sends no gameplay data and does not trigger RuneBound refreshes.",
		position = 0,
		warning = THIRD_PARTY_WARNING
	)
	default boolean enableNetworkLookups()
	{
		return false;
	}
}
