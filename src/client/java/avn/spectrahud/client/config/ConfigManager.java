package avn.spectrahud.client.config;

import avn.spectrahud.SpectraHud;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path path;
	private SpectraHudConfig config = new SpectraHudConfig();

	public ConfigManager() {
		path = FabricLoader.getInstance().getConfigDir().resolve("spectrahud.json");
	}

	public SpectraHudConfig get() {
		return config;
	}

	public void load() {
		try {
			if (Files.exists(path)) {
				config = GSON.fromJson(Files.readString(path), SpectraHudConfig.class);
				if (config == null) {
					config = new SpectraHudConfig();
				}
			}
		} catch (IOException | RuntimeException exception) {
			SpectraHud.LOGGER.warn("Не удалось прочитать конфиг NVP-assist, будут использованы значения по умолчанию", exception);
			config = new SpectraHudConfig();
		}

		config.clamp();
		save();
	}

	public void save() {
		try {
			config.clamp();
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(config));
		} catch (IOException exception) {
			SpectraHud.LOGGER.warn("Не удалось сохранить конфиг NVP-assist", exception);
		}
	}
}
