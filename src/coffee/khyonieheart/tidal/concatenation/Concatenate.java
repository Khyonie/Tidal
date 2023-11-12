package coffee.khyonieheart.tidal.concatenation;

import java.util.ArrayList;
import java.util.List;

import coffee.khyonieheart.hyacinth.util.marker.NotNull;
import coffee.khyonieheart.tidal.concatenation.ConcatenationException.FailureType;

public class Concatenate
{
	@NotNull
	public static String[] concatenate(
		char terminatorChars,
		char concatChar,
		String[] input
	)
		throws ConcatenationException
	{
		return concatenate(terminatorChars, terminatorChars, concatChar, input);
	}

	@NotNull
	public static String[] concatenate(
		char startChar, 
		char endChar,
		char concatChar,
		String[] input
	)
		throws ConcatenationException
	{
		List<String> data = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		
		for (int i = 0; i < input.length; i++)
		{
			String s = input[i];

			if (s.length() == 0)
			{
				if (builder.isEmpty())
				{
					data.add("");
					continue;
				}
				continue;
			}

			if (s.length() == 1 && (s.charAt(0) == startChar || s.charAt(0) == endChar))
			{
				if (builder.isEmpty())
				{
					builder.append(concatChar);
					continue;
				}

				builder.append("");
				data.add(builder.toString());

				builder = new StringBuilder();
				continue;
			}

			if (s.charAt(0) == startChar)
			{
				if (!builder.isEmpty())	
				{
					throw new ConcatenationException(FailureType.UNEXPECTED_START, i);
				}

				if (s.charAt(s.length() - 1) == endChar)
				{
					data.add(s.substring(1, s.length() - 1));
					continue;
				}

				builder.append(s.substring(1));
				builder.append(concatChar);

				continue;
			}

			if (s.charAt(s.length() - 1) == endChar)
			{
				if (builder.isEmpty())
				{
					throw new ConcatenationException(FailureType.UNEXPECTED_END, i);
				}

				builder.append(s.substring(0, s.length() - 1));
				data.add(builder.toString());

				builder = new StringBuilder();

				continue;
			}

			if (!builder.isEmpty())
			{
				builder.append(s);
				builder.append(concatChar);
				
				continue;
			}

			data.add(s);
		}

		if (!builder.isEmpty())
		{
			throw new ConcatenationException(FailureType.UNTERMINATED_END, input.length - 1);
		}

		//data.removeIf(s -> s.equals(""));

		return data.toArray(new String[data.size()]);
	}
}
