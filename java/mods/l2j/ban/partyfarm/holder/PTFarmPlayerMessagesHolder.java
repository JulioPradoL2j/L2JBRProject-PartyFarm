package mods.l2j.ban.partyfarm.holder;

import java.util.HashMap;
import java.util.Map;

public class PTFarmPlayerMessagesHolder
{
	private final Map<String, String> _messages = new HashMap<>();
	
	public void addMessage(String key, String message)
	{
		_messages.put(key, message);
	}
	
	public String get(String key)
	{
		return _messages.getOrDefault(key, "Message not found: " + key);
	}
	
	public String formatNamed(String key, Map<String, Object> values)
	{
		String msg = get(key);
		for (Map.Entry<String, Object> entry : values.entrySet())
		{
			msg = msg.replace("%" + entry.getKey() + "%", String.valueOf(entry.getValue()));
		}
		return msg;
	}
	
}
