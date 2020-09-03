package com.baplus;

import lombok.Getter;
import net.runelite.api.widgets.WidgetInfo;

enum Role
{
	ATTACKER(WidgetInfo.BA_ATK_LISTEN_TEXT, WidgetInfo.BA_ATK_CALL_TEXT, WidgetInfo.BA_ATK_ROLE_TEXT, WidgetInfo.BA_ATK_ROLE_SPRITE),
	DEFENDER(WidgetInfo.BA_DEF_LISTEN_TEXT, WidgetInfo.BA_DEF_CALL_TEXT, WidgetInfo.BA_DEF_ROLE_TEXT, WidgetInfo.BA_DEF_ROLE_SPRITE),
	COLLECTOR(WidgetInfo.BA_COLL_LISTEN_TEXT, WidgetInfo.BA_COLL_CALL_TEXT, WidgetInfo.BA_COLL_ROLE_TEXT, WidgetInfo.BA_COLL_ROLE_SPRITE),
	HEALER(WidgetInfo.BA_HEAL_LISTEN_TEXT, WidgetInfo.BA_HEAL_CALL_TEXT, WidgetInfo.BA_HEAL_ROLE_TEXT, WidgetInfo.BA_HEAL_ROLE_SPRITE);

	@Getter
	private final WidgetInfo listen;
	@Getter
	private final WidgetInfo call;
	@Getter
	private final WidgetInfo roleText;
	@Getter
	private final WidgetInfo roleSprite;

	Role(WidgetInfo listen, WidgetInfo call, WidgetInfo role, WidgetInfo roleSprite)
	{
		this.listen = listen;
		this.call = call;
		this.roleText = role;
		this.roleSprite = roleSprite;
	}

	@Override
	public String toString()
	{
		return name();
	}
}