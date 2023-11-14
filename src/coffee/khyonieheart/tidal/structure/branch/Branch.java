package coffee.khyonieheart.tidal.structure.branch;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
	private Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();

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

	public void addPermission(
		@NotNull String permission
	) {
		Objects.requireNonNull(permission);
		this.permissions = new String[] { permission };
	}

	public void addSenderType(
		@NotNull Class<? extends CommandSender> type	 
	) {
		this.senderType = Objects.requireNonNull(type);
	}

	public void addAnnotations(
		@NotNull Annotation[] annotations
	) {
		for (Annotation a : annotations)
		{
			this.annotations.put(a.annotationType(), a);
		}
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getAnnotation(
		@NotNull Class<? extends Annotation> annotation
	) {
		Objects.requireNonNull(annotation);
		return (T) this.annotations.get(annotation);
	} 

	public boolean hasAnnotation(
		@NotNull Class<? extends Annotation> annotation
	) {
		Objects.requireNonNull(annotation);
		return this.annotations.containsKey(annotation);
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

	@Nullable
	public BranchType getConnectedBranchType()
	{
		if (this.isLeaf())
		{
			return null;
		}

		return this.connectedBranches.values().iterator().next().getBranchType();
	}

	public boolean hasStaticBranch(
		@NotNull String label
	) {
		Objects.requireNonNull(label);

		return this.connectedBranches.containsKey(label);
	}

	@Nullable
	public Branch getStaticBranch(
		@NotNull String label
	) {
		Objects.requireNonNull(label);

		return this.connectedBranches.get(label);
	}

	@Nullable
	public Branch getVariableBranch()
	{
		return this.connectedBranches.values().iterator().next();
	}

	public List<Branch> getBranches()
	{
		return Collections.unmodifiableList(new ArrayList<>(this.connectedBranches.values()));
	}

	public int getBranchesCount()
	{
		return this.connectedBranches.size();
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
