package com.example.addon.mixin;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ThreadLocalRandom;

@Mixin(KillAura.class)
public abstract class KillAuraRandomDelayMixin {
    @Unique private Setting<Integer> glazed$randomDelay;
    @Unique private int glazed$delay;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void glazed$init(CallbackInfo ci) {
        KillAura aura = (KillAura) (Object) this;
        glazed$randomDelay = aura.settings.getDefaultGroup().add(new IntSetting.Builder()
            .name("random-delay")
            .description("Adds a random 0 to N tick delay between attacks.")
            .defaultValue(0)
            .min(0)
            .max(10)
            .sliderRange(0, 10)
            .build());
    }

    @Inject(method = "delayCheck", at = @At("HEAD"), cancellable = true)
    private void glazed$delayCheck(CallbackInfoReturnable<Boolean> cir) {
        if (glazed$delay > 0) {
            glazed$delay--;
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void glazed$afterAttack(Entity target, CallbackInfo ci) {
        if (glazed$randomDelay != null) {
            int max = glazed$randomDelay.get();
            glazed$delay = max == 0 ? 0 : ThreadLocalRandom.current().nextInt(max + 1);
        }
    }
}
