package avn.spectrahud.client.target;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NvpTargetDetector {
	private static final Pattern SUSPECT_PATTERN = Pattern.compile(
			"(?:Не\\s+отходите\\s+далеко\\s+от|(?:Начато\\s+)?наблюдение\\s+за\\s+игроком)\\s*[:：\\-]?\\s*([A-Za-zА-Яа-яЁё0-9_]{2,32})",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
	);
	private static final String[] TRIGGERS = {
			"не отходите далеко от",
			"начато наблюдение за игроком",
			"наблюдение за игроком"
	};

	public Optional<String> detect(Text message, ClientWorld world) {
		if (message == null) {
			return Optional.empty();
		}

		String raw = message.getString();
		String normalized = normalize(raw);
		if (!hasTrigger(normalized)) {
			return Optional.empty();
		}

		if (world != null) {
			for (AbstractClientPlayerEntity player : world.getPlayers()) {
				String name = player.getGameProfile().getName();
				if (containsNick(raw, name)) {
					return Optional.of(name);
				}
			}
		}

		Matcher matcher = SUSPECT_PATTERN.matcher(raw);
		if (matcher.find()) {
			return Optional.of(cleanNick(matcher.group(1)));
		}

		return Optional.empty();
	}

	private static boolean containsNick(String text, String nick) {
		Pattern nickPattern = Pattern.compile("(?<![A-Za-zА-Яа-яЁё0-9_])" + Pattern.quote(nick) + "(?![A-Za-zА-Яа-яЁё0-9_])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		return nickPattern.matcher(text).find();
	}

	private static String normalize(String value) {
		return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
	}

	private static boolean hasTrigger(String normalized) {
		for (String trigger : TRIGGERS) {
			if (normalized.contains(trigger)) {
				return true;
			}
		}
		return false;
	}

	private static String cleanNick(String value) {
		return value.replaceAll("[^A-Za-zА-Яа-яЁё0-9_]", "").trim();
	}
}
