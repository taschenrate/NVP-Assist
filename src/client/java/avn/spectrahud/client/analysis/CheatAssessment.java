package avn.spectrahud.client.analysis;

public record CheatAssessment(
		String label,
		double confidence,
		double reachScore,
		double triggerScore,
		double aimScore,
		double switchScore
) {
}
