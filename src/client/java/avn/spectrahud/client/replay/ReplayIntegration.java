package avn.spectrahud.client.replay;

import avn.spectrahud.client.SpectraHudClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ReplayIntegration {
	public void saveManualClip(MinecraftClient client) {
		if (client.player == null) {
			return;
		}
		if (!SpectraHudClient.isSpectatorMode(client)) {
			client.player.sendMessage(Text.literal("[НВП] Доступно только в spectator"), true);
			return;
		}

		if (!SpectraHudClient.config().replayClips) {
			client.player.sendMessage(Text.literal("[НВП] ReplayMod-клипы выключены"), true);
			return;
		}

		if (!FabricLoader.getInstance().isModLoaded("replaymod")) {
			client.player.sendMessage(Text.literal("[НВП] ReplayMod не найден"), true);
			return;
		}

		client.player.sendMessage(Text.literal("[НВП] ReplayMod-клип: интеграция оставлена на будущую версию"), true);
	}
}
