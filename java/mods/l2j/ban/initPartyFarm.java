package mods.l2j.ban;

import ext.mods.extensions.interfaces.L2JExtension;
import mods.l2j.ban.partyfarm.data.PartyFarmData;
import mods.l2j.ban.partyfarm.holder.PartyFarmConfig;
import mods.l2j.ban.partyfarm.task.PartyFarmTask;

public class initPartyFarm implements L2JExtension
{
	PartyFarmConfig config = PartyFarmData.getInstance().getConfig();
	
	@Override
	public void onLoad()
	{
		PartyFarmData.getInstance();
		
		if (config.isEnabled())
		{
			PartyFarmTask.getInstance().start();
		}
	}
	
	@Override
	public void onDisable()
	{
		
	}
	
	@Override
	public String getName()
	{
		return "Party Farm";
	}
}