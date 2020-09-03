package com.baplus;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.Color;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
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

@Slf4j
@PluginDescriptor(
	name = "BA Plus"
)
public class BAPlusPlugin extends Plugin
{
	private static final int BA_WAVE_NUM_INDEX = 2;
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
	}

	@Override
	protected void shutDown() throws Exception
	{
		gameTime = null;
		currentWave = START_WAVE;
		inGameBit = 0;
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		switch (event.getGroupId())
		{
			case WidgetID.BA_REWARD_GROUP_ID:
			{
				Widget rewardWidget = client.getWidget(WidgetInfo.BA_REWARD_TEXT);
				Widget pointsWidget = client.getWidget(WidgetID.BA_REWARD_GROUP_ID, 14); //RUNNERS_PASSED

				// Wave 10 ended
				if (config.waveTimes() && rewardWidget != null && rewardWidget.getText().contains(ENDGAME_REWARD_NEEDLE_TEXT) && gameTime != null)
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

					if (config.pointBreakdown())
					{
						chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message.build()).build());
					}
				}

				break;
			}
			case WidgetID.BA_ATTACKER_GROUP_ID:
			{
				setRound(Role.ATTACKER);
				break;
			}
			case WidgetID.BA_DEFENDER_GROUP_ID:
			{
				setRound(Role.DEFENDER);
				break;
			}
			case WidgetID.BA_HEALER_GROUP_ID:
			{
				setRound(Role.HEALER);
				break;
			}
			case WidgetID.BA_COLLECTOR_GROUP_ID:
			{
				setRound(Role.COLLECTOR);
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
