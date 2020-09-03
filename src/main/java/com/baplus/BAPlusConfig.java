package com.baplus;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;

@ConfigGroup("example")
public interface BAPlusConfig extends Config
{
	@ConfigItem(
		keyName = "waveTimes",
		name = "Show wave and game duration",
		description = "Displays wave and game duration",
		position = 0
	)
	default boolean waveTimes()
	{
		return true;
	}

	@ConfigItem(
		keyName = "waveCompare",
		name = "Compare wave times against goal",
		description = "Displays goal wave end times",
		position = 1
	)
	default boolean waveCompare()
	{
		return false;
	}

	@ConfigItem(
		keyName = "waveSplits",
		name = "Show wave end splits",
		description = "Makes you better",
		position = 2
	)
	default boolean waveSplits()
	{
		return false;
	}

	@ConfigItem(
		keyName = "pointBreakdown",
		name = "Show point breakdown",
		description = "Provides a detailed point breakdown for each role after every wave",
		position = 3
	)
	default boolean pointBreakdown()
	{
		return false;
	}

	@ConfigSection(
		name = "Split Comparisons",
		description = "Choose which splits to compare against",
		position = 4
	)
	String splitComparison = "splitComparison";
	@ConfigItem(
		keyName = "category",
		name = "Run Category",
		description = "Run category to compare against",
		position = 5,
		section = splitComparison
	)
	default RunCategory category()
	{
		return RunCategory.SOLOHEALWR;
	}

	@ConfigItem(
		keyName = "pbSaveKey",
		name = "Save last run as PB",
		description = "Choose the PB run category to save to, then hit this hotkey",
		position = 6,
		section = splitComparison
	)
	default Keybind saveLastRunAsPB()
	{
		return new Keybind(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "customPBSaveKey",
		name = "Save custom time as PB",
		description = "Choose the PB run category to save to, enter a custom time\n" +
			"in the Wave Splits box, then hit this hotkey",
		position = 7,
		section = splitComparison
	)
	default Keybind saveCustomAsPB()
	{
		return new Keybind(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "waveEndTimes",
		name = "Wave End Times",
		description = "Enter your desired wave end times (time for this specific wave)",
		position = 8,
		section = splitComparison
	)
	default String getDesiredWaveTimes()
	{
		return "";
	}

	@ConfigItem(
		keyName = "waveEndTimes",
		name = "",
		description = "",
		section = splitComparison
	)
	void setDesiredWaveTimes(String key);

	@ConfigItem(
		keyName = "waveEndSplits",
		name = "Wave Splits",
		description = "Enter your desired wave splits (total time from start to wave finish)",
		position = 9,
		section = splitComparison
	)
	default String getDesiredWaveSplits()
	{
		return "";
	}

	@ConfigItem(
		keyName = "waveSplits",
		name = "",
		description = "",
		section = splitComparison
	)
	void setDesiredWaveSplits(String key);
}
