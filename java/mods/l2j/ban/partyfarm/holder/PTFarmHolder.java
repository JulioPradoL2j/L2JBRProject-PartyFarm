package mods.l2j.ban.partyfarm.holder;

import ext.mods.commons.data.StatSet;

public class PTFarmHolder
{
	private final int npcId;
	private final int count;
	private final int x;
	private final int y;
	private final int z;
	private int _respawnDelay;
	
	public PTFarmHolder(StatSet set)
	{
		npcId = set.getInteger("npcId");
		count = set.getInteger("count", 1);
		x = set.getInteger("x");
		y = set.getInteger("y");
		z = set.getInteger("z");
		_respawnDelay = set.getInteger("respawnDelay", 0);
	}
	
	public int getNpcId()
	{
		return npcId;
	}
	
	public int getCount()
	{
		return count;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public int getZ()
	{
		return z;
	}
	
	public int getRespawnDelay()
	{
		return _respawnDelay;
	}
}
