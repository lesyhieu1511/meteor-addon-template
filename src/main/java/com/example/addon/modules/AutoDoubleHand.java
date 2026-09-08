package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.item.Items;

public class AutoDoubleHand extends Module {
    private boolean wasHoldingTotem = true;
    private boolean pendingSwap;

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
        if (wasHoldingTotem && !holdingNow) pendingSwap = true;
        wasHoldingTotem = holdingNow;

        if (!pendingSwap || mc.gui.screen() != null) return;

        int slot = findHotbarTotem();
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

    private void resetState() {
        wasHoldingTotem = mc.player != null && mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        pendingSwap = false;
    }
}
