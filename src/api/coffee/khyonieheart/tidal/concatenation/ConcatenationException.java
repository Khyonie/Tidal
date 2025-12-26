package coffee.khyonieheart.tidal.concatenation;

import coffee.khyonieheart.anenome.NotNull;

public class ConcatenationException extends Exception
{
	private final FailureType type;
	private final int index;

	public ConcatenationException(
		@NotNull FailureType type,
		int index
	) {
		this.type = type;
		this.index = index;
	}

	public FailureType getType()
	{
		return this.type;
	}

	public int getIndex()
	{
		return this.index;
	}

	public static enum FailureType
	{
		UNEXPECTED_START,
		UNEXPECTED_END,
		UNTERMINATED_END
		;
	}
}
