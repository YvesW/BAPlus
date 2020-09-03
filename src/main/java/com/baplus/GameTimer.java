package com.baplus;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import static net.runelite.client.util.RSTimeUnit.GAME_TICKS;

class GameTimer
{
	final private Instant startTime = Instant.now();
	private Instant prevWave = startTime;

	String getTime(boolean waveTime)
	{
		final Instant now = Instant.now();
		final Duration elapsed;

		if (waveTime)
		{
			elapsed = Duration.between(prevWave, now);
		}
		else
		{
			elapsed = Duration.between(startTime, now).minus(Duration.of(1, GAME_TICKS));
		}

		return formatTime(LocalTime.ofSecondOfDay(elapsed.getSeconds()));
	}

	void setWaveStartTime()
	{
		prevWave = Instant.now();
	}

	private static String formatTime(LocalTime time)
	{
		if (time.getHour() > 0)
		{
			return time.format(DateTimeFormatter.ofPattern("HH:mm"));
		}
		else if (time.getMinute() > 9)
		{
			return time.format(DateTimeFormatter.ofPattern("mm:ss"));
		}
		else
		{
			return time.format(DateTimeFormatter.ofPattern("m:ss"));
		}
	}
}