package com.example.addon.mixin;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.combat.AttributeSwap;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ThreadLocalRandom;

@Mixin(AttributeSwap.class)
public abstract class AttributeSwapMixin {
    @Unique private Setting<Integer> glazed$randomDelay;
    @Unique private int glazed$delay;
    @Unique private int glazed$pendingSlot = -1;
    @Unique private boolean glazed$executing;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void glazed$init(CallbackInfo ci) {
        AttributeSwap swap = (AttributeSwap) (Object) this;
        glazed$randomDelay = swap.settings.getDefaultGroup().add(new IntSetting.Builder()
            .name("random-delay")
            .description("Adds a random 0 to N tick delay before an attribute swap.")
            .defaultValue(0)
            .min(0)
            .max(10)
            .sliderRange(0, 10)
            .build());
    }

    @Inject(method = "doSwap", at = @At("HEAD"), cancellable = true)
    private void glazed$doSwap(int slotIndex, CallbackInfo ci) {
        if (glazed$executing || glazed$randomDelay == null) return;

        int max = glazed$randomDelay.get();
        if (max <= 0) return;

        glazed$pendingSlot = slotIndex;
        glazed$delay = ThreadLocalRandom.current().nextInt(max + 1);
        glazed$executing = false;
        ci.cancel();
    }

    @Inject(method = "onTick", at = @At("TAIL"))
    private void glazed$onTick(TickEvent.Post event, CallbackInfo ci) {
        if (glazed$pendingSlot < 0) return;
        if (glazed$delay > 0) {
            glazed$delay--;
            return;
        }

        int slot = glazed$pendingSlot;
        glazed$pendingSlot = -1;
        glazed$executing = true;
        ((AttributeSwap) (Object) this).getClass();
        InvUtils.swap(slot, true);
        glazed$executing = false;
    }
}
