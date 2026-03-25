package com.autoeject;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import org.lwjgl.glfw.GLFW;

public class AutoEjectClient implements ClientModInitializer {
	private static final AutoEjectConfig CONFIG = AutoEjectConfig.load();
	private static final KeyMapping TOGGLE_KEY = new KeyMapping(
		"Toggle Auto Eject",
		GLFW.GLFW_KEY_PERIOD,
		KeyMapping.Category.MISC
	);

	@Override
	public void onInitializeClient() {
		KeyBindingHelper.registerKeyBinding(TOGGLE_KEY);
		ClientTickEvents.END_CLIENT_TICK.register(this::tickClient);
	}

	private void tickClient(Minecraft client) {
		while (TOGGLE_KEY.consumeClick()) {
			CONFIG.enabled = !CONFIG.enabled;
			CONFIG.save();
			if (client.player != null) {
				client.player.displayClientMessage(toggleMessage(), true);
			}
		}

		if (!CONFIG.enabled) {
			return;
		}

		dropConfiguredItems(client);
	}

	private void dropConfiguredItems(Minecraft client) {
		if (client.player == null || client.gameMode == null) {
			return;
		}

		AbstractContainerMenu menu = client.player.containerMenu;
		for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
			Slot slot = menu.slots.get(slotIndex);
			if (!(slot.container instanceof Inventory) || !slot.hasItem()) {
				continue;
			}

			if (!shouldAutoEject(slot.getItem().getItem())) {
				continue;
			}

			client.gameMode.handleInventoryMouseClick(
				menu.containerId,
				slotIndex,
				1,
				ClickType.THROW,
				client.player
			);
			return;
		}
	}

	private boolean shouldAutoEject(Item item) {
		Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
		if (itemId == null) {
			return false;
		}

		return "minecraft".equals(itemId.getNamespace())
			? CONFIG.items.contains(itemId.getPath())
			: CONFIG.items.contains(itemId.toString());
	}

	public static AutoEjectConfig getConfig() {
		return CONFIG;
	}

	public static void updateConfig(boolean enabled, String itemsText, String toggleKeyName) {
		CONFIG.enabled = enabled;
		CONFIG.setItemsFromText(itemsText);
		CONFIG.save();
		setToggleKey(toggleKeyName);
	}

	private static Component toggleMessage() {
		String stateText;
		ChatFormatting stateColor;

		if (CONFIG.enabled) {
			stateText = "Enabled";
			stateColor = ChatFormatting.GREEN;
		} else {
			stateText = "Disabled";
			stateColor = ChatFormatting.RED;
		}

		return Component.literal("Auto Eject " + stateText)
			.withStyle(stateColor);
	}

	public static String getToggleKeyName() {
		return TOGGLE_KEY.saveString();
	}

	public static Component getToggleKeyDisplayName() {
		return TOGGLE_KEY.getTranslatedKeyMessage();
	}

	public static void setToggleKey(String keyName) {
		TOGGLE_KEY.setKey(InputConstants.getKey(keyName));
		KeyMapping.resetMapping();

		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			client.options.save();
		}
	}
}
