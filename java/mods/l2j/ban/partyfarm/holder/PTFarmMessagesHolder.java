package mods.l2j.ban.partyfarm.holder;

import java.util.ArrayList;
import java.util.List;

public class PTFarmMessagesHolder
{
	private final List<String> _onPrepare = new ArrayList<>();
	private final List<String> _onStart = new ArrayList<>();
	private final List<String> _onEnd = new ArrayList<>();
	
	public void addOnPrepare(String msg)
	{
		if (msg != null && !msg.isEmpty())
			_onPrepare.add(msg);
	}
	
	public void addOnStart(String msg)
	{
		if (msg != null && !msg.isEmpty())
			_onStart.add(msg);
	}
	
	public void addOnEnd(String msg)
	{
		if (msg != null && !msg.isEmpty())
			_onEnd.add(msg);
	}
	
	public List<String> getOnPrepare()
	{
		return _onPrepare;
	}
	
	public List<String> getOnStart()
	{
		return _onStart;
	}
	
	public List<String> getOnEnd()
	{
		return _onEnd;
	}
}