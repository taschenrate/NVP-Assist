package avn.spectrahud.client.render;

import avn.spectrahud.client.SpectraHudClient;
import avn.spectrahud.client.config.SpectraHudConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class VisualRenderer {
	public void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || client.player == null) {
			return;
		}

		SpectraHudConfig config = SpectraHudClient.config();
		Optional<AbstractClientPlayerEntity> suspectOptional = SpectraHudClient.getState().getSuspect(client);
		if (suspectOptional.isEmpty()) {
			return;
		}

		MatrixStack matrices = context.matrixStack();
		VertexConsumerProvider consumers = context.consumers();
		if (matrices == null || consumers == null) {
			return;
		}

		VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
		Vec3d camera = context.camera().getPos();
		AbstractClientPlayerEntity suspect = suspectOptional.get();
		float tickDelta = context.tickDelta();

		if (config.suspectOutline) {
			drawHitbox(matrices, lines, camera, interpolatedBox(suspect, tickDelta), config.suspectColor);
		}

		if (config.showViewRay) {
			Vec3d start = suspect.getCameraPosVec(tickDelta);
			Vec3d direction = suspect.getRotationVec(tickDelta).normalize();
			Vec3d end = start.add(direction.multiply(config.viewRayLength));
			int rayColor = hitsPlayerHitbox(client, suspect, start, end, tickDelta) ? config.rayHitColor : config.rayMissColor;
			drawThickRay(matrices, lines, camera, start, end, direction, rayColor);
		}
	}

	private void drawThickRay(MatrixStack matrices, VertexConsumer consumer, Vec3d camera, Vec3d start, Vec3d end, Vec3d direction, int color) {
		Vec3d side = direction.crossProduct(new Vec3d(0.0D, 1.0D, 0.0D));
		if (side.lengthSquared() < 1.0E-4D) {
			side = direction.crossProduct(new Vec3d(1.0D, 0.0D, 0.0D));
		}

		side = side.normalize().multiply(0.06D);
		Vec3d up = direction.crossProduct(side).normalize().multiply(0.04D);

		drawLine(matrices, consumer, camera, start, end, color, 1.00F);
		drawLine(matrices, consumer, camera, start.add(side), end.add(side), color, 0.95F);
		drawLine(matrices, consumer, camera, start.subtract(side), end.subtract(side), color, 0.90F);
		drawLine(matrices, consumer, camera, start.add(up), end.add(up), color, 0.78F);
		drawLine(matrices, consumer, camera, start.subtract(up), end.subtract(up), color, 0.78F);
		drawLine(matrices, consumer, camera, start.add(side).add(up), end.add(side).add(up), color, 0.70F);
		drawLine(matrices, consumer, camera, start.add(side).subtract(up), end.add(side).subtract(up), color, 0.70F);
		drawLine(matrices, consumer, camera, start.subtract(side).add(up), end.subtract(side).add(up), color, 0.70F);
		drawLine(matrices, consumer, camera, start.subtract(side).subtract(up), end.subtract(side).subtract(up), color, 0.70F);
	}

	private void drawBox(MatrixStack matrices, VertexConsumer consumer, Vec3d camera, Box box, int color, float alpha) {
		Vec3d v000 = new Vec3d(box.minX, box.minY, box.minZ);
		Vec3d v001 = new Vec3d(box.minX, box.minY, box.maxZ);
		Vec3d v010 = new Vec3d(box.minX, box.maxY, box.minZ);
		Vec3d v011 = new Vec3d(box.minX, box.maxY, box.maxZ);
		Vec3d v100 = new Vec3d(box.maxX, box.minY, box.minZ);
		Vec3d v101 = new Vec3d(box.maxX, box.minY, box.maxZ);
		Vec3d v110 = new Vec3d(box.maxX, box.maxY, box.minZ);
		Vec3d v111 = new Vec3d(box.maxX, box.maxY, box.maxZ);

		drawLine(matrices, consumer, camera, v000, v001, color, alpha);
		drawLine(matrices, consumer, camera, v001, v101, color, alpha);
		drawLine(matrices, consumer, camera, v101, v100, color, alpha);
		drawLine(matrices, consumer, camera, v100, v000, color, alpha);
		drawLine(matrices, consumer, camera, v010, v011, color, alpha);
		drawLine(matrices, consumer, camera, v011, v111, color, alpha);
		drawLine(matrices, consumer, camera, v111, v110, color, alpha);
		drawLine(matrices, consumer, camera, v110, v010, color, alpha);
		drawLine(matrices, consumer, camera, v000, v010, color, alpha);
		drawLine(matrices, consumer, camera, v001, v011, color, alpha);
		drawLine(matrices, consumer, camera, v100, v110, color, alpha);
		drawLine(matrices, consumer, camera, v101, v111, color, alpha);
	}

	private void drawHitbox(MatrixStack matrices, VertexConsumer consumer, Vec3d camera, Box box, int color) {
		drawBox(matrices, consumer, camera, box, color, 1.00F);
		drawBox(matrices, consumer, camera, box.expand(0.012D), color, 0.42F);
	}

	private Box interpolatedBox(AbstractClientPlayerEntity entity, float tickDelta) {
		Vec3d interpolatedPos = entity.getLerpedPos(tickDelta);
		Vec3d offset = interpolatedPos.subtract(entity.getPos());
		return entity.getBoundingBox().offset(offset);
	}

	private boolean hitsPlayerHitbox(MinecraftClient client, AbstractClientPlayerEntity suspect, Vec3d start, Vec3d end, float tickDelta) {
		if (client.world == null) {
			return false;
		}

		for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
			if (player == suspect || player == client.player || player.isSpectator()) {
				continue;
			}

			if (interpolatedBox(player, tickDelta).raycast(start, end).isPresent()) {
				return true;
			}
		}
		return false;
	}

	private void drawLine(MatrixStack matrices, VertexConsumer consumer, Vec3d camera, Vec3d from, Vec3d to, int color, float alphaScale) {
		float red = ((color >> 16) & 0xFF) / 255.0F;
		float green = ((color >> 8) & 0xFF) / 255.0F;
		float blue = (color & 0xFF) / 255.0F;
		float alpha = ((color >>> 24) & 0xFF) / 255.0F * alphaScale;
		float x1 = (float) (from.x - camera.x);
		float y1 = (float) (from.y - camera.y);
		float z1 = (float) (from.z - camera.z);
		float x2 = (float) (to.x - camera.x);
		float y2 = (float) (to.y - camera.y);
		float z2 = (float) (to.z - camera.z);
		MatrixStack.Entry entry = matrices.peek();

		consumer.vertex(entry.getPositionMatrix(), x1, y1, z1).color(red, green, blue, alpha).normal(entry.getNormalMatrix(), 0.0F, 1.0F, 0.0F).next();
		consumer.vertex(entry.getPositionMatrix(), x2, y2, z2).color(red, green, blue, alpha).normal(entry.getNormalMatrix(), 0.0F, 1.0F, 0.0F).next();
	}
}
