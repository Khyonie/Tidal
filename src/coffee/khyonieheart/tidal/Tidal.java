package coffee.khyonieheart.tidal;

import coffee.khyonieheart.hyacinth.module.HyacinthModule;

public class Tidal implements HyacinthModule
{
	private static Tidal instance;

	@Override
	public void onEnable() 
	{
		instance = this;

		//TypeManager.registerParser(new IntegerParser());
		//TypeManager.registerParser(new StringParser());
		//TypeManager.registerParser(new FloatParser());
	}

	@Override
	public void onDisable() 
	{
		
	}

	public static Tidal getInstance()
	{
		return instance;
	}
}
