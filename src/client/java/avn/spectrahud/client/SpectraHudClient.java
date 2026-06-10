package avn.spectrahud.client;

import avn.spectrahud.client.analysis.AimAnalyzer;
import avn.spectrahud.client.analysis.CheatClassifier;
import avn.spectrahud.client.analysis.CombatAnalyzer;
import avn.spectrahud.client.config.ConfigManager;
import avn.spectrahud.client.config.SpectraHudConfig;
import avn.spectrahud.client.hologram.HologramReader;
import avn.spectrahud.client.input.SpectraHudKeyBindings;
import avn.spectrahud.client.interaction.SuspectInteractionController;
import avn.spectrahud.client.replay.ReplayIntegration;
import avn.spectrahud.client.render.HudRenderer;
import avn.spectrahud.client.render.VisualRenderer;
import avn.spectrahud.client.state.SpectraHudState;
import avn.spectrahud.client.target.NvpTargetDetector;
import avn.spectrahud.client.target.TargetTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Locale;

public class SpectraHudClient implements ClientModInitializer {
	private static final ConfigManager CONFIG_MANAGER = new ConfigManager();
	private static final SpectraHudState STATE = new SpectraHudState();
	private static final NvpTargetDetector TARGET_DETECTOR = new NvpTargetDetector();
	private static final HologramReader HOLOGRAM_READER = new HologramReader();
	private static final TargetTracker TARGET_TRACKER = new TargetTracker();
	private static final CombatAnalyzer COMBAT_ANALYZER = new CombatAnalyzer();
	private static final AimAnalyzer AIM_ANALYZER = new AimAnalyzer();
	private static final CheatClassifier CHEAT_CLASSIFIER = new CheatClassifier();
	private static final HudRenderer HUD_RENDERER = new HudRenderer();
	private static final VisualRenderer VISUAL_RENDERER = new VisualRenderer();
	private static final ReplayIntegration REPLAY_INTEGRATION = new ReplayIntegration();
	private static final SuspectInteractionController SUSPECT_INTERACTION = new SuspectInteractionController();
	private static final SpectraHudKeyBindings KEY_BINDINGS = new SpectraHudKeyBindings();

	@Override
	public void onInitializeClient() {
		CONFIG_MANAGER.load();
		KEY_BINDINGS.register();

		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> handleIncomingMessage(message));
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleIncomingMessage(message));
		ClientTickEvents.END_CLIENT_TICK.register(SpectraHudClient::tick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> CONFIG_MANAGER.save());
		HudRenderCallback.EVENT.register((drawContext, tickDelta) -> HUD_RENDERER.render(drawContext, tickDelta));
		WorldRenderEvents.AFTER_ENTITIES.register(VISUAL_RENDERER::render);
	}

	private static void handleIncomingMessage(Text message) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (isBanMessageForActiveSuspect(message)) {
			clearSuspect(client, "бан");
			return;
		}

		TARGET_DETECTOR.detect(message, client.world).ifPresent(nick -> switchSuspect(client, nick));
	}

	private static void tick(MinecraftClient client) {
		KEY_BINDINGS.tick(client);

		if (client.world == null || client.player == null) {
			STATE.clearRuntime();
			return;
		}

		STATE.resolveSuspect(client);
		SUSPECT_INTERACTION.tick(client, STATE, config());
		if (STATE.shouldClearMissing(config().autoTeleportToSuspect ? 120 : 45)) {
			clearSuspect(client, "игрок исчез");
			return;
		}

		TARGET_TRACKER.tick(client, STATE, config());
		COMBAT_ANALYZER.tick(client, STATE, TARGET_TRACKER, config());
		AIM_ANALYZER.tick(client, STATE, TARGET_TRACKER, config());
		HOLOGRAM_READER.tick(client, STATE, config());
	}

	public static void switchSuspect(MinecraftClient client, String nick) {
		if (STATE.setSuspect(client, nick)) {
			TARGET_TRACKER.reset();
			COMBAT_ANALYZER.reset();
			AIM_ANALYZER.reset();
			HOLOGRAM_READER.reset();

			if (client.player != null) {
				client.player.sendMessage(Text.literal("[НВП] Слежка: " + nick), false);
			}
		}
	}

	public static void clearSuspect(MinecraftClient client, String reason) {
		if (!STATE.hasSuspect()) {
			return;
		}

		STATE.clearSuspect();
		TARGET_TRACKER.reset();
		COMBAT_ANALYZER.reset();
		AIM_ANALYZER.reset();
		HOLOGRAM_READER.reset();

		if (client.player != null) {
			client.player.sendMessage(Text.literal("[НВП] Слежка завершена: " + reason), false);
		}
	}

	private static boolean isBanMessageForActiveSuspect(Text message) {
		if (message == null || !STATE.hasSuspect()) {
			return false;
		}

		String text = message.getString().toLowerCase(Locale.ROOT);
		String nick = STATE.getActiveSuspectName().toLowerCase(Locale.ROOT);
		if (!text.contains(nick)) {
			return false;
		}

		return text.contains("забан")
				|| text.contains("бан")
				|| text.contains("заблок")
				|| text.contains("наказан")
				|| text.contains("выдано наказание")
				|| text.contains("кик")
				|| text.contains("kick");
	}

	public static SpectraHudConfig config() {
		return CONFIG_MANAGER.get();
	}

	public static ConfigManager getConfigManager() {
		return CONFIG_MANAGER;
	}

	public static SpectraHudState getState() {
		return STATE;
	}

	public static HologramReader getHologramReader() {
		return HOLOGRAM_READER;
	}

	public static TargetTracker getTargetTracker() {
		return TARGET_TRACKER;
	}

	public static CombatAnalyzer getCombatAnalyzer() {
		return COMBAT_ANALYZER;
	}

	public static AimAnalyzer getAimAnalyzer() {
		return AIM_ANALYZER;
	}

	public static CheatClassifier getCheatClassifier() {
		return CHEAT_CLASSIFIER;
	}

	public static HudRenderer getHudRenderer() {
		return HUD_RENDERER;
	}

	public static ReplayIntegration getReplayIntegration() {
		return REPLAY_INTEGRATION;
	}

	public static SuspectInteractionController getSuspectInteraction() {
		return SUSPECT_INTERACTION;
	}
}
