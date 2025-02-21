
package xwellp.saturn.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.RotationAxis;
import xwellp.saturn.Saturn;

import static xwellp.saturn.modules.SwingAnimation.AnimationType.Swipe;
import static xwellp.saturn.modules.SwingAnimation.AnimationType.Whirl;
import static xwellp.saturn.modules.SwingAnimation.PositionPreset.First;
import static xwellp.saturn.modules.SwingAnimation.PositionPreset.Second;

public class SwingAnimation extends Module {
    public static float animspeedMod = 1;
    public static AnimationType AnimationType;
    public static PositionPreset PositionPreset;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMainHand = settings.createGroup("Main Hand");
    private final SettingGroup sgOffHand = settings.createGroup("Off Hand");

    public final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("animation-speed")
            .description("Swing animation speed")
            .defaultValue(0.1)
            .min(0)
            .max(2)
            .sliderMax(2)
            .build()
    );

    public final Setting<AnimationType> animationType = sgGeneral.add(new EnumSetting.Builder<AnimationType>()
            .name("animation-type")
            .description("Choose animation type")
            .defaultValue(SwingAnimation.AnimationType.Swipe)
            .build()
    );

    public final Setting<PositionPreset> positionPreset = sgGeneral.add(new EnumSetting.Builder<PositionPreset>()
            .name("position-preset")
            .description("Position preset")
            .defaultValue(SwingAnimation.PositionPreset.First)
            .build()
    );

    // Настройки для основной руки
    public final Setting<Double> mainPosX = sgMainHand.add(new DoubleSetting.Builder()
            .name("main-position-x")
            .description("X position offset for main hand")
            .defaultValue(1.45)
            .min(-5.0)
            .max(5.0)
            .sliderMin(-5.0)
            .sliderMax(5.0)
            .build()
    );
    public final Setting<Double> mainPosY = sgMainHand.add(new DoubleSetting.Builder()
            .name("main-position-y")
            .description("Y position offset for main hand")
            .defaultValue(-0.6)
            .min(-5.0)
            .max(5.0)
            .sliderMin(-5.0)
            .sliderMax(5.0)
            .build()
    );
    public final Setting<Double> mainPosZ = sgMainHand.add(new DoubleSetting.Builder()
            .name("main-position-z")
            .description("Z position offset for main hand")
            .defaultValue(-1.5)
            .min(-5.0)
            .max(5.0)
            .sliderMin(-5.0)
            .sliderMax(5.0)
            .build()
    );
    public final Setting<Double> mainRotX = sgMainHand.add(new DoubleSetting.Builder()
            .name("main-rotation-x")
            .description("X rotation for main hand")
            .defaultValue(14.5)
            .min(-360)
            .max(360)
            .sliderMax(360)
            .build()
    );
    public final Setting<Double> mainRotY = sgMainHand.add(new DoubleSetting.Builder()
            .name("main-rotation-y")
            .description("Y rotation for main hand")
            .defaultValue(180)
            .min(-360)
            .max(360)
            .sliderMax(360)
            .build()
    );
    public final Setting<Double> mainRotZ = sgMainHand.add(new DoubleSetting.Builder()
            .name("main-rotation-z")
            .description("Z rotation for main hand")
            .defaultValue(0.0)
            .min(-360)
            .max(360)
            .sliderMax(360)
            .build()
    );

    // Настройки для дополнительной руки
    public final Setting<Double> offPosX = sgOffHand.add(new DoubleSetting.Builder()
            .name("off-position-x")
            .description("X position offset for off hand")
            .defaultValue(1.45)
            .min(-5.0)
            .max(5.0)
            .sliderMin(-5.0)
            .sliderMax(5.0)
            .build()
    );
    public final Setting<Double> offPosY = sgOffHand.add(new DoubleSetting.Builder()
            .name("off-position-y")
            .description("Y position offset for off hand")
            .defaultValue(-0.6)
            .min(-5.0)
            .max(5.0)
            .sliderMin(-5.0)
            .sliderMax(5.0)
            .build()
    );
    public final Setting<Double> offPosZ = sgOffHand.add(new DoubleSetting.Builder()
            .name("off-position-z")
            .description("Z position offset for off hand")
            .defaultValue(-1.5)
            .min(-5.0)
            .max(5.0)
            .sliderMin(-5.0)
            .sliderMax(5.0)
            .build()
    );
    public final Setting<Double> offRotX = sgOffHand.add(new DoubleSetting.Builder()
            .name("off-rotation-x")
            .description("X rotation for off hand")
            .defaultValue(14.5)
            .min(-360)
            .max(360)
            .sliderMax(360)
            .build()
    );
    public final Setting<Double> offRotY = sgOffHand.add(new DoubleSetting.Builder()
            .name("off-rotation-y")
            .description("Y rotation for off hand")
            .defaultValue(180)
            .min(-360)
            .max(360)
            .sliderMax(360)
            .build()
    );
    public final Setting<Double> offRotZ = sgOffHand.add(new DoubleSetting.Builder()
            .name("off-rotation-z")
            .description("Z rotation for off hand")
            .defaultValue(0.0)
            .min(-360)
            .max(360)
            .sliderMax(360)
            .build()
    );

    private final Setting<Boolean> usePreset = sgGeneral.add(new BoolSetting.Builder()
            .name("use-preset?")
            .description("Enable/Disable PositionPreset")
            .defaultValue(false)
            .build()
    );

    private float swingProgress = 0f;
    private boolean animating = false;
    private long lastUpdateTime = System.nanoTime();

    public SwingAnimation() {
        super(Saturn.CATEGORY, "swing-animation", "Custom swing animation");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        updateAnimationSpeed();
        applyPresetIfEnabled();
        updateSwingProgress();
    }

    private void updateAnimationSpeed() {
        if (animationType.get() == Swipe) {
            animspeedMod = 1.08F;
        } else if (animationType.get() == Whirl) {
            animspeedMod = 0.85F;
        }
    }

    private void applyPresetIfEnabled() {
        if (usePreset.get()) {
            if (positionPreset.get() == First) {
                mainPosX.set(1.45);
                mainPosY.set(-0.6);
                mainPosZ.set(-1.5);
                mainRotX.set(-10.0);
                mainRotY.set(0.0);
                mainRotZ.set(0.0);
            } else if (positionPreset.get() == Second) {
                mainPosX.set(1.9);
                mainPosY.set(-0.6);
                mainPosZ.set(-1.5);
                mainRotX.set(14.6);
                mainRotY.set(340.0);
                mainRotZ.set(100.0);
            }
        }
    }

    private void updateSwingProgress() {
        if (mc.player == null) return;

        long now = System.nanoTime();
        float deltaSeconds = (now - lastUpdateTime) / 1_000_000_000f;
        lastUpdateTime = now;

        if (mc.player.handSwinging && !animating) {
            animating = true;
            swingProgress = 0f;
        }

        if (animating) {
            swingProgress += (float) ((speed.get().floatValue() * 1.2 * animspeedMod) * deltaSeconds);
            if (swingProgress >= 1.0f) {
                swingProgress = 1.0f;
                animating = false;
            }
        } else {
            swingProgress = 0f;
        }
    }

    public float getSwingProgress() {
        return swingProgress;
    }

    public enum AnimationType {
        Swipe("Swipe"),
        Whirl("Whirl");

        private final String name;

        AnimationType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum PositionPreset {
        First("1"),
        Second("2");

        private final String name;

        PositionPreset(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
