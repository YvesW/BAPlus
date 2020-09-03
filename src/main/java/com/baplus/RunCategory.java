package com.baplus;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum RunCategory
{
	SOLOHEALWR("Solo Heal WR"),
	DUOHEALWR("Duo Heal WR"),
	LEECHWR("Leech WR"),
	SOLOHEALPB("Solo Heal PB"),
	DUOHEALPB("Duo Heal PB"),
	LEECHPB("Leech PB"),
	CUSTOM("Custom");
	private String name = "";

	@Override
	public String toString()
	{
		return name;
	}
}
