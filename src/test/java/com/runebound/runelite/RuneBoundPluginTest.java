package com.runebound.runelite;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RuneBoundPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RuneBoundPlugin.class);
		RuneLite.main(args);
	}
}
