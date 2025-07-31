package mods.l2j.ban.events.voiced;

import java.util.List;
import java.util.stream.Collectors;

import ext.mods.commons.data.StatSet;
import ext.mods.commons.random.Rnd;
import ext.mods.gameserver.enums.TeamType;
import ext.mods.gameserver.handler.IVoicedCommandHandler;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.model.group.Party;
import ext.mods.gameserver.model.location.Location;
import mods.l2j.ban.events.EventsRegisters;
import mods.l2j.ban.partyfarm.data.PartyFarmData;
import mods.l2j.ban.partyfarm.holder.PTFarmPlayerMessagesHolder;
import mods.l2j.ban.partyfarm.holder.PartyFarmConfig;
import mods.l2j.ban.partyfarm.task.PartyFarmTask;

public class VoicedRegisters implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
		"register",
		"unregister"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String params)
	{
		if (player == null)
			return false;
		
		if (command.equalsIgnoreCase("register"))
		{
			if (params == null || params.isEmpty())
			{
				player.sendMessage("Usage: .register partyfarm");
				return true;
			}
			
			String eventName = params.toLowerCase();
			
			switch (eventName)
			{
				case "partyfarm":
					if (!PartyFarmTask.getInstance().isRunning())
					{
						PartyFarmConfig config = PartyFarmData.getInstance().getConfig();
						
						if (config == null || !config.isEnabled())
						{
							player.sendMessage("The Party Farm event is currently disabled.");
							return true;
						}
						
						String daysString = config.getDays().stream().map(VoicedRegisters::dayOfWeekToString).collect(Collectors.joining(", "));
						String timesString = String.join(", ", config.getTimes());
						
						String message = "The Party Farm event is not currently running. Scheduled days: " + daysString + " at " + timesString + ".";
						player.sendMessage(message);
						return true;
					}
					
					int requiredMembers = PartyFarmData.getInstance().getSettings("partyfarm").getMemberCount();
					int requiredMembersLevel = PartyFarmData.getInstance().getSettings("partyfarm").getMembersLevel();
					PTFarmPlayerMessagesHolder msgs = PartyFarmData.getInstance().getPlayerMessages("partyfarm");
					
					if (requiredMembers != 0)
					{
						Party party = player.getParty();
						
						if (party == null)
						{
							player.sendMessage(msgs.get("need_party"));
							return true;
						}
						
						if (!party.isLeader(player))
						{
							player.sendMessage(msgs.get("need_leader"));
							return true;
						}
						
						if (party.getMembersCount() < requiredMembers)
						{
							player.sendMessage(msgs.get("not_enough_members").replace("%members%", String.valueOf(requiredMembers)));
							return true;
						}
						
						if (party.getMembers().stream().anyMatch(m -> m.getStatus().getLevel() < requiredMembersLevel))
						{
							for (Player member : party.getMembers())
							{
								if (member != null && member.isOnline())
								{
									member.sendMessage(msgs.get("not_enough_level").replace("%level%", String.valueOf(requiredMembersLevel)));
									
								}
							}
							
							return true;
						}
						
						if (!EventsRegisters.getInstance().registerPlayer(player, eventName))
						{
							for (Player member : party.getMembers())
							{
								if (member != null && member.isOnline())
								{
									member.sendMessage(msgs.get("already_registered"));
								}
							}
							
							return true;
						}
						
						for (Player member : party.getMembers())
						{
							if (member != null && member.isOnline())
							{
								member.sendMessage(msgs.get("registered_success"));
							}
						}
						if (PartyFarmTask.getInstance().isMobsSpawned())
						{
							if (party.getMembersCount() < requiredMembers)
							{
								for (Player member : party.getMembers())
								{
									if (member != null && member.isOnline())
									{
										member.sendMessage(msgs.get("cannot_teleport_members"));
									}
								}
								
								return true;
							}
							
							List<StatSet> teleportData = PartyFarmData.getInstance().getTeleports("partyfarm");
							
							if (!teleportData.isEmpty())
							{
								Location randomLoc = teleportData.stream().map(s -> new Location(s.getInteger("x"), s.getInteger("y"), s.getInteger("z"))).collect(Collectors.collectingAndThen(Collectors.toList(), list -> list.get(Rnd.get(list.size()))));
								
								for (Player member : party.getMembers())
								{
									if (member != null && member.isOnline())
									{
										if (party.getMembers().stream().anyMatch(m -> m.getStatus().getLevel() < requiredMembersLevel))
										{
											
											member.sendMessage(msgs.get("not_enough_level").replace("%level%", String.valueOf(requiredMembersLevel)));
											
											return true;
										}
										
										member.teleToLocation(randomLoc);
										member.sendMessage(msgs.get("teleport_success"));
										
									}
								}
							}
						}
					}
					else
					{
						
						if (player.getStatus().getLevel() < requiredMembersLevel)
						{
							
							player.sendMessage(msgs.get("solo_not_enough_level").replace("%level%", String.valueOf(requiredMembersLevel)));
							
							return true;
						}
						
						if (!EventsRegisters.getInstance().registerPlayer(player, eventName))
						{
							player.sendMessage(msgs.get("solo_already_registered"));
							return true;
						}
						
						player.sendMessage(msgs.get("solo_registered_success"));
						
						if (PartyFarmTask.getInstance().isMobsSpawned())
						{
							
							List<StatSet> teleportData = PartyFarmData.getInstance().getTeleports("partyfarm");
							
							if (!teleportData.isEmpty())
							{
								Location randomLoc = teleportData.stream().map(s -> new Location(s.getInteger("x"), s.getInteger("y"), s.getInteger("z"))).collect(Collectors.collectingAndThen(Collectors.toList(), list -> list.get(Rnd.get(list.size()))));
								
								if (player.getStatus().getLevel() < requiredMembersLevel)
								{
									player.sendMessage(msgs.get("solo_low_level"));
									return true;
								}
								
								player.teleToLocation(randomLoc);
								player.sendMessage(msgs.get("teleport_success"));
								
							}
						}
					}
					
					break;
				
				default:
					player.sendMessage("Unknown event: " + eventName);
					break;
			}
			return true;
		}
		
		if (command.equalsIgnoreCase("unregister"))
		{
			if (params == null || params.isEmpty())
			{
				player.sendMessage("Usage: .unregister partyfarm");
				return true;
			}
			
			String eventName = params.toLowerCase();
			
			if (EventsRegisters.getInstance().unregisterPlayer(player, eventName))
				player.sendMessage("You have been unregistered from " + eventName + " event.");
			else
				player.sendMessage("You are not registered in " + eventName + ".");
			
			if (player.getTeam() != TeamType.NONE)
			{
				player.setTeam(TeamType.NONE);
			}
			return true;
		}
		
		return false;
	}
	
	private static String dayOfWeekToString(int day)
	{
		final String[] days =
		{
			"Sunday",
			"Monday",
			"Tuesday",
			"Wednesday",
			"Thursday",
			"Friday",
			"Saturday"
		};
		if (day >= 0 && day <= 6)
			return days[day];
		return "Unknown(" + day + ")";
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}