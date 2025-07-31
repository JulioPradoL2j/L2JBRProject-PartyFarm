package mods.l2j.ban.events;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ext.mods.gameserver.model.actor.Player;


public class EventsRegisters
{
	private final Map<String, Set<Integer>> _eventPlayers = new ConcurrentHashMap<>();
	
	public boolean registerPlayer(Player player, String eventName)
	{
		return _eventPlayers.computeIfAbsent(eventName.toLowerCase(), k -> ConcurrentHashMap.newKeySet()).add(player.getObjectId());
	}
	
	public boolean unregisterPlayer(Player player, String eventName)
	{
		Set<Integer> set = _eventPlayers.get(eventName.toLowerCase());
		return set != null && set.remove(player.getObjectId());
	}
	
	public boolean isRegistered(Player player, String eventName)
	{
		Set<Integer> set = _eventPlayers.get(eventName.toLowerCase());
		return set != null && set.contains(player.getObjectId());
	}
	
	public Set<Integer> getRegisteredPlayers(String eventName)
	{
		return _eventPlayers.getOrDefault(eventName.toLowerCase(), Collections.emptySet());
	}
	
	public void clear(String eventName)
	{
		Set<Integer> set = _eventPlayers.get(eventName.toLowerCase());
		if (set != null)
			set.clear();
	}
	
	public static EventsRegisters getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EventsRegisters INSTANCE = new EventsRegisters();
	}
}
