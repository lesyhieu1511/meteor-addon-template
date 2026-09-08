package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

public class AutoDoubleHand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks to wait after the offhand totem is lost before changing the hotbar slot.")
        .defaultValue(0)
        .min(0).max(10)
        .sliderMin(0).sliderMax(10)
        .build());

    private final Setting<Boolean> refillFromInventory = sgGeneral.add(new BoolSetting.Builder()
        .name("refill-from-inventory")
        .description("Move a replacement totem from the inventory into the selected hotbar slot when needed.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> refillSlot = sgGeneral.add(new IntSetting.Builder()
        .name("refill-slot")
        .description("Hotbar slot used for the replacement totem (1-9).")
        .defaultValue(1)
        .min(1).max(9)
        .sliderMin(1).sliderMax(9)
        .build());

    private boolean wasHoldingTotem = true;
    private boolean pendingSwap;
    private int delayTicks;

    public AutoDoubleHand() {
        super(AddonTemplate.CATEGORY, "auto-double-hand", "After an offhand totem pops, switches to a hotbar totem.");
    }

    @Override
    public void onActivate() {
        resetState();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.gameMode == null) return;

        if (mc.player.isDeadOrDying()) {
            resetState();
            return;
        }

        boolean holdingNow = mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);

        if (wasHoldingTotem && !holdingNow) {
            pendingSwap = true;
            delayTicks = delay.get();
        }

        wasHoldingTotem = holdingNow;

        if (!pendingSwap || mc.gui.screen() != null) return;

        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        int slot = findHotbarTotem();
        if (slot == -1 && refillFromInventory.get()) {
            slot = refillHotbarWithTotem();
        }

        if (slot == -1) {
            pendingSwap = false;
            return;
        }

        if (mc.player.getInventory().getSelectedSlot() == slot) {
            pendingSwap = false;
            return;
        }

        mc.player.getInventory().setSelectedSlot(slot);
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        pendingSwap = false;
    }

    private int findHotbarTotem() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) return i;
        }
        return -1;
    }

    private int findInventoryTotem() {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) return i;
        }
        return -1;
    }

    private int refillHotbarWithTotem() {
        if (mc.gui.screen() != null) return -1;

        int sourceSlot = findInventoryTotem();
        if (sourceSlot == -1) return -1;

        int targetHotbar = refillSlot.get() - 1;
        if (mc.player.getInventory().getItem(targetHotbar).is(Items.TOTEM_OF_UNDYING)) return targetHotbar;

        // Player inventory is container 0 when no GUI is open.
        // SWAP is used to exchange the inventory slot with the chosen hotbar slot.
        mc.gameMode.handleContainerInput(
            0, sourceSlot, targetHotbar, ContainerInput.SWAP, mc.player
        );

        return targetHotbar;
    }

    private void resetState() {
        wasHoldingTotem = mc.player != null
            && mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        pendingSwap = false;
        delayTicks = 0;
    }
}
