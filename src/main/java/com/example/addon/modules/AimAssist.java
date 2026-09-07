package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class AimAssist extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> notifications = sgGeneral.add(new BoolSetting.Builder()
        .name("notifications")
        .description("Show chat feedback.")
        .defaultValue(true)
        .build());

    private final SettingGroup sgSpeed = settings.createGroup("Aim Speed");

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to aim at.")
        .defaultValue(Set.of(EntityType.PLAYER))
        .build());

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range at which an entity can be targeted.")
        .defaultValue(5.0)
        .min(0.0)
        .sliderRange(0.0, 10.0)
        .build());

    private final Setting<Double> fov = sgGeneral.add(new DoubleSetting.Builder()
        .name("fov")
        .description("Will only aim entities in the FOV.")
        .defaultValue(360.0)
        .min(0.0)
        .max(360.0)
        .sliderRange(0.0, 360.0)
        .build());

    private final Setting<Boolean> ignoreWalls = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-walls")
        .description("Whether or not to ignore aiming through walls.")
        .defaultValue(false)
        .build());

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("priority")
        .description("How to filter targets within range.")
        .defaultValue(SortPriority.LowestHealth)
        .build());

    private final Setting<Target> bodyTarget = sgGeneral.add(new EnumSetting.Builder<Target>()
        .name("aim-target")
        .description("Which part of the entity to aim at.")
        .defaultValue(Target.Body)
        .build());

    private final Setting<Boolean> instant = sgSpeed.add(new BoolSetting.Builder()
        .name("instant-look")
        .description("Instantly looks at the entity.")
        .defaultValue(false)
        .build());

    private final Setting<Double> speed = sgSpeed.add(new DoubleSetting.Builder()
        .name("speed")
        .description("How fast to aim at the entity.")
        .defaultValue(5.0)
        .min(0.0)
        .sliderRange(0.0, 20.0)
        .visible(() -> !instant.get())
        .build());

    private Entity target;

    public AimAssist() {
        super(AddonTemplate.CATEGORY, "aim-assist", "Automatically aims at selected entities.");
    }

    @Override
    public void onActivate() {
        target = null;
    }

    @Override
    public void onDeactivate() {
        target = null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        target = TargetUtils.get(entity -> {
            if (!entity.isAlive()) return false;
            if (!PlayerUtils.isWithin(entity, range.get())) return false;
            if (!ignoreWalls.get() && !PlayerUtils.canSeeEntity(entity)) return false;
            if (entity == mc.player || !entities.get().contains(entity.getType())) return false;
            if (entity instanceof Player player && !Friends.get().shouldAttack(player)) return false;
            return isInFov(entity, fov.get());
        }, priority.get());
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (target != null && mc.player != null) {
            aim(target, (float) event.tickDelta, instant.get());
        }
    }

    private void aim(Entity target, float delta, boolean instant) {
        Vec3 pos = target.getPosition(delta);
        Vec3 targetPos = pos;

        switch (bodyTarget.get()) {
            case Head -> targetPos = targetPos.add(0, target.getEyeHeight(target.getPose()), 0);
            case Body -> targetPos = targetPos.add(0, target.getEyeHeight(target.getPose()) / 2.0, 0);
            default -> { }
        }

        double deltaX = targetPos.x - mc.player.getX();
        double deltaZ = targetPos.z - mc.player.getZ();
        double deltaY = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));

        double angle = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;

        if (instant) {
            mc.player.setYRot((float) angle);
        } else {
            double deltaAngle = Mth.wrapDegrees(angle - mc.player.getYRot());
            double toRotate = Math.min(Math.abs(deltaAngle), speed.get() * delta);
            toRotate = Math.copySign(toRotate, deltaAngle);
            mc.player.setYRot(mc.player.getYRot() + (float) toRotate);
        }

        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        angle = -Math.toDegrees(Math.atan2(deltaY, horizontal));

        if (instant) {
            mc.player.setXRot((float) angle);
        } else {
            double deltaAngle = Mth.wrapDegrees(angle - mc.player.getXRot());
            double toRotate = Math.min(Math.abs(deltaAngle), speed.get() * delta);
            toRotate = Math.copySign(toRotate, deltaAngle);
            mc.player.setXRot(mc.player.getXRot() + (float) toRotate);
        }
    }

    private boolean isInFov(Entity entity, double fov) {
        if (fov >= 360.0) return true;

        Vec3 entityPos = entity.position();
        Vec3 playerPos = mc.player.getEyePosition();
        Vec3 direction = entityPos.subtract(playerPos).normalize();
        double yaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0;
        double pitch = -Math.toDegrees(Math.asin(direction.y));
        double yawDiff = Mth.wrapDegrees(yaw - mc.player.getYRot());
        double pitchDiff = Mth.wrapDegrees(pitch - mc.player.getXRot());

        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff) <= fov / 2.0;
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}
