package xwellp.saturn.modules;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;

import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import xwellp.saturn.Saturn;
import xwellp.saturn.utils.SaturnUtils;

import java.util.Set;
import java.util.function.Predicate;

public class FightHelper extends Module {
    private final SettingGroup sgFilter = settings.createGroup("Filter");
    private final SettingGroup sgAttack = settings.createGroup("Attack");
    private final SettingGroup sgVisual = settings.createGroup("Visual");
    private final SettingGroup sgVisibility = settings.createGroup("Visibility");
    private final SettingGroup sgAim = settings.createGroup("Aim");
    private final SettingGroup sgTiming = settings.createGroup("Timing");
    private final SettingGroup sgFovCircle = settings.createGroup("FOV Circle");

    private final Setting<Boolean> fovCircleEnabled = sgFovCircle.add(new BoolSetting.Builder()
            .name("fov-circle")
            .description("Renders the FOV circle.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyyaw = sgAim.add(new BoolSetting.Builder()
            .name("only-yaw")
            .description("Отключает повороты по оси Y")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> onlypitch = sgAim.add(new BoolSetting.Builder()
            .name("only-pitch")
            .description("Отключает повороты по оси X")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> yawmod = sgAim.add(new DoubleSetting.Builder()
            .name("yaw-mod")
            .description("Дополнительное изменение скорости по оси Y")
            .defaultValue(1)
            .min(0)
            .sliderMin(0)
            .sliderMax(2)
            .build()
    );

    private final Setting<Double> pitchmod = sgAim.add(new DoubleSetting.Builder()
            .name("pitch-mod")
            .description("Дополнительное изменение скорости по оси X")
            .defaultValue(1)
            .min(0)
            .sliderMin(0)
            .sliderMax(2)
            .build()
    );

    private final Setting<SettingColor> fovCircleColor = sgFovCircle.add(new ColorSetting.Builder()
            .name("color")
            .description("Color of the FOV circle.")
            .defaultValue(new SettingColor(255, 0, 0, 100))
            .build()
    );

    private final Setting<Set<EntityType<?>>> entities = sgFilter.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Specifies the entity types to target for attack.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .build()
    );
    private final Setting<Double> range = sgFilter.add(new DoubleSetting.Builder()
            .name("range")
            .description("Defines the maximum range for attacking a target entity.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(6)
            .build()
    );
    private final Setting<Boolean> ignoreBabies = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-babies")
            .description("Prevents attacking baby variants of mobs.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreNamed = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-named")
            .description("Prevents attacking named mobs.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> ignorePassive = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-passive")
            .description("Allows attacking passive mobs only if they target you.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreTamed = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-tamed")
            .description("Prevents attacking tamed mobs.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> ignoreFriends = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Prevents attacking players on your friends list.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreWalls = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-walls")
            .description("Allows attacking through walls.")
            .defaultValue(false)
            .build()
    );
    private final Setting<OnFallMode> onFallMode = sgAttack.add(new EnumSetting.Builder<OnFallMode>()
            .name("on-fall-mode")
            .description("Chooses an attack strategy when falling to maximize critical damage.")
            .defaultValue(OnFallMode.Value)
            .build()
    );

    private final Setting<Boolean> tpsSync = sgTiming.add(new BoolSetting.Builder()
            .name("TPS-sync")
            .description("Tries to sync attack delay with the server's TPS.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoSwitch = sgAttack.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Switches to your selected weapon when attacking the target.")
            .defaultValue(false)
            .build()
    );

    private final Setting<KillAura.Weapon> weapon = sgAttack.add(new EnumSetting.Builder<KillAura.Weapon>()
            .name("weapon")
            .description("Only attacks an entity when a specified weapon is in your hand.")
            .defaultValue(KillAura.Weapon.All)
            .build()
    );

    private final Setting<KillAura.ShieldMode> shieldMode = sgAttack.add(new EnumSetting.Builder<KillAura.ShieldMode>()
            .name("shield-mode")
            .description("Will try and use an axe to break target shields.")
            .defaultValue(KillAura.ShieldMode.Break)
            .visible(() -> autoSwitch.get() && weapon.get() != KillAura.Weapon.Axe)
            .build()
    );

    private final Setting<Integer> switchDelay = sgTiming.add(new IntSetting.Builder()
            .name("switch-delay")
            .description("How many ticks to wait before hitting an entity after switching hotbar slots.")
            .defaultValue(0)
            .min(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Double> onFallValue = sgAttack.add(new DoubleSetting.Builder()
            .name("on-fall-value")
            .description("Defines a specific value for attacking while falling.")
            .min(0)
            .defaultValue(0.25)
            .sliderMax(1)
            .visible(() -> onFallMode.get() == OnFallMode.Value)
            .build()
    );
    private final Setting<Double> onFallMinRandomValue = sgAttack.add(new DoubleSetting.Builder()
            .name("on-fall-min-random-value")
            .description("Specifies the minimum randomized value for attacking while falling.")
            .min(0)
            .defaultValue(0.2)
            .sliderMax(1)
            .visible(() -> onFallMode.get() == OnFallMode.RandomValue)
            .build()
    );
    private final Setting<Double> onFallMaxRandomValue = sgAttack.add(new DoubleSetting.Builder()
            .name("on-fall-max-random-value")
            .description("Specifies the maximum randomized value for attacking while falling.")
            .min(0)
            .defaultValue(0.4)
            .sliderMax(1)
            .visible(() -> onFallMode.get() == OnFallMode.RandomValue)
            .build()
    );
    private final Setting<HitSpeedMode> hitSpeedMode = sgAttack.add(new EnumSetting.Builder<HitSpeedMode>()
            .name("hit-speed-mode")
            .description("Selects a hit speed mode for attacking.")
            .defaultValue(HitSpeedMode.Value)
            .build()
    );
    private final Setting<Double> hitSpeedValue = sgAttack.add(new DoubleSetting.Builder()
            .name("hit-speed-value")
            .description("Defines a specific hit speed value for attacking.")
            .defaultValue(0)
            .sliderRange(-10, 10)
            .visible(() -> hitSpeedMode.get() == HitSpeedMode.Value)
            .build()
    );
    private final Setting<Double> hitSpeedMinRandomValue = sgAttack.add(new DoubleSetting.Builder()
            .name("hit-speed-min-random-value")
            .description("Specifies the minimum randomized hit speed value.")
            .defaultValue(0)
            .sliderRange(-10, 10)
            .visible(() -> hitSpeedMode.get() == HitSpeedMode.RandomValue)
            .build()
    );
    private final Setting<Double> hitSpeedMaxRandomValue = sgAttack.add(new DoubleSetting.Builder()
            .name("hit-speed-max-random-value")
            .description("Specifies the maximum randomized hit speed value.")
            .defaultValue(0)
            .sliderRange(-10, 10)
            .visible(() -> hitSpeedMode.get() == HitSpeedMode.RandomValue)
            .build()
    );
    private final Setting<Boolean> swingHand = sgVisual.add(new BoolSetting.Builder()
            .name("swing-hand")
            .description("Makes hand swing visible client-side.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SortPriority> priority = sgFilter.add(new EnumSetting.Builder<SortPriority>()
            .name("priority")
            .description("Sorting method to prioritize targets within range.")
            .defaultValue(SortPriority.ClosestAngle)
            .build()
    );
    private final Setting<Target> bodyTarget = sgAim.add(new EnumSetting.Builder<Target>()
            .name("aim-target")
            .description("Part of the target entity's body to aim at.")
            .defaultValue(Target.Head)
            .build()
    );

    public final Setting<Double> targetMovementPrediction = sgAim.add(new DoubleSetting.Builder()
            .name("target-movement-prediction")
            .description("Amount to predict the target's movement when aiming.")
            .min(0.0F)
            .sliderMax(20.0F)
            .defaultValue(0.0F)
            .build()
    );

    private final Setting<Boolean> instantAim = sgAim.add(new BoolSetting.Builder()
            .name("instant-aim")
            .description("Aim at the target entity instantly.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> syncSpeedWithCooldown = sgAim.add(new BoolSetting.Builder()
            .name("sync-speed-with-cooldown")
            .description("Synchronize aim speed with attack cooldown progress.")
            .defaultValue(false)
            .visible(() -> !instantAim.get())
            .build()
    );
    private final Setting<Double> speed = sgAim.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Speed at which to adjust aim.")
            .min(0)
            .defaultValue(1)
            .sliderRange(0.1, 10)
            .visible(() -> !instantAim.get())
            .build()
    );
    private final Setting<Boolean> useFovRange = sgVisibility.add(new BoolSetting.Builder()
            .name("use-fov-range")
            .description("Restrict aiming to entities within the specified FOV.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> fovRange = sgVisibility.add(new DoubleSetting.Builder()
            .name("fov-range")
            .description("Maximum Field of View (FOV) range for targeting entities.")
            .sliderRange(0, 180)
            .defaultValue(90)
            .visible(useFovRange::get)
            .build()
    );


    float randomOnFallFloat = 0;
    float randomHitSpeedFloat = 0;
    double getFovR = - 0;
    private static Entity currentTarget = null;

    public FightHelper() {
        super(Saturn.CATEGORY, "fight-helper", "AttackAura");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        currentTarget = getTarget();
        if (mc.player.isDead() || mc.world == null) return;

        OnFallMode currOnFallMode = onFallMode.get();
        if (currOnFallMode != OnFallMode.None) {
            float onFall = currOnFallMode == OnFallMode.Value ? onFallValue.get().floatValue() : randomOnFallFloat;
            if (!(mc.player.fallDistance > onFall)) return;
        }

        HitSpeedMode currHitSpeedMode = hitSpeedMode.get();
        float hitSpeed = currHitSpeedMode == HitSpeedMode.Value ? hitSpeedValue.get().floatValue() : randomHitSpeedFloat;
        if (currHitSpeedMode != HitSpeedMode.None && (mc.player.getAttackCooldownProgress(hitSpeed) * 17.0F) < 16)
            return;

        HitResult hitResult = SaturnUtils.getCrosshairTarget(mc.player, range.get(), ignoreWalls.get(), (e -> !e.isSpectator()
                && e.canHit()
                && entities.get().contains(e.getType())
                && !(ignoreBabies.get() && (e instanceof AnimalEntity && (((AnimalEntity) e).isBaby())))
                && !(ignoreNamed.get() && e.hasCustomName())
                && !(ignorePassive.get() && ((e instanceof EndermanEntity enderman && !enderman.isAngry()) || (e instanceof ZombifiedPiglinEntity piglin && !piglin.isAttacking()) || (e instanceof WolfEntity wolf && !wolf.isAttacking())))
                && !(ignoreTamed.get() && (e instanceof Tameable tameable && tameable.getOwnerUuid() != null && tameable.getOwnerUuid().equals(mc.player.getUuid())))
                && !(ignoreFriends.get() && (e instanceof PlayerEntity player && !Friends.get().shouldAttack(player)))
        ));

        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) return;
        Entity entity = ((EntityHitResult) hitResult).getEntity();

        LivingEntity livingEntity = (LivingEntity) entity;
        if (livingEntity.getHealth() > 0) {
            if (autoSwitch.get()) {
                Predicate<ItemStack> predicate = switch (weapon.get()) {
                    case Axe -> stack -> stack.getItem() instanceof AxeItem;
                    case Sword -> stack -> stack.getItem() instanceof SwordItem;
                    case Mace -> stack -> stack.getItem() instanceof MaceItem;
                    case Trident -> stack -> stack.getItem() instanceof TridentItem;
                    case All -> stack -> stack.getItem() instanceof AxeItem || stack.getItem() instanceof SwordItem || stack.getItem() instanceof MaceItem || stack.getItem() instanceof TridentItem;
                    default -> o -> true;
                };
                FindItemResult weaponResult = InvUtils.findInHotbar(predicate);

                if (shouldShieldBreak()) {
                    FindItemResult axeResult = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof AxeItem);
                    if (axeResult.found()) weaponResult = axeResult;
                }

                InvUtils.swap(weaponResult.slot(), false);
            }

            if (!itemInHand()) return;

            mc.interactionManager.attackEntity(mc.player, livingEntity);

            if (swingHand.get()) mc.player.swingHand(Hand.MAIN_HAND);

            if (currOnFallMode == OnFallMode.RandomValue) {
                float min = Math.min(onFallMinRandomValue.get().floatValue(), onFallMaxRandomValue.get().floatValue());
                float max = Math.max(onFallMinRandomValue.get().floatValue(), onFallMaxRandomValue.get().floatValue());
                float average = (float) Math.abs(onFallMaxRandomValue.get() / onFallMinRandomValue.get());
                randomOnFallFloat = min + mc.world.random.nextFloat() * (max - min);
            }

            if (currHitSpeedMode == HitSpeedMode.RandomValue) {
                float min = Math.min(hitSpeedMinRandomValue.get().floatValue(), hitSpeedMaxRandomValue.get().floatValue());
                float max = Math.max(hitSpeedMinRandomValue.get().floatValue(), hitSpeedMaxRandomValue.get().floatValue());
                randomHitSpeedFloat = min + mc.world.random.nextFloat() * (max - min);
            }
        }
    }

    private boolean shouldShieldBreak() {
        if (currentTarget instanceof PlayerEntity player) {
            return player.blockedByShield(mc.world.getDamageSources().playerAttack(mc.player)) && shieldMode.get() == KillAura.ShieldMode.Break;
        }
        return false;
    }

    private boolean itemInHand() {
        if (shouldShieldBreak()) return mc.player.getMainHandStack().getItem() instanceof AxeItem;

        return switch (weapon.get()) {
            case Axe -> mc.player.getMainHandStack().getItem() instanceof AxeItem;
            case Sword -> mc.player.getMainHandStack().getItem() instanceof SwordItem;
            case Mace -> mc.player.getMainHandStack().getItem() instanceof MaceItem;
            case Trident -> mc.player.getMainHandStack().getItem() instanceof TridentItem;
            case All -> mc.player.getMainHandStack().getItem() instanceof AxeItem || mc.player.getMainHandStack().getItem() instanceof SwordItem || mc.player.getMainHandStack().getItem() instanceof MaceItem || mc.player.getMainHandStack().getItem() instanceof TridentItem;
            default -> true;
        };
    }

    @EventHandler
    private void renderTick(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        Entity target = TargetUtils.get(e -> !e.equals(mc.player)
                && e.isAlive()
                && entities.get().contains(e.getType())
                && !(ignoreBabies.get() && (e instanceof LivingEntity entity && entity.isBaby()))
                && !(ignoreNamed.get() && e.hasCustomName())
                && !(ignorePassive.get() && ((e instanceof EndermanEntity enderman && !enderman.isAngry()) || (e instanceof ZombifiedPiglinEntity piglin && !piglin.isAttacking()) || (e instanceof WolfEntity wolf && !wolf.isAttacking())))
                && !(ignoreTamed.get() && (e instanceof Tameable tameable && tameable.getOwnerUuid() != null && tameable.getOwnerUuid().equals(mc.player.getUuid())))
                && !(ignoreFriends.get() && (e instanceof PlayerEntity player && !Friends.get().shouldAttack(player)))
                && PlayerUtils.isWithin(e, range.get())
                && (!useFovRange.get() || calculateFov(mc.player, e) <= fovRange.get())
                && (ignoreWalls.get() || PlayerUtils.canSeeEntity(e)), priority.get()
        );

        if (target == null) return;
        aim(mc.player, target);
    }


    public static Entity getCurrentTarget() {
        return currentTarget;
    }

    public static double getFovRange() {
        FightHelper instance = Modules.get().get(FightHelper.class);
        return instance != null ? instance.fovRange.get() : 90.0;
    }



    private float calculateFov(LivingEntity player, Entity target) {
        Vec3d lookDirection = player.getRotationVec(1.0F);
        Vec3d targetDirection = target.getPos().subtract(player.getPos()).normalize();

        return (float) Math.toDegrees(Math.acos(lookDirection.dotProduct(targetDirection)));
    }

    private void aim(LivingEntity player, Entity target) {
        float targetYaw = (float) Rotations.getYaw(target.getPos().add(target.getVelocity().multiply(targetMovementPrediction.get())));
        float targetPitch = (float) Rotations.getPitch(target, bodyTarget.get());

        float yawDifference = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        float pitchDifference = MathHelper.wrapDegrees(targetPitch - player.getPitch());

        if (instantAim.get()) {
            player.setYaw(targetYaw);
            player.setPitch(targetPitch);
        } else {
            float cooldownProgress = syncSpeedWithCooldown.get() ? mc.player.getAttackCooldownProgress(0) : 1;
            if (!onlypitch.get()) {
            player.setYaw((float) (player.getYaw() + yawDifference * cooldownProgress * (speed.get().floatValue() / 10) * yawmod.get()));
            }
            if (!onlyyaw.get()) {
                player.setPitch((float) (player.getPitch() + pitchDifference * cooldownProgress * (speed.get().floatValue() / 10) * yawmod.get()));
            }
        }

    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (fovCircleEnabled.get() || mc.options.getFov().getValue() == 0) return;

        Renderer2D.COLOR.begin();

        Window window = mc.getWindow();
        int width = window.getScaledWidth();
        int height = window.getScaledHeight();

        double fov = mc.options.getFov().getValue();
        double radius = (width / 2.0) * Math.tan(Math.toRadians(fovRange.get() / 2)) / Math.tan(Math.toRadians(fov / 2));

        Color circleColor = new Color(
                fovCircleColor.get().r,
                fovCircleColor.get().g,
                fovCircleColor.get().b,
                fovCircleColor.get().a
        );

        final int segments = 360;
        final double step = Math.PI * 2 / segments;
        double prevX = width + radius;
        double prevY = height;

        for (int i = 1; i <= segments; i++) {
            double angle = i * step;
            double x = width + Math.cos(angle) * radius;
            double y = height + Math.sin(angle) * radius;

            Renderer2D.COLOR.line(prevX, prevY, x, y, circleColor);

            prevX = x;
            prevY = y;
        }

        Renderer2D.COLOR.end();
    }

    public Entity getTarget() {
        Entity target = TargetUtils.get(e -> !e.equals(mc.player)
                && e.isAlive()
                && entities.get().contains(e.getType())
                && !(ignoreBabies.get() && (e instanceof LivingEntity entity && entity.isBaby()))
                && !(ignoreNamed.get() && e.hasCustomName())
                && !(ignorePassive.get() && ((e instanceof EndermanEntity enderman && !enderman.isAngry()) || (e instanceof ZombifiedPiglinEntity piglin && !piglin.isAttacking()) || (e instanceof WolfEntity wolf && !wolf.isAttacking())))
                && !(ignoreTamed.get() && (e instanceof Tameable tameable && tameable.getOwnerUuid() != null && tameable.getOwnerUuid().equals(mc.player.getUuid())))
                && !(ignoreFriends.get() && (e instanceof PlayerEntity player && !Friends.get().shouldAttack(player)))
                && PlayerUtils.isWithin(e, range.get())
                && (!useFovRange.get() || calculateFov(mc.player, e) <= fovRange.get())
                && (ignoreWalls.get() || PlayerUtils.canSeeEntity(e)), priority.get()
        );
        currentTarget = target;
        return target;
    }


    public enum OnFallMode {
        None,
        Value,
        RandomValue
    }

    public enum HitSpeedMode {
        None,
        Value,
        RandomValue
    }
}