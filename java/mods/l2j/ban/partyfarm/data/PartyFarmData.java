package mods.l2j.ban.partyfarm.data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ext.mods.commons.data.StatSet;
import ext.mods.commons.data.xml.IXmlReader;
import mods.l2j.ban.partyfarm.holder.PTFarmHolder;
import mods.l2j.ban.partyfarm.holder.PTFarmMessagesHolder;
import mods.l2j.ban.partyfarm.holder.PTFarmPlayerMessagesHolder;
import mods.l2j.ban.partyfarm.holder.PTFarmSettingsHolder;
import mods.l2j.ban.partyfarm.holder.PartyFarmConfig;

public class PartyFarmData implements IXmlReader
{
	private final Map<String, List<PTFarmHolder>> _ptfarm = new HashMap<>();
	private PartyFarmConfig _config;
	private final Map<String, List<StatSet>> _teleports = new HashMap<>();
	private final Map<String, PTFarmSettingsHolder> _settings = new HashMap<>();
	private final Map<String, PTFarmMessagesHolder> _messages = new HashMap<>();
	private final Map<String, PTFarmPlayerMessagesHolder> _playersmessages = new HashMap<>();
	
	public PartyFarmData()
	{
		load();
	}
	
	public void reload()
	{
		_ptfarm.clear();
		load();
	}
	
	@Override
	public void load()
	{
		parseFile("./data/custom/mods/partyfarm.xml");
		LOGGER.info("Loaded {" + _ptfarm.size() + "} Partyfarm event.");
		
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "partyfarm", eventsNode ->
		{
			forEach(eventsNode, "event", eventNode ->
			{
				StatSet set = parseAttributes(eventNode);
				String id = set.getString("name", "partyfarm");
				
				boolean enabled = Boolean.parseBoolean(getChildText(eventNode, "enabled"));
				int duration = Integer.parseInt(getChildText(eventNode, "duration"));
				int preparation = Integer.parseInt(getChildText(eventNode, "preparation"));
				
				String[] dayTokens = getChildText(eventNode, "days").split(",");
				List<Integer> days = new ArrayList<>();
				for (String token : dayTokens)
					days.add(Integer.parseInt(token.trim()));
				
				List<String> times = new ArrayList<>();
				forEach(eventNode, "times", timesNode ->
				{
					forEach(timesNode, "time", timeNode -> times.add(timeNode.getTextContent()));
				});
				
				_config = new PartyFarmConfig(enabled, duration, preparation, days, times);
				
				forEach(eventNode, "settings", settingsNode ->
				{
					forEach(settingsNode, "setting", settingNode ->
					{
						StatSet settingSet = parseAttributes(settingNode);
						PTFarmSettingsHolder settings = new PTFarmSettingsHolder(settingSet);
						_settings.put(id, settings);
					});
				});
				
				forEach(eventNode, "messages", msgsNode ->
				{
					PTFarmMessagesHolder holder = new PTFarmMessagesHolder();
					
					forEach(msgsNode, "message", msgNode ->
					{
						StatSet set2 = parseAttributes(msgNode);
						
						holder.addOnPrepare(set2.getString("onPrepare", ""));
						holder.addOnStart(set2.getString("onStart", ""));
						holder.addOnEnd(set2.getString("onEnd", ""));
					});
					
					_messages.put(id, holder);
				});
				
				forEach(eventNode, "spawns", spawnsNode ->
				{
					forEach(spawnsNode, "spawn", spawnNode ->
					{
						StatSet spawnSet = parseAttributes(spawnNode);
						PTFarmHolder spawn = new PTFarmHolder(spawnSet);
						_ptfarm.computeIfAbsent(id, k -> new ArrayList<>()).add(spawn);
					});
				});
				
				forEach(eventNode, "teleports", teleportsNode ->
				{
					forEach(teleportsNode, "teleport", teleportNode ->
					{
						StatSet teleportSet = parseAttributes(teleportNode);
						_teleports.computeIfAbsent(id, k -> new ArrayList<>()).add(teleportSet);
					});
				});
				
				forEach(eventNode, "players", msgsNode ->
				{
					
					PTFarmPlayerMessagesHolder holder = new PTFarmPlayerMessagesHolder();
					
					forEach(msgsNode, "player", msgNode ->
					{
						String key = parseString(msgNode.getAttributes(), "key");
						String value = msgNode.getTextContent().trim();
						holder.addMessage(key, value);
					});
					
					_playersmessages.put(id, holder);
				});
				
			});
		});
	}
	
	public List<PTFarmHolder> getSpawns(String eventId)
	{
		return _ptfarm.getOrDefault(eventId, new ArrayList<>());
	}
	
	public PartyFarmConfig getConfig()
	{
		return _config;
	}
	
	public PTFarmSettingsHolder getSettings(String eventName)
	{
		return _settings.get(eventName);
	}
	
	public List<StatSet> getTeleports(String eventName)
	{
		return _teleports.getOrDefault(eventName, new ArrayList<>());
	}
	
	// Helper: Get text content of a child node by tag name
	private static String getChildText(Node node, String tag)
	{
		Node child = getChild(node, tag);
		return (child != null) ? child.getTextContent().trim() : "";
	}
	
	private static Node getChild(Node node, String tag)
	{
		for (int i = 0; i < node.getChildNodes().getLength(); i++)
		{
			Node child = node.getChildNodes().item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && tag.equals(child.getNodeName()))
				return child;
		}
		return null;
	}
	
	public PTFarmMessagesHolder getMessages(String eventName)
	{
		return _messages.get(eventName);
	}
	
	public PTFarmPlayerMessagesHolder getPlayerMessages(String eventName)
	{
		return _playersmessages.get(eventName);
	}
	
	public static PartyFarmData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PartyFarmData _instance = new PartyFarmData();
	}
}