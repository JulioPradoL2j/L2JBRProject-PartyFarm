package mods.l2j.ban.partyfarm.task;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import ext.mods.commons.data.StatSet;
import ext.mods.commons.logging.CLogger;
import ext.mods.commons.pool.ThreadPool;
import ext.mods.commons.random.Rnd;
import ext.mods.extensions.listener.manager.PlayerListenerManager;
import ext.mods.gameserver.data.manager.SpawnManager;
import ext.mods.gameserver.data.xml.NpcData;
import ext.mods.gameserver.handler.VoicedCommandHandler;
import ext.mods.gameserver.model.World;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.model.actor.template.NpcTemplate;
import ext.mods.gameserver.model.group.Party;
import ext.mods.gameserver.model.location.Location;
import ext.mods.gameserver.model.spawn.Spawn;
import ext.mods.gameserver.network.serverpackets.ExShowScreenMessage;
import mods.l2j.ban.events.EventsRegisters;
import mods.l2j.ban.events.voiced.VoicedRegisters;
import mods.l2j.ban.partyfarm.data.PartyFarmData;
import mods.l2j.ban.partyfarm.holder.PTFarmHolder;
import mods.l2j.ban.partyfarm.holder.PTFarmMessagesHolder;
import mods.l2j.ban.partyfarm.holder.PTFarmPlayerMessagesHolder;
import mods.l2j.ban.partyfarm.holder.PartyFarmConfig;
import mods.l2j.ban.partyfarm.listener.players.PTFarmLogoutListener;
import mods.l2j.ban.partyfarm.listener.players.PTFarmOnEnterListener;

public class PartyFarmTask
{
	private static final CLogger LOGGER = new CLogger(PartyFarmTask.class.getName());
	
	private static ScheduledFuture<?> eventChecker;
	private static boolean isRunning;
	private static List<Spawn> activeSpawns = Collections.synchronizedList(new ArrayList<>());
	private static boolean mobsSpawned;
	
	private static String lastEventTime;
	private static ScheduledFuture<?> _showScreenMessage;
	
	private static long timeRemaining; // em milissegundos
	private static final int COUNTDOWN_INTERVAL = 1000; // 1 segundo
	
	public void start()
	{
		if (eventChecker == null || eventChecker.isCancelled())
			eventChecker = ThreadPool.scheduleAtFixedRate(() -> checkAndStartEvent(), 500, 1000);
		
		VoicedCommandHandler.getInstance().registerHandler(new VoicedRegisters());
		PlayerListenerManager.getInstance().registerEnterListener(new PTFarmOnEnterListener());
		PlayerListenerManager.getInstance().registerExitListener(new PTFarmLogoutListener());
	}
	
	private static void checkAndStartEvent()
	{
		LocalDateTime now = LocalDateTime.now();
		int currentDay = now.getDayOfWeek().getValue() % 7; // 0 = domingo
		
		PartyFarmConfig config = PartyFarmData.getInstance().getConfig();
		
		if (!config.isEnabled() || !config.getDays().contains(currentDay))
			return;
		
		String nowStr = new SimpleDateFormat("HH:mm").format(new Date());
		
		for (String time : config.getTimes())
		{
			if (nowStr.equals(time) && !isRunning && !nowStr.equals(lastEventTime))
			{
				isRunning = true;
				lastEventTime = nowStr;
				
				PTFarmMessagesHolder messages = PartyFarmData.getInstance().getMessages("partyfarm");
				if (messages != null)
				{
					for (String msg : messages.getOnPrepare())
					{
						World.announceToOnlinePlayers(msg, true);
						
					}
				}
				
				ThreadPool.schedule(() -> spawnMobs(), 1000 * 60 * config.getPreparation());
				
				break;
			}
		}
	}
	
	private static void spawnMobs()
	{
		
		PTFarmMessagesHolder messages = PartyFarmData.getInstance().getMessages("partyfarm");
		if (messages != null)
		{
			for (String msg : messages.getOnStart())
			{
				World.announceToOnlinePlayers(msg, true);
			}
		}
		
		List<PTFarmHolder> spawns = PartyFarmData.getInstance().getSpawns("partyfarm");
		PartyFarmConfig config = PartyFarmData.getInstance().getConfig();
		ThreadPool.schedule(() -> endEvent(), 1000 * 60 * config.getDuration());
		
		timeRemaining = config.getDuration() * 60 * 1000L; // duração em milissegundos
		
		if (_showScreenMessage == null || _showScreenMessage.isCancelled())
		{
			_showScreenMessage = ThreadPool.scheduleAtFixedRate(() -> countdown(), 1000, 1000);
		}
		
		for (PTFarmHolder holder : spawns)
		{
			for (int i = 0; i < holder.getCount(); i++)
			{
				try
				{
					final NpcTemplate template = NpcData.getInstance().getTemplate(holder.getNpcId());
					if (template == null)
					{
						LOGGER.warn("[PartyFarmEvent] Template not found for npcId: " + holder.getNpcId());
						continue;
					}
					
					int x = holder.getX();
					int y = holder.getY();
					int z = holder.getZ();
					
					if (holder.getCount() > 1)
					{
						final int radius = 400;
						final double angle = Rnd.nextDouble() * 2 * Math.PI;
						x += (int) (Math.cos(angle) * Rnd.get(0, radius));
						y += (int) (Math.sin(angle) * Rnd.get(0, radius));
					}
					
					Location loc = new Location(x, y, z);
					Spawn spawn = new Spawn(template);
					
					spawn.setLoc(loc.getX(), loc.getY(), loc.getZ(), 0);
					if (holder.getRespawnDelay() != 0)
						spawn.setRespawnDelay(holder.getRespawnDelay());
					else
					{
						spawn.setRespawnDelay(0);
					}
					
					SpawnManager.getInstance().addSpawn(spawn);
					spawn.doSpawn(false);
					activeSpawns.add(spawn);
					
				}
				catch (Exception e)
				{
					LOGGER.warn("[PartyFarmEvent] Erro ao spawnar NPC: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
		
		List<StatSet> teleportData = PartyFarmData.getInstance().getTeleports("partyfarm");
		if (teleportData.isEmpty())
		{
			LOGGER.warn("No teleport locations configured for partyfarm.");
			return;
		}
		PTFarmPlayerMessagesHolder msgs = PartyFarmData.getInstance().getPlayerMessages("partyfarm");
		
		List<Location> locations = teleportData.stream().map(s -> new Location(s.getInteger("x"), s.getInteger("y"), s.getInteger("z"))).collect(Collectors.toList());
		
		Collections.shuffle(locations);
		
		int requiredMembers = PartyFarmData.getInstance().getSettings("partyfarm").getMemberCount();
		int requiredMembersLevel = PartyFarmData.getInstance().getSettings("partyfarm").getMembersLevel();
		
		if (requiredMembers != 0)
		{
			int i = 0;
			for (int objectId : EventsRegisters.getInstance().getRegisteredPlayers("partyfarm"))
			{
				Player player = World.getInstance().getPlayer(objectId);
				if (player == null || !player.isOnline())
					continue;
				
				for (Player member : player.getParty().getMembers())
				{
					if (member != null && member.isOnline())
					{
						if (member.getParty().getMembers().stream().anyMatch(m -> m.getStatus().getLevel() < requiredMembersLevel))
						{
							member.sendMessage(msgs.get("not_enough_level").replace("%level%", String.valueOf(requiredMembersLevel)));
							
							continue;
						}
						
						Location loc = locations.get(i % locations.size());
						member.teleToLocation(loc);
						member.sendMessage(msgs.get("teleport_success"));
						i++;
					}
				}
				
			}
		}
		else
		{
			int i = 0;
			for (int objectId : EventsRegisters.getInstance().getRegisteredPlayers("partyfarm"))
			{
				Player player = World.getInstance().getPlayer(objectId);
				if (player == null || !player.isOnline())
					continue;
				
				if (player.getStatus().getLevel() < requiredMembersLevel)
				{
					player.sendMessage(msgs.get("solo_not_enough_level").replace("%level%", String.valueOf(requiredMembersLevel)));
					continue;
				}
				
				Location loc = locations.get(i % locations.size());
				player.teleToLocation(loc);
				player.sendMessage(msgs.get("teleport_success"));
				i++;
			}
		}
		
		mobsSpawned = true;
		
	}
	
	private static void countdown()
	{
		if (timeRemaining <= 0)
		{
			_showScreenMessage.cancel(true);
			return;
		}
		
		String message = formatTimeRemaining(timeRemaining);
		
		for (int objectId : EventsRegisters.getInstance().getRegisteredPlayers("partyfarm"))
		{
			Player player = World.getInstance().getPlayer(objectId);
			if (player != null && player.isOnline())
			{
				
				Party party = player.getParty();
				
				if (party == null)
				{
					player.sendPacket(new ExShowScreenMessage(message, 2500, ExShowScreenMessage.SMPOS.BOTTOM_RIGHT, false));
					
					return;
				}
				
				for (Player member : player.getParty().getMembers())
				{
					if (member != null && member.isOnline())
					{
						member.sendPacket(new ExShowScreenMessage(message, 2500, ExShowScreenMessage.SMPOS.BOTTOM_RIGHT, false));
						
					}
				}
			}
		}
		
		timeRemaining -= COUNTDOWN_INTERVAL;
	}
	
	private static void endEvent()
	{
		mobsSpawned = false;
		
		PTFarmMessagesHolder messages = PartyFarmData.getInstance().getMessages("partyfarm");
		if (messages != null)
		{
			for (String msg : messages.getOnEnd())
			{
				World.announceToOnlinePlayers(msg, true);
			}
		}
		if (_showScreenMessage != null && !_showScreenMessage.isCancelled())
		{
			_showScreenMessage.cancel(true);
		}
		
		EventsRegisters.getInstance().getRegisteredPlayers("partyfarm").clear();
		
		unSpawn();
		activeSpawns.clear();
		isRunning = false;
		lastEventTime = "";
		
	}
	
	private static void unSpawn()
	{
		for (Spawn npc : activeSpawns)
		{
			if (npc != null && !npc.getNpc().isDead())
			{
				npc.getNpc().deleteMe();
			}
		}
		activeSpawns.clear();
		
	}
	
	public boolean isRunning()
	{
		return isRunning;
	}
	
	public boolean isMobsSpawned()
	{
		return mobsSpawned;
	}
	
	public String lastEvent()
	{
		return lastEventTime;
	}
	
	public void reset()
	{
		if (eventChecker != null)
		{
			eventChecker.cancel(false);
			eventChecker = null;
		}
	}
	
	private static String formatTimeRemaining(long millis)
	{
		PTFarmPlayerMessagesHolder msgs = PartyFarmData.getInstance().getPlayerMessages("partyfarm");
		
		long totalSeconds = millis / 1000;
		
		if (totalSeconds <= 10)
		{
			return String.valueOf(totalSeconds);
		}
		
		long minutes = totalSeconds / 60;
		long seconds = totalSeconds % 60;
		return String.format(msgs.get("countdown") + " %02d:%02d", minutes, seconds);
	}
	
	public static PartyFarmTask getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PartyFarmTask _instance = new PartyFarmTask();
	}
}