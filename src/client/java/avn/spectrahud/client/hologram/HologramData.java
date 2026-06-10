package avn.spectrahud.client.hologram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HologramData {
	public static final HologramData EMPTY = new HologramData(false, 0.0D, -1, List.of(), 0L);

	private final boolean present;
	private final double average;
	private final int percent;
	private final List<Double> values;
	private final long updatedAtMs;

	public HologramData(boolean present, double average, int percent, List<Double> values, long updatedAtMs) {
		this.present = present;
		this.average = average;
		this.percent = percent;
		this.values = Collections.unmodifiableList(new ArrayList<>(values));
		this.updatedAtMs = updatedAtMs;
	}

	public boolean isPresent() {
		return present;
	}

	public double average() {
		return average;
	}

	public int percent() {
		return percent;
	}

	public List<Double> values() {
		return values;
	}

	public long updatedAtMs() {
		return updatedAtMs;
	}

	public double riskValue() {
		if (percent >= 0) {
			return clamp(percent / 100.0D);
		}
		return clamp(average);
	}

	private static double clamp(double value) {
		return Math.max(0.0D, Math.min(1.0D, value));
	}
}
