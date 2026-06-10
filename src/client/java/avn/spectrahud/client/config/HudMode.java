package avn.spectrahud.client.config;

public enum HudMode {
	COMPACT("компактный"),
	NORMAL("обычный");

	private final String title;

	HudMode(String title) {
		this.title = title;
	}

	public String title() {
		return title;
	}

	public HudMode next() {
		return this == COMPACT ? NORMAL : COMPACT;
	}
}
