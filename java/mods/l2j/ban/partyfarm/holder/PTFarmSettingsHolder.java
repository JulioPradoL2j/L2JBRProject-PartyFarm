package mods.l2j.ban.partyfarm.holder;

import ext.mods.commons.data.StatSet;

public class PTFarmSettingsHolder
{
	private final int _minPartyRequest;
	private final boolean _checkHwid;
	private final boolean _punishFullPartyOnHwid;
	private final int _minLvlParticipe;
	
	public PTFarmSettingsHolder(StatSet set)
	{
		_minPartyRequest = set.getInteger("minMemberCount");
		_checkHwid = set.getBool("HwidCheck", false);
		_punishFullPartyOnHwid = set.getBool("punishFullPartyOnHwid", false);
		_minLvlParticipe = set.getInteger("minMembersLevel");
		
	}
	
	public int getMemberCount()
	{
		return _minPartyRequest;
	}
	
	public boolean getCheckHwid()
	{
		return _checkHwid;
	}
	
	public boolean punishFullPartyOnHwid()
	{
		return _punishFullPartyOnHwid;
	}
	
	public int getMembersLevel()
	{
		return _minLvlParticipe;
	}
	
}
