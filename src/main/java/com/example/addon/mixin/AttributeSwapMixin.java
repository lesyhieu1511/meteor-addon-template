package com.example.addon.mixin;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.combat.AttributeSwap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.EndCrystal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ThreadLocalRandom;

@Mixin(AttributeSwap.class)
public abstract class AttributeSwapMixin {
    @Shadow private void performSwap(Entity target) {}

    @Unique private Setting<Integer> glazed$randomDelay;
    @Unique private int glazed$delay;
    @Unique private Entity glazed$pendingTarget;

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

    @Inject(method = "performSwap", at = @At("HEAD"), cancellable = true)
    private void glazed$performSwap(Entity target, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (target instanceof EndCrystal && mc.player != null && !mc.player.hasEffect(MobEffects.WEAKNESS)) return;
        if (glazed$randomDelay == null || glazed$randomDelay.get() <= 0 || glazed$pendingTarget != null) return;

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
        performSwap(target);
    }
}
