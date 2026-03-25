package com.autoeject;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class AutoEjectConfigScreen extends Screen {
	private static final Component TITLE = Component.literal("Auto Eject Settings");

	private final Screen parent;
	private boolean enabledValue = AutoEjectClient.getConfig().enabled;
	private boolean excludeHotbarValue = Boolean.TRUE.equals(AutoEjectClient.getConfig().excludeHotbar);
	private final List<String> itemValues = new ArrayList<>(AutoEjectClient.getConfig().items);
	private String toggleKeyNameValue = AutoEjectClient.getToggleKeyName();
	private boolean listeningForToggleKey;

	private EditBox itemsInput;
	private Button enabledButton;
	private Button excludeHotbarButton;
	private Button toggleKeyButton;
	private Button addItemButton;
	private Button removeItemButton;
	private Button resetItemsButton;
	private final List<Button> itemEntryButtons = new ArrayList<>();
	private final List<ScrollableWidget> scrollableWidgets = new ArrayList<>();
	private int scrollAreaTop;
	private int scrollAreaBottom;
	private int maxScroll;
	private int scrollOffset;
	private int layoutFieldX;
	private int layoutFieldWidth;
	private int layoutButtonsY;
	private int layoutControlHeight;
	private int layoutItemsTop;
	private int selectedItemIndex = -1;

	public AutoEjectConfigScreen(Screen parent) {
		super(TITLE);
		this.parent = parent;
	}

	@Override
	protected void init() {
		clearWidgets();
		scrollableWidgets.clear();
		scrollOffset = 0;

		int centerX = width / 2;
		int titleWidth = font.width(title);
		int titleY = 20;
		int titleGap = 20;
		int rowHeight = 27;
		int controlHeight = 20;
		int leftWidth = 110;
		int fieldWidth = 186;
		int addButtonWidth = 56;
		int panelWidth = leftWidth + 10 + fieldWidth;
		int panelLeft = centerX - (panelWidth / 2);
		int left = panelLeft;
		int fieldX = panelLeft + leftWidth + 10;
		int rowY = titleY + font.lineHeight + titleGap;
		int buttonsY = height - 28;
		layoutButtonsY = buttonsY;
		layoutFieldX = fieldX;
		layoutFieldWidth = fieldWidth;
		layoutControlHeight = controlHeight;
		scrollAreaTop = rowY;
		scrollAreaBottom = buttonsY - 8;

		addRenderableOnly(new StringWidget(centerX - (titleWidth / 2), titleY, titleWidth, controlHeight, title, font));

		addScrollableWidget(addRenderableOnly(new StringWidget(left, rowY + 5, leftWidth, controlHeight, Component.literal("Kill Switch"), font)), rowY + 5);
		enabledButton = addScrollableWidget(addRenderableWidget(Button.builder(toggleLabel(), button -> {
			enabledValue = !enabledValue;
			button.setMessage(toggleLabel());
		}).bounds(fieldX, rowY, fieldWidth, controlHeight).build()), rowY);

		rowY += rowHeight;
		addScrollableWidget(addRenderableOnly(new StringWidget(left, rowY + 5, leftWidth, controlHeight, Component.literal("Toggle Key"), font)), rowY + 5);
		toggleKeyButton = addScrollableWidget(addRenderableWidget(Button.builder(toggleKeyLabel(), button -> {
			listeningForToggleKey = !listeningForToggleKey;
			button.setMessage(toggleKeyLabel());
		}).bounds(fieldX, rowY, fieldWidth, controlHeight).build()), rowY);

		rowY += rowHeight;
		addScrollableWidget(addRenderableOnly(new StringWidget(left, rowY + 5, leftWidth, controlHeight, Component.literal("Skip Hotbar"), font)), rowY + 5);
		excludeHotbarButton = addScrollableWidget(addRenderableWidget(Button.builder(hotbarLabel(), button -> {
			excludeHotbarValue = !excludeHotbarValue;
			button.setMessage(hotbarLabel());
		}).bounds(fieldX, rowY, fieldWidth, controlHeight).build()), rowY);

		rowY += rowHeight;
		addScrollableWidget(addRenderableOnly(new StringWidget(left, rowY + 5, leftWidth, controlHeight, Component.literal("Eject List"), font)), rowY + 5);
		itemsInput = addScrollableWidget(addRenderableWidget(new EditBox(font, fieldX, rowY, fieldWidth - addButtonWidth - 5, controlHeight, Component.literal("unwanted_item"))), rowY);
		itemsInput.setMaxLength(128);
		itemsInput.setHint(Component.literal("unwanted_item"));
		addItemButton = addScrollableWidget(addRenderableWidget(Button.builder(Component.literal("Add"), button -> addItemEntry())
			.bounds(fieldX + fieldWidth - addButtonWidth, rowY, addButtonWidth, controlHeight)
			.build()), rowY);

		rowY += rowHeight;
		layoutItemsTop = rowY;
		rowY = layoutItemsTop + 30;
		removeItemButton = addScrollableWidget(addRenderableWidget(Button.builder(Component.literal("Remove"), button -> removeSelectedItemEntry())
			.bounds(fieldX, rowY, (fieldWidth - 5) / 2, controlHeight)
			.build()), rowY);
		resetItemsButton = addScrollableWidget(addRenderableWidget(Button.builder(Component.literal("Defaults"), button -> resetItemsToDefaults())
			.bounds(fieldX + ((fieldWidth - 5) / 2) + 5, rowY, (fieldWidth - 5) / 2, controlHeight)
			.build()), rowY);
		refreshItemEntries();
		removeItemButton.active = false;

		addRenderableWidget(Button.builder(Component.literal("Done"), button -> saveAndClose())
			.bounds(centerX + 5, buttonsY, 150, controlHeight)
			.build());
		addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
			.bounds(centerX - 155, buttonsY, 150, controlHeight)
			.build());

		setInitialFocus(itemsInput);
	}

	@Override
	public void onClose() {
		if (minecraft != null) {
			minecraft.setScreen(parent);
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (listeningForToggleKey) {
			if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
				listeningForToggleKey = false;
			} else {
				toggleKeyNameValue = InputConstants.getKey(event).getName();
				listeningForToggleKey = false;
			}
			toggleKeyButton.setMessage(toggleKeyLabel());
			return true;
		}

		if (itemsInput != null && itemsInput.isFocused() && event.key() == GLFW.GLFW_KEY_ENTER) {
			addItemEntry();
			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
			return true;
		}
		if (mouseY >= scrollAreaTop && mouseY <= scrollAreaBottom && maxScroll > 0) {
			scrollOffset = clampScroll(scrollOffset - (int) (verticalAmount * 16.0));
			applyScroll();
			return true;
		}
		return false;
	}

	private Component toggleLabel() {
		String stateText;
		ChatFormatting stateColor;

		if (enabledValue) {
			stateText = "Enabled";
			stateColor = ChatFormatting.GREEN;
		} else {
			stateText = "Disabled";
			stateColor = ChatFormatting.RED;
		}

		return Component.literal(stateText)
			.withStyle(stateColor);
	}

	private void saveAndClose() {
		AutoEjectClient.updateConfig(enabledValue, excludeHotbarValue, String.join(", ", itemValues), toggleKeyNameValue);
		onClose();
	}

	private Component hotbarLabel() {
		String stateText;
		ChatFormatting stateColor;

		if (excludeHotbarValue) {
			stateText = "On";
		} else {
			stateText = "Off";
		}

		return Component.literal(stateText);
	}

	private Component toggleKeyLabel() {
		if (listeningForToggleKey) {
			Component keyName = InputConstants.getKey(toggleKeyNameValue)
				.getDisplayName()
				.copy()
				.withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE);
			return Component.empty()
				.append(Component.literal("> ").withStyle(ChatFormatting.YELLOW))
				.append(keyName)
				.append(Component.literal(" <").withStyle(ChatFormatting.YELLOW));
		}

		return InputConstants.getKey(toggleKeyNameValue).getDisplayName();
	}

	private void addItemEntry() {
		List<String> parsed = AutoEjectConfig.parseItems(itemsInput.getValue());
		if (parsed.isEmpty()) {
			return;
		}

		String value = parsed.getFirst();
		if (!itemValues.contains(value)) {
			itemValues.add(value);
			refreshItemEntries();
		}

		itemsInput.setValue("");
	}

	private void removeSelectedItemEntry() {
		if (selectedItemIndex < 0) {
			return;
		}

		itemValues.remove(selectedItemIndex);
		refreshItemEntries();
	}

	private void resetItemsToDefaults() {
		itemValues.clear();
		itemValues.addAll(AutoEjectConfig.DEFAULT_ITEMS);
		refreshItemEntries();
	}

	private void refreshItemEntries() {
		selectedItemIndex = -1;
		for (Button button : itemEntryButtons) {
			scrollableWidgets.removeIf(entry -> entry.widget == button);
			removeWidget(button);
		}
		itemEntryButtons.clear();

		int rowY = layoutItemsTop;
		for (int i = 0; i < itemValues.size(); i++) {
			final int index = i;
			Button entryButton = addRenderableWidget(Button.builder(itemLabel(index), button -> selectItemEntry(index))
				.bounds(layoutFieldX, rowY, layoutFieldWidth, layoutControlHeight)
				.build());
			entryButton.setTooltip(Tooltip.create(Component.literal(itemValues.get(index))));
			addScrollableWidget(entryButton, rowY);
			itemEntryButtons.add(entryButton);
			rowY += layoutControlHeight + 2;
		}

		relayoutItemSection();
		removeItemButton.active = false;
	}

	private void relayoutItemSection() {
		int listHeight = Math.max(24, itemEntryButtons.size() * (layoutControlHeight + 2));
		int actionButtonsY = layoutItemsTop + listHeight + 6;
		int buttonWidth = (layoutFieldWidth - 5) / 2;

		removeItemButton.setWidth(buttonWidth);
		removeItemButton.setX(layoutFieldX);
		setScrollableBaseY(removeItemButton, actionButtonsY);

		resetItemsButton.setWidth(buttonWidth);
		resetItemsButton.setX(layoutFieldX + buttonWidth + 5);
		setScrollableBaseY(resetItemsButton, actionButtonsY);

		int contentBottom = actionButtonsY + layoutControlHeight;
		maxScroll = Math.max(0, contentBottom - scrollAreaBottom);
		scrollOffset = clampScroll(scrollOffset);
		applyScroll();
	}

	private void selectItemEntry(int index) {
		selectedItemIndex = index;
		for (int i = 0; i < itemEntryButtons.size(); i++) {
			itemEntryButtons.get(i).setMessage(itemLabel(i));
		}
		removeItemButton.active = true;
	}

	private Component itemLabel(int index) {
		String value = itemValues.get(index);
		if (index == selectedItemIndex) {
			return Component.literal("> " + value + " <").withStyle(ChatFormatting.YELLOW);
		}

		return Component.literal(value);
	}

	private int clampScroll(int value) {
		return Math.max(0, Math.min(maxScroll, value));
	}

	private void applyScroll() {
		for (ScrollableWidget widget : scrollableWidgets) {
			int y = widget.baseY - scrollOffset;
			boolean visible = y + widget.widget.getHeight() > scrollAreaTop && y < scrollAreaBottom;
			widget.widget.visible = visible;
			widget.widget.setY(y);
		}
	}

	private <T extends net.minecraft.client.gui.components.AbstractWidget> T addScrollableWidget(T widget, int baseY) {
		scrollableWidgets.add(new ScrollableWidget(widget, baseY));
		return widget;
	}

	private void setScrollableBaseY(net.minecraft.client.gui.components.AbstractWidget widget, int baseY) {
		for (ScrollableWidget entry : scrollableWidgets) {
			if (entry.widget == widget) {
				entry.baseY = baseY;
				return;
			}
		}
	}

	private static final class ScrollableWidget {
		private final net.minecraft.client.gui.components.AbstractWidget widget;
		private int baseY;

		private ScrollableWidget(net.minecraft.client.gui.components.AbstractWidget widget, int baseY) {
			this.widget = widget;
			this.baseY = baseY;
		}
	}
}
