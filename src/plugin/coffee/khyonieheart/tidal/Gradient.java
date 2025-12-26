package coffee.khyonieheart.tidal;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;

class Gradient
{
	/**
	 * Colors
	 */
	private final int redStart, greenStart, blueStart, redEnd, greenEnd, blueEnd;
	private final float hueDelta, saturationDelta, valueDelta;
	private final HsvColor startHSV;

	public Gradient(
		String colorA,
		String colorB
	) {
		this.redStart = Integer.parseInt(colorA.substring(1, 3), 16);
		this.redEnd = Integer.parseInt(colorB.substring(1, 3), 16);
		this.greenStart = Integer.parseInt(colorA.substring(3, 5), 16);
		this.greenEnd = Integer.parseInt(colorB.substring(3, 5), 16);
		this.blueStart = Integer.parseInt(colorA.substring(5), 16);
		this.blueEnd = Integer.parseInt(colorB.substring(5), 16);

		HsvColor colorAHSV = toHsv(redStart, greenStart, blueStart);
		HsvColor colorBHSV = toHsv(redEnd, greenEnd, blueEnd);

		this.startHSV = colorAHSV;

		this.hueDelta = colorBHSV.hue() - colorAHSV.hue();
		this.saturationDelta = colorBHSV.saturation() - colorAHSV.saturation();
		this.valueDelta = colorBHSV.value() - colorAHSV.value();
	}

	public BaseComponent[] createComponents(
		String input,
		char... formatting
	) {
		if (input.length() == 0)
		{
			return new ComponentBuilder().create();
		}

		float characterHueDelta = this.hueDelta / input.length();
		float characterSaturationDelta = this.saturationDelta / input.length();
		float characterValueDelta = this.valueDelta / input.length();

		ComponentBuilder builder = new ComponentBuilder();
		float hue = this.startHSV.hue();
		float saturation = this.startHSV.saturation();
		float value = this.startHSV.value();

		for (int i = 0; i < input.length(); i++)
		{
			hue += characterHueDelta;
			saturation += characterSaturationDelta;
			value += characterValueDelta;

			String hex = toRgb(hue, saturation, value);

			TextComponent component = new TextComponent(Character.toString(input.charAt(i)));
			component.setColor(ChatColor.of(hex));
			for (char format : formatting)
			{
				switch (format)
				{
					case 'k' -> component.setObfuscated(true);
					case 'l' -> component.setBold(true);
					case 'm' -> component.setStrikethrough(true);
					case 'n' -> component.setUnderlined(true);
					case 'o' -> component.setItalic(true);
					case 'r' -> component.setReset(true);
				}
			}

			builder.append(component);
		}

		return builder.create();
	}

	private static HsvColor toHsv(
		int red,
		int green,
		int blue
	) {
		float redNormalized = red / 255f;
		float greenNormalized = green / 255f;
		float blueNormalized = blue / 255f;

		// Calculate hue
		float max = Math.max(redNormalized, Math.max(greenNormalized, blueNormalized));
		float min = Math.min(redNormalized, Math.min(greenNormalized, blueNormalized));
		float delta = max - min;

		float hue = 0f;
		if (delta > 1e-6f) {
			if (max == redNormalized) {
				hue = ((greenNormalized - blueNormalized) / delta) % 6f;
			} else if (max == greenNormalized) {
				hue = ((blueNormalized - redNormalized) / delta) + 2f;
			} else {
				hue = ((redNormalized - greenNormalized) / delta) + 4f;
			}

			hue *= 60f;
			if (hue < 0f)
			{
				hue += 360f;
			}
		}

		// Saturation and value
		float saturation = (max == 0f) ? 0f : delta / max;
		float value = max;

		return new HsvColor(hue, saturation, value);
	}

	private static String toRgb(
		float hue,
		float saturation,
		float value
	) {
		hue = (hue % 360f + 360f) % 360f; // Wrap hue if needed

		float chroma = value * saturation;
		float x = chroma * (1 - Math.abs((hue / 60f) % 2 - 1));
		float m = value - chroma;

		float redNormalized = 0, greenNormalized = 0, blueNormalized = 0;

		if (hue < 60)
		{
			redNormalized = chroma;
			greenNormalized = x;
			blueNormalized = 0;
		} else if (hue < 120) {
			redNormalized = x;
			greenNormalized = chroma;
			blueNormalized = 0;
		} else if (hue < 180) {
			redNormalized = 0;
			greenNormalized = chroma;
			blueNormalized = x;
		} else if (hue < 240) {
			redNormalized = 0;
			greenNormalized = x;
			blueNormalized = chroma;
		} else if (hue < 300) {
			redNormalized = x;
			greenNormalized = 0;
			blueNormalized = chroma;
		} else {
			redNormalized = chroma;
			greenNormalized = 0;
			blueNormalized = x;
		}

		int red = Math.round((redNormalized + m) * 255f);
		int green = Math.round((greenNormalized + m) * 255f);
		int blue = Math.round((blueNormalized + m) * 255f);

		return String.format("#%02X%02X%02X", red, green, blue);
	}

	private static record HsvColor(
		float hue,
		float saturation,
		float value
	) {}
}
