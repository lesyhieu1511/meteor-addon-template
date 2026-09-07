package com.example.addon.mixin;

import com.example.addon.modules.GlazedFreecam;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public class CameraMixin {
    @Shadow private boolean detached;

    @Inject(method = "update", at = @At("HEAD"))
    private void addon$stepFreecam(DeltaTracker deltaTracker, CallbackInfo ci) {
        GlazedFreecam freecam = Modules.get().get(GlazedFreecam.class);
        if (freecam != null && freecam.isActive()) freecam.onGameRender();
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void addon$showBody(DeltaTracker deltaTracker, CallbackInfo ci) {
        GlazedFreecam freecam = Modules.get().get(GlazedFreecam.class);
        if (freecam != null && freecam.isActive()) detached = true;
    }

    @ModifyArgs(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void addon$setPos(Args args) {
        GlazedFreecam freecam = Modules.get().get(GlazedFreecam.class);
        if (freecam == null || !freecam.isActive()) return;
        args.set(0, freecam.getInterpolatedX(0.0f));
        args.set(1, freecam.getInterpolatedY(0.0f));
        args.set(2, freecam.getInterpolatedZ(0.0f));
    }

    @ModifyArgs(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
    private void addon$setRotation(Args args) {
        GlazedFreecam freecam = Modules.get().get(GlazedFreecam.class);
        if (freecam == null || !freecam.isActive()) return;
        args.set(0, (float) freecam.getInterpolatedYaw(0.0f));
        args.set(1, (float) freecam.getInterpolatedPitch(0.0f));
    }

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void addon$freecamNoClip(float desiredDistance, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        GlazedFreecam freecam = Modules.get().get(GlazedFreecam.class);
        if (freecam != null && freecam.isActive()) cir.setReturnValue(desiredDistance);
    }
}
