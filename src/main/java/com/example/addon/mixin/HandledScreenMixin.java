package com.example.addon.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {
    @Shadow
    public abstract Slot getHoveredSlot();

    @Unique
    public Slot addon$getFocusedSlot() {
        return getHoveredSlot();
    }
}
