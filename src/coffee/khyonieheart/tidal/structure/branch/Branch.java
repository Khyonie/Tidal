package coffee.khyonieheart.tidal.structure.branch;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.command.CommandSender;

import coffee.khyonieheart.hyacinth.util.marker.NotNull;
import coffee.khyonieheart.hyacinth.util.marker.Nullable;
import coffee.khyonieheart.tidal.structure.BranchType;
import coffee.khyonieheart.tidal.structure.Protected;

public abstract class Branch
{
	private String label;
	private Map<String, Branch> connectedBranches = new HashMap<>();
	private Class<? extends CommandSender> senderType;
	private String[] permissions;
	private Method executor;

	public Branch(
		@Nullable String label, 
		@Nullable Protected commandProtectionData
	) {
		this.label = label == null ? "<unnamed>" : label;
		this.permissions = commandProtectionData == null ? new String[0] : commandProtectionData.permission();
		this.senderType = commandProtectionData == null ? CommandSender.class : commandProtectionData.senderType();
	}

	public abstract BranchType getBranchType();

	public boolean isAuthorized(
		@NotNull CommandSender sender
	) {
		Objects.requireNonNull(sender);
		if (!senderType.isAssignableFrom(sender.getClass()))
		{
			return false;
		}

		for (String permission : permissions)
		{
			if (!sender.hasPermission(permission))
			{
				return false;
			}
		}

		return true;
	}

	public void attach(
		@NotNull Branch branch
	) {
		Objects.requireNonNull(branch);
		connectedBranches.put(branch.getLabel(), branch);
	}

	public void setExecutor(
		@NotNull Method method
	) {
		this.executor = Objects.requireNonNull(method);
	}

	public void setSenderType(
		@NotNull Class<? extends CommandSender> senderType
	) {
		this.senderType = Objects.requireNonNull(senderType);
	}
	
	public void merge(
		@NotNull Branch branch
	)
		throws IllegalArgumentException
	{
		// Make sure this class is either equal or a subclass of the given branch
		if (branch.getClass().isAssignableFrom(this.getClass()))
		{
			throw new IllegalArgumentException("Cannot merge incompatible branches \"" + this.getLabel() + " (typed " + this.getClass().getName() + ") and " + branch.getLabel() + " (typed " + branch.getClass().getName() + ")");
		}
		
		// Permissions
		List<String> permissions = new ArrayList<>();
		permissions.addAll(Arrays.asList(this.permissions));
		for (String perm : branch.getPermissions())
		{
			if (!permissions.contains(perm))
			{
				permissions.add(perm);
			}
		}

		this.permissions = permissions.toArray(new String[permissions.size()]);

		// Executors
		if (this.getExecutor() == null && branch.getExecutor() != null)
		{
			this.setExecutor(branch.getExecutor());
		}

		// Sender type
		if (!branch.getSenderType().isAssignableFrom(this.getSenderType()))
		{
			this.senderType = branch.getSenderType();
		}
	}

	// Traversal aids
	//-------------------------------------------------------------------------------- 
	
	public boolean isLeaf()
	{
		return this.connectedBranches.size() == 0;
	}

	// Getters
	//-------------------------------------------------------------------------------- 

	@NotNull
	public String getLabel()
	{
		return this.label;
	}

	@NotNull
	public String[] getPermissions()
	{
		return this.permissions;
	}

	@NotNull
	public Class<? extends CommandSender> getSenderType()
	{
		return this.senderType;
	}

	@Nullable
	public Method getExecutor()
	{
		return this.executor;
	}

	// Misc.
	//-------------------------------------------------------------------------------- 

	public boolean equals(
		Object object
	) {
		if (object == null)
		{
			return false;
		}

		if (object instanceof Branch branch)
		{
			return branch.getLabel().equals(this.label) && Arrays.equals(branch.getPermissions(), this.permissions) && branch.getSenderType().equals(this.senderType) && (branch.getExecutor() == this.executor);
		}

		return false;
	}
}