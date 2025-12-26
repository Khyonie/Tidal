package coffee.khyonieheart.tidal.structure.branch;

import coffee.khyonieheart.tidal.structure.BranchType;
import coffee.khyonieheart.tidal.structure.Protected;

public class StaticBranch extends Branch
{
	public StaticBranch(
		String label,
		Protected commandProtectionData
	) {
		super(label, commandProtectionData);
	}

	@Override
	public BranchType getBranchType() 
	{
		return BranchType.STATIC;
	}
}
