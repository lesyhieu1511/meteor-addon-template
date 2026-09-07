package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoInvTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").description("Delay before moving a totem.").defaultValue(3).min(1).max(20).sliderMin(1).sliderMax(20).build());
    private final Setting<Boolean> moveFromHotbar = sgGeneral.add(new BoolSetting.Builder().name("move-from-hotbar").description("Also search hotbar slots.").defaultValue(true).build());
    private final Setting<Boolean> disableLogs = sgGeneral.add(new BoolSetting.Builder().name("disable-logs").description("Disable movement messages.").defaultValue(true).build());
    private final Setting<Boolean> openInv = sgGeneral.add(new BoolSetting.Builder().name("open-inv").description("Automatically open inventory after a totem pop.").defaultValue(false).build());
    private final Setting<Integer> invOpenDelay = sgGeneral.add(new IntSetting.Builder().name("inv-open-delay").description("Ticks before opening inventory.").defaultValue(2).min(1).max(10).sliderMin(1).sliderMax(10).visible(openInv::get).build());
    private final Setting<Integer> invCloseDelay = sgGeneral.add(new IntSetting.Builder().name("inv-close-delay").description("Ticks before closing auto-opened inventory.").defaultValue(8).min(5).max(20).sliderMin(5).sliderMax(20).visible(openInv::get).build());

    private boolean needsTotem, hadTotemInOffhand;
    private int delayTicks;
    private boolean shouldOpenInv, invAutoOpened;
    private int invOpenTicks, invCloseTicks;

    public AutoInvTotem() { super(AddonTemplate.CATEGORY, "auto-inv-totem", "Moves a replacement totem to the offhand when inventory is opened after a pop."); }

    @Override public void onActivate() { resetState(); }
    @Override public void onDeactivate() { resetInvAutoState(); }

    private void resetState() {
        if (mc.player == null) return;
        hadTotemInOffhand = hasTotemInOffhand();
        needsTotem = false; delayTicks = 0; resetInvAutoState();
    }
    private void resetInvAutoState() { shouldOpenInv = false; invOpenTicks = 0; invCloseTicks = 0; invAutoOpened = false; }

    @EventHandler private void onGameJoined(GameJoinedEvent event) { resetState(); }

    @EventHandler private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundEntityEventPacket packet && packet.getEventId() == 35 && mc.player != null && packet.getEntity(mc.level) == mc.player) {
            if (openInv.get() && mc.gui.screen() == null) { shouldOpenInv = true; invOpenTicks = invOpenDelay.get(); }
        }
    }

    @EventHandler private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.gameMode == null) return;
        handleAutoInventory();
        boolean currentlyHasTotem = hasTotemInOffhand();
        if (hadTotemInOffhand && !currentlyHasTotem) {
            needsTotem = true;
            if (mc.gui.screen() instanceof InventoryScreen) delayTicks = delay.get();
        }
        hadTotemInOffhand = currentlyHasTotem;
        if (currentlyHasTotem && needsTotem) { needsTotem = false; delayTicks = 0; }
    }

    private void handleAutoInventory() {
        if (shouldOpenInv && invOpenTicks > 0 && --invOpenTicks == 0 && mc.gui.screen() == null) {
            mc.gui.setScreen(new InventoryScreen(mc.player)); invAutoOpened = true; invCloseTicks = invCloseDelay.get(); shouldOpenInv = false;
        }
        if (invAutoOpened && invCloseTicks > 0 && --invCloseTicks == 0 && mc.gui.screen() instanceof InventoryScreen) {
            mc.gui.setScreen(null); invAutoOpened = false;
        }
        if (invAutoOpened && !(mc.gui.screen() instanceof InventoryScreen)) { invAutoOpened = false; invCloseTicks = 0; }
    }

    @EventHandler private void onOpenScreen(OpenScreenEvent event) { if (event.screen instanceof InventoryScreen && needsTotem) delayTicks = delay.get(); }

    @EventHandler private void onTickDelayed(TickEvent.Post event) { if (delayTicks > 0 && mc.player != null) { if (--delayTicks == 0) moveTotemToOffhand(); } }

    private void moveTotemToOffhand() {
        int totemSlot = findTotemSlot();
        if (totemSlot == -1) return;
        int containerSlot = totemSlot < 9 ? totemSlot + 36 : totemSlot;
        ItemStack offhand = mc.player.getOffhandItem();
        if (offhand.isEmpty()) {
            mc.gameMode.handleContainerInput(0, containerSlot, 40, ContainerInput.SWAP, mc.player);
        } else {
            mc.gameMode.handleContainerInput(0, containerSlot, 0, ContainerInput.PICKUP, mc.player);
            mc.gameMode.handleContainerInput(0, 45, 0, ContainerInput.PICKUP, mc.player);
            mc.gameMode.handleContainerInput(0, containerSlot, 0, ContainerInput.PICKUP, mc.player);
        }
        needsTotem = false;
    }

    private int findTotemSlot() {
        for (int i = 9; i < 36; i++) if (mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) return i;
        if (moveFromHotbar.get()) for (int i = 0; i < 9; i++) if (mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) return i;
        return -1;
    }
    private boolean hasTotemInOffhand() { return mc.player != null && mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING); }
}
