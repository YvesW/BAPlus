/*
 * Copyright (c) 2018, Cameron <https://github.com/noremac201>
 * Copyright (c) 2018, Jacob M <https://github.com/jacoblairm>
 * Copyright (c) 2020, Sean 'Furret' Hill <https://github.com/hisean1>
 * Huge shoutout to Gamma1991#0001 who carried me through the egg recoloring
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

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.Color;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "BA Plus",
	description = "Adds speedrunning and general qol improvements to the base BA plugin",
	tags = {"ba", "barb assault", "barbarian assault", "speedrunning"}
)
public class BAPlusPlugin extends Plugin
{
	private final int BA_GREEN_EGG_ID = 10531;
	private final int BA_RED_EGG_ID = 10532;
	private final int BA_BLUE_EGG_ID = 10533;
	private final int BA_YELLOW_EGG_ID = 10534;
	private final int BA_POISONED_EGG_ID = 10535;
	private final int BA_SPIKED_EGG_ID = 10536;
	private final int BA_OMEGA_EGG_ID = 10537;

	private final int WHITE_TO_RED = 896;		// 000000 111 0000000 | Red, Full saturation, No brightness change
	private final int WHITE_TO_GREEN = 22400;	// 010101 111 0000000 | Green, Full saturation, No brightness change
	private final int WHITE_TO_BLUE = 43392;	// 101010 011 0000000 | Blue, Half saturation, No brightness change
	private final int RED_TO_YELLOW = -56320;
	private final int YELLOW_RED_DIVIDE = 60466;
	private final int SATURATION_LIMIT = 128;	// 000000 001 0000000 | Under this is pure brightness, no color (grey)
	private final int RED_BRIGHTNESS_LIMIT = 940;
	private final int RED_ADJUST = 920;
	private final int RED_SHADOW_LIMIT = 918;
	private final int RED_SHADOW_ADJUST = 917;
	private final int GREEN_BRIGHTNESS_LIMIT = 22450;
	private final int GREEN_ADJUST = 22418;
	private final int GREEN_SHADOW_LIMIT = 22415;
	private final int GREEN_SHADOW_ADJUST = 22408;
	private final int BLUE_BRIGHTNESS_LIMIT = 43426;
	private final int BLUE_ADJUST = 43426;
	private final int BLUE_SHADOW_LIMIT = 43420;
	private final int BLUE_SHADOW_ADJUST = 43418;

	private static final int BA_WAVE_NUM_INDEX = 2;
	private static final int BA_MONSTER_DEATH_INDEX = 5;
	private static final int BA_WAVE_COUNT = 10;
	private static final String START_WAVE = "1";
	private static final String ENDGAME_REWARD_NEEDLE_TEXT = "<br>5";
	private static final String CONFIG_GROUP = "barbarianAssault";

	final int[] childIDsOfPointsWidgets = new int[]{33, 32, 25, 26, 24, 28, 31, 27, 29, 30, 21, 22, 19};

	private int pointsHealer, pointsDefender , pointsCollector, pointsAttacker, totalEggsCollected, totalIncorrectAttacks, totalHealthReplenished;

	private static final Gson GSON = new Gson();

	private int inGameBit = 0;
	private String currentWave = START_WAVE;
	private GameTimer gameTime;

	private String[] waveGoal = {"0:30", "0:37", "0:43", "0:42", "0:48", "1:01", "1:09", "1:12", "1:21", "1:48"};
	private String[] lastRun = {"", "", "", "", "", "", "", "", "", ""};

	@Getter
	private Round currentRound;

	@Inject
	private Client client;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BAPlusConfig config;

	@Provides
	BAPlusConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BAPlusConfig.class);
	}

	private final HotkeyListener previousKeyListener = new HotkeyListener(() -> config.saveLastRunAsPB())
	{
		@Override
		public void hotkeyPressed()
		{
			if (config.category() == RunCategory.SOLOHEALPB ||
				config.category() == RunCategory.DUOHEALPB ||
				config.category() == RunCategory.LEECHPB)
			{
				saveTime(config.category(), lastRun);
				announceMessage("Personal best saved for run type: " + config.category().toString() + ".");
			}
			else
			{
				announceMessage("Unable to save personal best - Invalid run type: " + config.category().toString() + ", save as a PB.");
			}
		}
	};

	private final HotkeyListener nextKeyListener = new HotkeyListener(() -> config.saveCustomAsPB())
	{
		@Override
		public void hotkeyPressed()
		{
			if (config.category() == RunCategory.SOLOHEALPB ||
				config.category() == RunCategory.DUOHEALPB ||
				config.category() == RunCategory.LEECHPB)
			{
				saveTime(config.category(), parseWaveTimesFromString(config.getDesiredWaveSplits()));
				announceMessage("Personal best saved for run type: " + config.category().toString() + ".");
			}
			else if (config.category() == RunCategory.CUSTOM)
			{
				waveGoal = parseWaveTimesFromString(config.getDesiredWaveTimes());
				announceMessage("Saved new goal times: " + waveGoal[0] + ", " + waveGoal[1] + ", " + waveGoal[2] + ", " + waveGoal[3]
					+ ", " + waveGoal[4] + ", " + waveGoal[5] + ", " + waveGoal[6] + ", " + waveGoal[7] + ", " + waveGoal[8] + ", " + waveGoal[9]);
			}
			else
			{
				announceMessage("Unable to save personal best - Invalid run type: " + config.category().toString() + ", save as a PB.");
			}
		}
	};

	@Override
	protected void startUp() throws Exception
	{
		keyManager.registerKeyListener(previousKeyListener);
		keyManager.registerKeyListener(nextKeyListener);

		// Save splits for current WRs and custom times
		saveTime(RunCategory.SOLOHEALWR, new String[] {"0:30", "1:10", "1:55", "2:41", "3:36", "4:39", "5:50", "7:06", "8:30", "10:18"});
		saveTime(RunCategory.DUOHEALWR, new String[] {"0:30", "1:11", "1:57", "2:41", "3:33", "4:34", "5:38", "6:48", "8:11", "9:57"});
		saveTime(RunCategory.LEECHWR, new String[] {"0:32", "1:14", "2:04", "2:55", "3:52", "4:57", "6:10", "7:27", "8:56", "10:50"});
		saveTime(RunCategory.CUSTOM, parseWaveTimesFromString(config.getDesiredWaveSplits()));

		// Read in the custom wave end times
		if (config.category() == RunCategory.CUSTOM)
		{
			// Only overwrite if string looks valid
			if (parseWaveTimesFromString(config.getDesiredWaveTimes()).length == 10)
			{
				waveGoal = parseWaveTimesFromString((config.getDesiredWaveTimes()));
			}
		}

		log.debug("Startup");
	}

	@Override
	protected void shutDown() throws Exception
	{
		gameTime = null;
		currentWave = START_WAVE;
		inGameBit = 0;
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned itemSpawned)
	{
		if (config.legacyEggModels())
		{
			TileItem item = itemSpawned.getItem();
			int itemID = item.getId();
			/*if (item.getId() == BA_GREEN_EGG_ID)
			{
				recolorAllFaces(item.getModel(), BA_GREEN_EGG_ID);
			}
			else if (item.getId() == BA_RED_EGG_ID)
			{
				recolorAllFaces(item.getModel(), BA_RED_EGG_ID);
			}
			else if (item.getId() == BA_BLUE_EGG_ID)
			{
				recolorAllFaces(item.getModel(), BA_BLUE_EGG_ID);
			}
			else if (item.getId() == BA_YELLOW_EGG_ID)
			{
				recolorAllFaces(item.getModel(), BA_YELLOW_EGG_ID);
			}
			else if (item.getId() == BA_POISONED_EGG_ID)
			{
				recolorAllFaces(item.getModel(), BA_YELLOW_EGG_ID);
			}
			else if (item.getId() == BA_SPIKED_EGG_ID)
			{
				recolorAllFaces(item.getModel(), BA_YELLOW_EGG_ID);
			}*/
			if (itemID >= BA_GREEN_EGG_ID && itemID <= BA_OMEGA_EGG_ID)
			{
				recolorEggFaces(item.getModel(), itemID);
			}
		}
	}

	private void recolorEggFaces(Model model, int itemID)
	{
		System.out.println("recolorEggFaces: " + itemID);
		switch(itemID)
		{
			case BA_GREEN_EGG_ID:
				replaceFaceColorValues(model, SATURATION_LIMIT, WHITE_TO_GREEN, GREEN_BRIGHTNESS_LIMIT, GREEN_ADJUST, GREEN_SHADOW_LIMIT, GREEN_SHADOW_ADJUST);
				break;
			case BA_RED_EGG_ID:
				replaceFaceColorValues(model, SATURATION_LIMIT, WHITE_TO_RED, RED_BRIGHTNESS_LIMIT, RED_ADJUST, RED_SHADOW_LIMIT, RED_SHADOW_ADJUST);
				break;
			case BA_BLUE_EGG_ID:
				replaceFaceColorValues(model, SATURATION_LIMIT, WHITE_TO_BLUE, BLUE_BRIGHTNESS_LIMIT, BLUE_ADJUST, BLUE_SHADOW_LIMIT, BLUE_SHADOW_ADJUST);
				break;
			case BA_YELLOW_EGG_ID:
				replaceFaceColorValues(model, 0, RED_TO_YELLOW, YELLOW_RED_DIVIDE, 0, 0, 0);
				break;
			default:
				break;
		}
	}

	private void replaceFaceColorValues(Model model, int saturationLimit, int correctionValue, int brightnessLimit, int adjust, int shadowLimit, int shadowAdjust)
	{
		int[] faceColors1 = model.getFaceColors1();
		int[] faceColors2 = model.getFaceColors2();
		int[] faceColors3 = model.getFaceColors3();

		if (faceColors3.length != faceColors1.length || faceColors3.length != faceColors2.length || faceColors1.length != faceColors2.length)
		{
			System.out.println("Model face array length mismatch");
			return;
		}

		try
		{
			// All face colors arrays should be the same length
			for (int i = 0; i < faceColors1.length; i++)
			{
				// Backside logo of eggs are 0<x<128
				if (faceColors1[i] < saturationLimit)
				{
					faceColors1[i] += correctionValue;
					// Fix for western bright patches
					if (faceColors1[i] > shadowLimit)
					{
						/*if (i < faceColors1.length / 2)
						{
							faceColors1[i] = 0;
						}*/
					}
				}
				// Front side logo of eggs are over brightness limit, ignore yellow eggs
				else if (faceColors1[i] > brightnessLimit && saturationLimit != 0)
				{
					faceColors1[i] = adjust;
				}

				// Backside logo of eggs are 0<x<128
				if (faceColors2[i] < saturationLimit)
				{
					faceColors2[i] += correctionValue;
					// Fix for western bright patches
					if (faceColors2[i] > shadowLimit)
					{
						/*if (i < faceColors2.length / 2)
						{
							faceColors2[i] = 0;
						}*/
					}
				}
				// Front side logo of eggs are over brightness limit, ignore yellow eggs
				else if (faceColors2[i] > brightnessLimit && saturationLimit != 0)
				{
					faceColors2[i] = adjust;
				}

				// Backside logo of eggs are 0<x<128
				if (faceColors3[i] < saturationLimit)
				{
					faceColors3[i] += correctionValue;
					// Fix for western bright patches
					if (faceColors3[i] > shadowLimit)
					{
						/*if (i < faceColors3.length / 2)
						{
							faceColors3[i] = 0;
						}*/
					}
				}
				// Front side logo of eggs are over brightness limit, ignore yellow eggs
				else if (faceColors3[i] > brightnessLimit && saturationLimit != 0)
				{
					faceColors3[i] = adjust;
				}

				if (faceColors1[i] >= YELLOW_RED_DIVIDE)
				{
					faceColors1[i] = faceColors1[i] + RED_TO_YELLOW;
				}
				if (faceColors2[i] >= YELLOW_RED_DIVIDE)
				{
					faceColors2[i] = faceColors2[i] + RED_TO_YELLOW;
				}
				if (faceColors3[i] >= YELLOW_RED_DIVIDE)
				{
					faceColors3[i] = faceColors3[i] + RED_TO_YELLOW;
				}
			}
		}
		catch (Exception ex)
		{
			System.out.println("--- BAPlusPlugin.replaceColorFaceValues() ---\n" + ex.getClass().toString() + " exception: " + ex.getMessage());
			log.debug("--- BAPlusPlugin.replaceColorFaceValues() ---\n" + ex.getClass().toString() + " exception: " + ex.getMessage());
		}
		System.out.println("Done recoloring");
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		switch (event.getGroupId())
		{
			// Reward Screen Widget
			case WidgetID.BA_REWARD_GROUP_ID:
			{
				Widget rewardWidget = client.getWidget(WidgetInfo.BA_REWARD_TEXT);
				Widget pointsWidget = client.getWidget(WidgetID.BA_REWARD_GROUP_ID, 14); //RUNNERS_PASSED

				// Wave 10 ended
				if (rewardWidget != null && rewardWidget.getText().contains(ENDGAME_REWARD_NEEDLE_TEXT) && gameTime != null)
				{
					gameTime = null;
					ChatMessageBuilder message = new ChatMessageBuilder()
						.append("Attacker: ")
						.append(Color.red, pointsAttacker + 80 + "")
						.append(" |  Healer: ")
						.append(Color.GREEN, pointsHealer + 80 + "")
						.append(" | Defender: ")
						.append(Color.blue, pointsDefender + 80 + "")
						.append(" | Collector: ")
						.append(Color.yellow, pointsCollector + 80 + "")
						.append(System.getProperty("line.separator"))
						.append(totalEggsCollected + " eggs collected, " + totalHealthReplenished + "HP vialed and " + totalIncorrectAttacks + " wrong attacks.");

					if (config.pointBreakdown())
					{
						chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message.build()).build());
					}
				}
				// Wave 1-9 ended
				else if (pointsWidget != null && client.getVar(Varbits.IN_GAME_BA) == 0)
				{
					int wavePoints_Attacker, wavePoints_Defender, wavePoints_Healer, wavePoints_Collector, waveEggsCollected, waveHPReplenished, waveFailedAttacks;

					wavePoints_Attacker = wavePoints_Defender = wavePoints_Healer = wavePoints_Collector = Integer.parseInt(client.getWidget(WidgetID.BA_REWARD_GROUP_ID, childIDsOfPointsWidgets[0]).getText()); //set base pts to all roles
					waveEggsCollected = waveHPReplenished = waveFailedAttacks = 0;

					// Gather post-wave info from points widget
					for (int i = 0; i < childIDsOfPointsWidgets.length; i++)
					{
						int value = Integer.parseInt(client.getWidget(WidgetID.BA_REWARD_GROUP_ID, childIDsOfPointsWidgets[i]).getText());

						switch (i)
						{
							case 1:
							case 2:
							case 3:
								wavePoints_Attacker += value;
								break;
							case 4:
							case 5:
								wavePoints_Defender += value;
								break;
							case 6:
								wavePoints_Collector += value;
								break;
							case 7:
							case 8:
							case 9:
								wavePoints_Healer += value;
								break;
							case 10:
								waveEggsCollected = value;
								totalEggsCollected += value;

								break;
							case 11:
								waveFailedAttacks = value;
								totalIncorrectAttacks += value;
								break;
							case 12:
								waveHPReplenished = value;
								totalHealthReplenished += value;
								break;
						}
					}

					pointsCollector += wavePoints_Collector;
					pointsHealer += wavePoints_Healer;
					pointsDefender += wavePoints_Defender;
					pointsAttacker += wavePoints_Attacker;

					ChatMessageBuilder message = new ChatMessageBuilder()
						.append("Attacker: ")
						.append(Color.red, wavePoints_Attacker + "")
						.append(" |  Healer: ")
						.append(Color.GREEN, wavePoints_Healer + "")
						.append(" | Defender: ")
						.append(Color.blue, wavePoints_Defender + "")
						.append(" | Collector: ")
						.append(Color.yellow, wavePoints_Collector + "")
						.append(System.getProperty("line.separator"))
						.append(waveEggsCollected + " eggs collected, " + waveHPReplenished + "HP vialed and " + waveFailedAttacks + " wrong attacks.");

					if (config.wavePointBreakdown())
					{
						chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message.build()).build());
					}
				}

				break;
			}
			// Wave Starting (Attacker)
			case WidgetID.BA_ATTACKER_GROUP_ID:
			{
				setRound(Role.ATTACKER);
				disableCallFlash(Role.ATTACKER);
				break;
			}
			// Wave Starting (Defender)
			case WidgetID.BA_DEFENDER_GROUP_ID:
			{
				setRound(Role.DEFENDER);
				disableCallFlash(Role.DEFENDER);
				break;
			}
			// Wave Starting (Healer)
			case WidgetID.BA_HEALER_GROUP_ID:
			{
				setRound(Role.HEALER);
				disableCallFlash(Role.HEALER);
				break;
			}
			// Wave Starting (Collector)
			case WidgetID.BA_COLLECTOR_GROUP_ID:
			{
				setRound(Role.COLLECTOR);
				disableCallFlash(Role.COLLECTOR);
				break;
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() == ChatMessageType.GAMEMESSAGE
			&& event.getMessage().startsWith("---- Wave:"))
		{
			String[] message = event.getMessage().split(" ");
			currentWave = message[BA_WAVE_NUM_INDEX];

			if (currentWave.equals(START_WAVE))
			{
				gameTime = new GameTimer();
				pointsHealer = pointsDefender = pointsCollector = pointsAttacker = totalEggsCollected = totalIncorrectAttacks = totalHealthReplenished = 0;
			}
			else if (gameTime != null)
			{
				gameTime.setWaveStartTime();
			}
		}
		else if (event.getMessage().startsWith("All of the Penance") && gameTime != null && inGameBit != 0 && config.monsterTimer())
		{
			final MessageNode node = event.getMessageNode();
			final String nodeValue = Text.removeTags(node.getValue());
			node.setValue(nodeValue + " (<col=ff8000>" + timeToSeconds(gameTime.getTime(true)) + "s<col=ffffff>)");
			chatMessageManager.update(node);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int inGame = client.getVar(Varbits.IN_GAME_BA);

		if (inGameBit != inGame)
		{
			if (inGameBit == 1)
			{
				currentRound = null;

				// Use an instance check to determine if this is exiting a game or a tutorial
				// After exiting tutorials there is a small delay before changing IN_GAME_BA back to
				// 0 whereas when in a real wave it changes while still in the instance.
				if (config.waveTimes() && gameTime != null && client.isInInstancedRegion())
				{
					int curWave = 0;
					try
					{
						curWave = Integer.parseInt(currentWave);
					}
					catch (NumberFormatException nfex)
					{
						return;
					}

					// Display relevant post-wave info based on plugin settings
					waveEnd(curWave, gameTime.getTime(false));
				}
			}
		}

		inGameBit = inGame;
	}

	private void setRound(Role role)
	{
		// Prevent changing rounds when a round is already set, as widgets can be
		// loaded multiple times in game from eg. opening and closing the horn
		// of glory.
		if (currentRound == null)
		{
			currentRound = new Round(role);
		}
	}

	private void waveEnd(int waveNum, String time)
	{
		if (waveNum < 1 || waveNum > BA_WAVE_COUNT)
		{
			announceMessage("waveNum: " + waveNum);
			return;
		}

		// Reset lastRun on a new round
		if (waveNum == 1)
		{
			for (int i = 0; i < lastRun.length; i++)
			{
				lastRun[i] = "";
			}
		}

		lastRun[waveNum - 1] = time;

		// Display wave durations
		if (config.waveTimes())
		{
			announceTime(waveNum, gameTime.getTime(true));
		}

		// Display wave splits against desired category
		if (config.waveSplits())
		{
			announceTime(waveNum, config.category(), time);
		}
	}

	private void disableCallFlash(Role role)
	{
		if (config.disableCallFlashing())
		{
			switch (role)
			{
				case ATTACKER:
					final Widget atkFlashWidget = client.getWidget(485, 4);
					atkFlashWidget.setOpacity(255);
					break;
				case DEFENDER:
					final Widget defFlashWidget = client.getWidget(487, 4);
					defFlashWidget.setOpacity(255);
					break;
				case COLLECTOR:
					final Widget colFlashWidget = client.getWidget(486, 4);
					colFlashWidget.setOpacity(255);
					break;
				case HEALER:
					final Widget healFlashWidget = client.getWidget(488, 4);
					healFlashWidget.setOpacity(255);
					break;
				default:
					break;
			}
		}
	}

	private void announceTime(int waveNum, String time)
	{
		// Error if waveNum is invalid
		if (waveNum < 1 || waveNum > BA_WAVE_COUNT)
		{
			final String errormsg = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append("Invalid waveNum: " + waveNum)
				.build();

			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(errormsg)
				.build());
			return;
		}

		String timeMessage = "";

		// Write a wave time message if the option is checked
		if (config.waveTimes())
		{
			// Include a goal comparison if the option is checked
			if (config.waveCompare())
			{
				timeMessage = new ChatMessageBuilder()
					.append(ChatColorType.NORMAL)
					.append("Wave " + waveNum + " goal: ")
					.append(Color.BLUE, waveGoal[waveNum - 1])
					.append(ChatColorType.NORMAL)
					.append(" || Duration: ")
					.append(compareSplitColor(time, waveGoal[waveNum - 1]), time)
					.build();
			}
			else
			{
				timeMessage = new ChatMessageBuilder()
					.append(ChatColorType.NORMAL)
					.append("Wave " + waveNum + " duration: ")
					.append(ChatColorType.HIGHLIGHT)
					.append(time)
					.build();
			}

			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(timeMessage)
				.build());

			// Display game duration if not already displaying splits
			if (waveNum == BA_WAVE_COUNT && !config.waveSplits())
			{
				String endMessage = new ChatMessageBuilder()
					.append(ChatColorType.NORMAL)
					.append("Game finished, duration: ")
					.append(ChatColorType.HIGHLIGHT)
					.append(gameTime.getTime(false))
					.build();

				chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(endMessage)
					.build());
			}
		}
	}

	private void announceTime(int waveNum, RunCategory category, String time)
	{
		String[] compareTimes = getTimes(category);
		if (waveNum < 1 || waveNum > compareTimes.length)
		{
			return;
		}

		final String timeMessage = new ChatMessageBuilder()
			.append(ChatColorType.NORMAL)
			.append(category.toString() + " - Wave " + waveNum + " pace: ")
			.append(Color.BLUE, compareTimes[waveNum - 1])
			.append(ChatColorType.NORMAL)
			.append(" || Split: ")
			.append(compareSplitColor(time, compareTimes[waveNum - 1]), time + " (" + (timeToSeconds(time) - timeToSeconds(compareTimes[waveNum - 1]) ) + "s)")
			.build();

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(timeMessage)
			.build());
	}

	private void announceMessage(String msg)
	{
		final String chatmsg = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append(msg)
			.build();

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(chatmsg)
			.build());
	}

	private Color compareSplitColor(String split, String pace)
	{
		if (timeToSeconds(split) < timeToSeconds(pace))
		{
			return Color.GREEN;
		}
		else if (timeToSeconds(split) > timeToSeconds(pace))
		{
			return Color.RED;
		}
		else
		{
			return Color.BLUE;
		}
	}

	private int timeToSeconds(String time)
	{
		String times[] = time.split(":");
		try
		{
			if (times.length == 1)
			{
				return Integer.parseInt(time);
			}
			else if (times.length == 2)
			{
				return (Integer.parseInt(times[0]) * 60) + Integer.parseInt(times[1]);
			}
			else
			{
				return 0;
			}
		}
		catch (NumberFormatException nfex)
		{
			return -1;
		}
	}

	private String[] parseWaveTimesFromString(String timeString)
	{
		String[] timeArray = timeString.split("[,\\s\n]");

		if (timeArray.length != 10)
		{
			// bad
			return new String[0];
		}
		return timeArray;
	}

	private void saveTime(RunCategory category, String[] times)
	{
		if (times == null || times.length == 0)
		{
			configManager.unsetConfiguration(CONFIG_GROUP, category.toString());
			return;
		}

		String json = GSON.toJson(times);
		configManager.setConfiguration(CONFIG_GROUP, category.toString(), json);
	}

	private String[] getTimes(RunCategory category)
	{
		String json = configManager.getConfiguration(CONFIG_GROUP, category.toString());
		if (Strings.isNullOrEmpty(json))
		{
			announceMessage("Error getting split time, no time found for category: " + category.toString());
			return waveGoal;
		}

		// CHECKSTYLE:OFF
		return GSON.fromJson(json, new TypeToken<String[]>(){}.getType());
		// CHECKSTYLE:ON
	}
}
