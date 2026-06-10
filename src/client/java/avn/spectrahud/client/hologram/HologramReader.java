package avn.spectrahud.client.hologram;

import avn.spectrahud.client.config.SpectraHudConfig;
import avn.spectrahud.client.state.SpectraHudState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HologramReader {
	private static final Pattern AVG_PATTERN = Pattern.compile("AVG\\s*:?\\s*([0-9]+(?:[\\.,][0-9]+)?)\\s*(?:\\((\\d{1,3})%\\))?", Pattern.CASE_INSENSITIVE);
	private static final Pattern NUMBER_PATTERN = Pattern.compile("(?<![A-Za-zА-Яа-яЁё0-9_])([0-9]+(?:[\\.,][0-9]+)?)(?![A-Za-zА-Яа-яЁё0-9_])");

	private final Set<Integer> hiddenEntityIds = new HashSet<>();
	private HologramData data = HologramData.EMPTY;

	public void tick(MinecraftClient client, SpectraHudState state, SpectraHudConfig config) {
		hiddenEntityIds.clear();

		if (!config.hideAnticheatHologram || client.world == null) {
			data = HologramData.EMPTY;
			return;
		}

		Optional<AbstractClientPlayerEntity> suspect = state.getSuspect(client);
		if (suspect.isEmpty()) {
			data = HologramData.EMPTY;
			return;
		}

		Box scanBox = getScanBox(suspect.get());
		List<Entity> carriers = client.world.getEntitiesByClass(Entity.class, scanBox, this::isHologramCarrier);
		List<String> lines = new ArrayList<>();
		boolean anticheatCluster = false;

		for (Entity entity : carriers) {
			String text = extractText(entity);
			if (!text.isBlank()) {
				lines.add(text);
				if (looksLikeAnticheatText(text)) {
					anticheatCluster = true;
				}
			}
		}

		if (anticheatCluster) {
			for (Entity entity : carriers) {
				hiddenEntityIds.add(entity.getId());
			}
		}

		data = parse(lines);
	}

	public boolean shouldHide(Entity entity) {
		if (entity == null) {
			return false;
		}

		if (hiddenEntityIds.contains(entity.getId())) {
			return true;
		}

		if (!avn.spectrahud.client.SpectraHudClient.config().hideAnticheatHologram) {
			return false;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) {
			return false;
		}

		Optional<AbstractClientPlayerEntity> suspect = avn.spectrahud.client.SpectraHudClient.getState().getSuspect(client);
		if (suspect.isEmpty()) {
			return false;
		}

		Box scanBox = getScanBox(suspect.get());
		return scanBox.contains(entity.getPos()) && isHologramCarrier(entity) && (data.isPresent() || looksLikeHologram(entity));
	}

	public void reset() {
		hiddenEntityIds.clear();
		data = HologramData.EMPTY;
	}

	public HologramData getData() {
		return data;
	}

	private HologramData parse(List<String> lines) {
		if (lines.isEmpty()) {
			return HologramData.EMPTY;
		}

		double average = 0.0D;
		int percent = -1;
		List<Double> numbers = new ArrayList<>();

		for (String line : lines) {
			Matcher avgMatcher = AVG_PATTERN.matcher(line);
			while (avgMatcher.find()) {
				average = parseDouble(avgMatcher.group(1));
				if (avgMatcher.group(2) != null) {
					percent = parseInt(avgMatcher.group(2));
				}
			}

			Matcher numberMatcher = NUMBER_PATTERN.matcher(line);
			while (numberMatcher.find()) {
				numbers.add(parseDouble(numberMatcher.group(1)));
			}
		}

		return new HologramData(true, average, percent, numbers, System.currentTimeMillis());
	}

	private boolean looksLikeHologram(Entity entity) {
		return isHologramCarrier(entity) && looksLikeAnticheatText(extractText(entity));
	}

	private boolean isHologramCarrier(Entity entity) {
		if (!(entity instanceof ArmorStandEntity) && !entity.getType().toString().toLowerCase().contains("text_display")) {
			return false;
		}

		String text = extractText(entity);
		return !text.isBlank();
	}

	private boolean looksLikeAnticheatText(String text) {
		String upper = text.toUpperCase();
		String lower = text.toLowerCase();
		return upper.contains("AVG")
				|| text.contains("%")
				|| countDecimalNumbers(text) >= 2
				|| lower.contains("сид")
				|| lower.contains("risk")
				|| lower.contains("гряз");
	}

	private String extractText(Entity entity) {
		Text customName = entity.getCustomName();
		if (customName != null) {
			return customName.getString();
		}

		try {
			Method getText = entity.getClass().getMethod("getText");
			Object value = getText.invoke(entity);
			if (value instanceof Text text) {
				return text.getString();
			}
		} catch (ReflectiveOperationException ignored) {
		}

		return "";
	}

	private static int countDecimalNumbers(String text) {
		int count = 0;
		Matcher matcher = NUMBER_PATTERN.matcher(text);
		while (matcher.find()) {
			if (matcher.group(1).contains(".") || matcher.group(1).contains(",")) {
				count++;
			}
		}
		return count;
	}

	private static double parseDouble(String value) {
		try {
			return Double.parseDouble(value.replace(',', '.'));
		} catch (NumberFormatException exception) {
			return 0.0D;
		}
	}

	private static int parseInt(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			return -1;
		}
	}

	private static Box getScanBox(AbstractClientPlayerEntity suspect) {
		return suspect.getBoundingBox().expand(2.8D, 4.6D, 2.8D).offset(0.0D, 1.25D, 0.0D);
	}
}
