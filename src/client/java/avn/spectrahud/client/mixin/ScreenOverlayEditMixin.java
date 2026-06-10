package avn.spectrahud.client.mixin;

import avn.spectrahud.client.gui.HudEditOverlayController;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenOverlayEditMixin {
	@Inject(method = "removed", at = @At("HEAD"))
	private void spectrahud$removed(CallbackInfo ci) {
		if ((Object) this instanceof ChatScreen) {
			HudEditOverlayController.setActive(false);
		}
	}
}
