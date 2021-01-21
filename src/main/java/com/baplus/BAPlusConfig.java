/*
 * Copyright (c) 2020, Sean 'Furret' Hill <https://github.com/hisean1>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
		name = "Show game point breakdown",
		description = "Provides a detailed point breakdown for each role after a round",
		position = 3
	)
	default boolean pointBreakdown()
	{
		return false;
	}

	@ConfigItem(
		keyName = "wavePointBreakdown",
		name = "Individual wave point breakdown",
		description = "Provides a point breakdown after every wave",
		position = 4
	)
	default boolean wavePointBreakdown()
	{
		return false;
	}

	@ConfigItem(
		keyName = "disableCallFlash",
		name = "Disable Call Flashing",
		description = "Stops the flashing effect on call changes",
		position = 5
	)
	default boolean disableCallFlashing()
	{
		return true;
	}

	@ConfigItem(
		keyName = "legacyEggModels",
		name = "Legacy Egg Models",
		description = "Replaces eggs with their old solid-color counterparts ('Leggacy' mode)",
		position = 6
	)
	default boolean legacyEggModels()
	{
		return false;
	}

	@ConfigSection(
		name = "Split Comparisons",
		description = "Choose which splits to compare against",
		position = 7
	)
	String splitComparison = "splitComparison";
	@ConfigItem(
		keyName = "category",
		name = "Run Category",
		description = "Run category to compare against",
		position = 8,
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
		position = 9,
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
		position = 10,
		section = splitComparison
	)
	default Keybind saveCustomAsPB()
	{
		return new Keybind(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "waveEndTimes",
		name = "Wave End Times",
		description = "Enter your desired wave end times (time for each specific wave)",
		position = 11,
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
		position = 12,
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
