package com.example.addon.mixin;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.combat.AttributeSwap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(AttributeSwap.class)
public abstract class AttributeSwapMixin {
    @Shadow private void performSwap(Entity target) {}
    @Shadow private Entity getTargetEntity() { return null; }

    @Unique private Setting<Integer> glazed$randomDelay;
    @Unique private Setting<Boolean> glazed$avoidSpearWhileHolding;
    @Unique private Setting<List<Item>> glazed$noSpearWhileHolding;
    @Unique private Setting<Boolean> glazed$avoidSpearInMeleeRange;
    @Unique private int glazed$delay;
    @Unique private Entity glazed$pendingTarget;
    @Unique private boolean glazed$executingPending;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void glazed$init(CallbackInfo ci) {
        AttributeSwap swap = (AttributeSwap) (Object) this;
        glazed$randomDelay = swap.settings.getDefaultGroup().add(new IntSetting.Builder()
            .name("random-delay")
            .description("Adds a random 0 to N tick delay before an attribute swap.")
            .defaultValue(0)
            .min(0).max(10)
            .sliderRange(0, 10)
            .build());

        glazed$avoidSpearWhileHolding = swap.settings.getDefaultGroup().add(new BoolSetting.Builder()
            .name("avoid-spear-while-holding")
            .description("Enable the selected-item filter that prevents switching to a spear.")
            .defaultValue(true)
            .build());

        glazed$noSpearWhileHolding = swap.settings.getDefaultGroup().add(new ItemListSetting.Builder()
            .name("no-spear-while-holding")
            .description("Do not swap to a spear while either hand holds one of these items.")
            .defaultValue()
            .visible(glazed$avoidSpearWhileHolding::get)
            .build());

        glazed$avoidSpearInMeleeRange = swap.settings.getDefaultGroup().add(new BoolSetting.Builder()
            .name("avoid-spear-in-melee-range")
            .description("Do not swap to a spear while the targeted entity is within normal melee reach.")
            .defaultValue(true)
            .build());
    }

    @Inject(method = "getSmartSpearSlot", at = @At("HEAD"), cancellable = true)
    private void glazed$skipSpearForHeldItems(boolean requireLunge, CallbackInfoReturnable<Integer> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (glazed$avoidSpearWhileHolding != null
            && glazed$avoidSpearWhileHolding.get()
            && glazed$noSpearWhileHolding != null) {
            List<Item> excluded = glazed$noSpearWhileHolding.get();
            if (!excluded.isEmpty()) {
                Item mainHand = mc.player.getMainHandItem().getItem();
                Item offHand = mc.player.getOffhandItem().getItem();

                if (excluded.contains(mainHand) || excluded.contains(offHand)) {
                    cir.setReturnValue(-1);
                    return;
                }
            }
        }

        if (glazed$avoidSpearInMeleeRange != null && glazed$avoidSpearInMeleeRange.get()) {
            Entity target = getTargetEntity();
            if (target != null && mc.player.distanceTo(target) <= mc.player.entityInteractionRange() + 0.5) {
                cir.setReturnValue(-1);
            }
        }
    }

    @Inject(method = "performSwap", at = @At("HEAD"), cancellable = true)
    private void glazed$performSwap(Entity target, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (target instanceof EndCrystal && mc.player != null && !mc.player.hasEffect(MobEffects.WEAKNESS)) {
            ci.cancel();
            return;
        }
        if (glazed$executingPending || glazed$randomDelay == null || glazed$randomDelay.get() <= 0 || glazed$pendingTarget != null) return;
        int max = glazed$randomDelay.get();
        glazed$pendingTarget = target;
        glazed$delay = ThreadLocalRandom.current().nextInt(max + 1);
        ci.cancel();
    }

    @Inject(method = "onTick", at = @At("HEAD"))
    private void glazed$onTick(TickEvent.Post event, CallbackInfo ci) {
        if (glazed$pendingTarget == null) return;
        if (glazed$delay > 0) {
            glazed$delay--;
            return;
        }
        Entity target = glazed$pendingTarget;
        glazed$pendingTarget = null;
        glazed$executingPending = true;
        try {
            performSwap(target);
        } finally {
            glazed$executingPending = false;
        }
    }
}