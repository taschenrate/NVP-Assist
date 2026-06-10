package avn.spectrahud;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpectraHud implements ModInitializer {
	public static final String MOD_ID = "spectrahud";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("NVP-assist загружен");
	}
}
