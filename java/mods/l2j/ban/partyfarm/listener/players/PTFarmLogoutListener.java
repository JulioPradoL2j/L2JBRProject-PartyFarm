package mods.l2j.ban.partyfarm.listener.players;

import ext.mods.extensions.listener.actor.player.OnPlayerExitListener;
import ext.mods.gameserver.model.actor.Player;
import mods.l2j.ban.events.EventsRegisters;

public class PTFarmLogoutListener implements OnPlayerExitListener
{
	@Override
	public void onPlayerExit(Player player)
	{
		EventsRegisters.getInstance().unregisterPlayer(player, "partyfarm");
	}
	
}
