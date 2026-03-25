package com.autoeject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class AutoEjectConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("auto-eject.json");
	public static final List<String> DEFAULT_ITEMS = List.of(
		"diorite",
		"granite",
		"andesite"
	);

	public boolean enabled = true;
	public List<String> items = new ArrayList<>(DEFAULT_ITEMS);

	public static AutoEjectConfig load() {
		if (!Files.exists(CONFIG_PATH)) {
			return defaults();
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			AutoEjectConfig config = GSON.fromJson(reader, AutoEjectConfig.class);
			if (config == null) {
				return defaults();
			}

			config.items = parseItems(String.join(",", config.items));
			return config;
		} catch (IOException | RuntimeException ignored) {
			return defaults();
		}
	}

	public void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException ignored) {
		}
	}

	public String itemsAsText() {
		return String.join(", ", items);
	}

	public void setItemsFromText(String text) {
		items = parseItems(text);
	}

	public static Screen getConfigScreen(Screen parent) {
		return new AutoEjectConfigScreen(parent);
	}

	private static AutoEjectConfig defaults() {
		return new AutoEjectConfig();
	}

	public static List<String> parseItems(String text) {
		LinkedHashSet<String> parsed = new LinkedHashSet<>();
		for (String part : text.split(",")) {
			String normalized = normalizeItem(part);
			if (normalized != null) {
				parsed.add(normalized);
			}
		}

		if (parsed.isEmpty()) {
			parsed.addAll(DEFAULT_ITEMS);
		}

		return new ArrayList<>(parsed);
	}

	private static String normalizeItem(String value) {
		if (value == null) {
			return null;
		}

		String normalized = value.trim().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			return null;
		}
		if (normalized.startsWith("minecraft:")) {
			normalized = normalized.substring("minecraft:".length());
		}

		Identifier id = normalized.contains(":")
			? Identifier.tryParse(normalized)
			: Identifier.withDefaultNamespace(normalized);
		if (id == null) {
			return null;
		}

		Item item = BuiltInRegistries.ITEM.getValue(id);
		if (id.equals(Identifier.withDefaultNamespace("air")) || item == Items.AIR) {
			return null;
		}

		if ("minecraft".equals(id.getNamespace())) {
			return id.getPath();
		}

		return id.toString();
	}
}
