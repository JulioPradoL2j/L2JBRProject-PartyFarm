package mods.l2j.ban.partyfarm.listener.players;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import ext.mods.extensions.listener.actor.player.OnPlayerEnterListener;
import ext.mods.gameserver.model.actor.Player;
import mods.l2j.ban.partyfarm.data.PartyFarmData;
import mods.l2j.ban.partyfarm.holder.PTFarmPlayerMessagesHolder;
import mods.l2j.ban.partyfarm.task.PartyFarmTask;

public class PTFarmOnEnterListener implements OnPlayerEnterListener
{
	private static PTFarmPlayerMessagesHolder msgs = PartyFarmData.getInstance().getPlayerMessages("partyfarm");
	
	@Override
	public void onPlayerEnter(Player player)
	{
		if (PartyFarmTask.getInstance().isRunning())
		{
			String lastStart = PartyFarmTask.getInstance().lastEvent();
			LocalTime startTime = LocalTime.parse(lastStart, DateTimeFormatter.ofPattern("HH:mm"));
			LocalTime now = LocalTime.now();
			
			int durationMinutes = PartyFarmData.getInstance().getConfig().getDuration();
			LocalTime endTime = startTime.plusMinutes(durationMinutes);
			
			Duration remaining = Duration.between(now, endTime);
			long minutes = remaining.toMinutes();
			
			String msgStart;
			if (minutes > 0)
			{
				msgStart = msgs.get("enter_live_with_time").replace("%start%", lastStart).replace("%minutes%", String.valueOf(minutes));
			}
			else
			{
				msgStart = msgs.get("enter_live_no_time").replace("%start%", lastStart);
			}
			
			String msgRegister = msgs.get("enter_register_open");
			
			player.sendMessage(msgStart);
			player.sendMessage(msgRegister);
		}
	}
	
}
