package com.baplus;

import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import static net.runelite.client.util.RSTimeUnit.GAME_TICKS;

class Round
{
	private final Instant roundStartTime;
	@Getter
	private final Role roundRole;
	@Getter
	@Setter
	private boolean runnersKilled;
	@Getter
	@Setter
	private boolean rangersKilled;
	@Getter
	@Setter
	private boolean healersKilled;
	@Getter
	@Setter
	private boolean fightersKilled;

	@Inject
	public Round(Role role)
	{
		this.roundRole = role;
		this.roundStartTime = Instant.now().plus(Duration.of(2, GAME_TICKS));
	}

	public long getRoundTime()
	{
		return Duration.between(roundStartTime, Instant.now()).getSeconds();
	}

	public long getTimeToChange()
	{
		return 30 + (Duration.between(Instant.now(), roundStartTime).getSeconds() % 30);
	}
}