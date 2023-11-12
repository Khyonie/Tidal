package coffee.khyonieheart.tidal.structure;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.bukkit.command.CommandSender;

import coffee.khyonieheart.hyacinth.util.marker.NotNull;
import coffee.khyonieheart.hyacinth.util.marker.Nullable;

public class Branch
{
	private String label;
	private String permission;
	private Class<? extends CommandSender> senderType;
	private Class<?> type;
	private Set<Annotation> annotations = new HashSet<>(); 
	private Method method;
	private boolean isStatic;
	private boolean isVarargs = false;
	private Map<String, Branch> branches = new HashMap<>();

	public Branch(
		@NotNull String label,
		boolean isStatic,
		@Nullable Method method,
		@Nullable String permission,
		@Nullable Class<? extends CommandSender> senderType,
		@NotNull Class<?> type
	) {
		Objects.requireNonNull(label);
		Objects.requireNonNull(type);

		this.label = label;
		this.permission = permission == null ? null : (permission.equals("NOT_APPLICABLE") ? null : permission);
		this.senderType = senderType == null ? CommandSender.class : senderType;
		this.type = type;
		this.method = method;
		this.isStatic = isStatic;
	}

	public void addBranch(
		@NotNull String label,
		@NotNull Branch branch
	) {
		Objects.requireNonNull(label);
		Objects.requireNonNull(branch);

		this.branches.put(label, branch);
	}

	@NotNull 
	public Set<Branch> getBranches()
	{
		return new HashSet<>(this.branches.values());
	}

	public void setMethod(
		@NotNull Method method
	) {
		this.method = method;
	}

	public void setIsVarArgs()
	{
		this.isVarargs = true;
	}

	public boolean isVarargs()
	{
		return this.isVarargs;
	}

	public boolean isStatic()
	{
		return this.isStatic;
	}

	public boolean isLeaf()
	{
		return this.branches.isEmpty();
	}

	public boolean hasStaticBranch(
		@NotNull String name
	) {
		if (!this.branches.containsKey(name))
		{
			return false;
		}

		return this.branches.get(name).isStatic();
	}

	public boolean hasPermission()
	{
		return this.permission != null;
	}

	public boolean leadsToStatic()
	{
		return !hasVariableBranch();
	}

	public void addAnnotations(
		@NotNull Annotation... annotation
	) {
		for (Annotation a : annotation)
		{
			this.annotations.add(a);
		}
	}

	public Set<Annotation> getAnnotations()
	{
		return this.annotations;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getAnnotation(
		@NotNull Class<T> clazz
	) {
		for (Annotation a : this.annotations)
		{
			if (a.annotationType() == clazz)
			{
				return (T) a;
			}
		}

		return null;
	} 

	public boolean hasAnnotation(
		@NotNull Class<? extends Annotation> clazz
	) {
		return getAnnotation(clazz) != null;
	}

	public boolean hasVariableBranch() 
	{
		if (this.branches.size() == 1)
		{
			return !this.branches.entrySet().iterator().next().getValue().isStatic();
		}

		return false;
	}

	@Nullable
	public Branch getVariableBranch()
	{
		return this.branches.entrySet().iterator().next().getValue();
	}

	@Nullable
	public Branch getStaticBranch(
		@NotNull String name
	) {
		return this.branches.get(name);
	}

	@NotNull
	public String getLabel()
	{
		return this.label;
	}

	@Nullable
	public Method getMethod()
	{
		return this.method;
	}

	@Nullable
	public String getPermission()
	{
		return this.permission;
	}

	@NotNull
	public Class<? extends CommandSender> getSenderType()
	{
		return this.senderType;
	}

	public Class<?> getType()
	{
		return this.type;
	}

	@Override
	public boolean equals(Object object)
	{
		if (object instanceof Branch branch)
		{
			return
				branch.getLabel().equals(this.getLabel()) &&
				branch.getType().equals(this.getType()) &&
				branch.getSenderType() == null ? this.getSenderType() == null : branch.getSenderType().equals(this.getSenderType()) &&
				branch.getPermission() == null ? this.getPermission() == null : branch.getPermission().equals(this.getPermission());
		}

		return false;
	}
}
