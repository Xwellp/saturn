package xwellp.saturn.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import xwellp.saturn.Saturn;
import xwellp.saturn.modules.FightHelper;

public class FightMark extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("General");
    private final SettingGroup sgSize = settings.createGroup("Size");
    private final SettingGroup sgAnimation = settings.createGroup("Animation");
    private final SettingGroup sgAppearance = settings.createGroup("Appearance");

    public enum TextureType {
        Default("Default", "saturn:textures/icons/attackaura/rbox.png"),
        Circle("Circle", "saturn:textures/icons/attackaura/circle.png"),
        Square("Squares", "saturn:textures/icons/attackaura/romb.png"),
        Star("Star", "saturn:textures/icons/attackaura/star.png");

        private final String name;
        private final String texturePath;

        TextureType(String name, String texturePath) {
            this.name = name;
            this.texturePath = texturePath;
        }

        public Identifier getTexture() {
            return Identifier.of(texturePath);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Setting<TextureType> textureType = sgAppearance.add(new EnumSetting.Builder<TextureType>()
            .name("texture")
            .description("The texture to display above the target.")
            .defaultValue(TextureType.Default)
            .build()
    );

    private final Setting<Boolean> pulse = sgAnimation.add(new BoolSetting.Builder()
            .name("pulsation")
            .description("Pulsation animation")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> pulseStrength = sgSize.add(new DoubleSetting.Builder()
            .name("pulse-strength")
            .description("Pulsation strength")
            .defaultValue(1)
            .sliderMax(5)
            .build()
    );

    private final Setting<Double> outSize = sgSize.add(new DoubleSetting.Builder()
            .name("box-size")
            .description("Final size modificator.")
            .defaultValue(1)
            .sliderMax(3)
            .build()
    );

    private final Setting<Double> rotationSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotation-speed")
            .description("Rotation effect speed.")
            .defaultValue(8.0)
            .sliderMax(50.0)
            .build()
    );

    private final Setting<Double> maxBoxSize = sgSize.add(new DoubleSetting.Builder()
            .name("max-box-size")
            .description("Maximum size of the square when the entity is closest.")
            .defaultValue(20.0)
            .sliderMax(50.0)
            .build()
    );

    private final Setting<Double> minBoxSize = sgSize.add(new DoubleSetting.Builder()
            .name("min-box-size")
            .description("Minimum size of the square when the entity is farthest.")
            .defaultValue(5.0)
            .sliderMax(20.0)
            .build()
    );

    private final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("max-distance")
            .description("Maximum distance at which the square will be visible.")
            .defaultValue(10.0)
            .sliderMax(50.0)
            .build()
    );

    private final Setting<SettingColor> Color = sgAppearance.add(new ColorSetting.Builder()
            .name("color")
            .description("Color")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
    );

    private float rotationAngle = 0f;
    private Entity lastTarget = null;

    private double currentX = 0;
    private double dadasda = 0;
    private double currentY = 0;
    private double currentSize = 0;
    private float animationProgress = 0f;

    public FightMark() {
        super(Saturn.CATEGORY, "rotiender-box", "Renders a spinning texture above the targeted entity.");
    }

    public float[] getColorForShader() {
        SettingColor color = Color.get();
        return new float[] {
                color.r / 255.0f, // Red
                color.g / 255.0f, // Green
                color.b / 255.0f, // Blue
                color.a / 255.0f  // Alpha
        };
    }

    public float getRed() {
        return Color.get().r / 255.0f;
    }

    public float getGreen() {
        return Color.get().g / 255.0f;
    }

    public float getBlue() {
        return Color.get().b / 255.0f;
    }

    public float getAlpha() {
        return Color.get().a / 255.0f;
    }



    private void drawRotatingTexture(DrawContext context, double x, double y, double size, float angle, float alpha) {
        MatrixStack matrices = context.getMatrices();


        if (pulse.get()) {
            double pulse = 1.0 + (pulseStrength.get() / 10) * MathHelper.sin((float) ((System.currentTimeMillis() % 1000) / 1000.0 * Math.PI * 2));
            size *= pulse;
        }

        matrices.push();
        matrices.translate(x, y, 0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotation(angle));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(getRed(), getGreen(), getBlue(), getAlpha() * alpha);

        float scale = (float) ((size / 512.0) * outSize.get());
        matrices.scale(scale, scale, 1f);

        context.drawTexture(textureType.get().getTexture(), -256, -256, 0, 0, 512, 512, 512, 512);
        context.setShaderColor(1f, 1f, 1f, 1f);

        RenderSystem.disableBlend();

        matrices.pop();
    }


    private Vector3d worldToScreen(Vector3d worldPos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return null;

        Vector3d screenPos = new Vector3d(worldPos);
        return NametagUtils.to2D(screenPos, 1.0) ? screenPos : null;
    }

    // Функция экспоненциального ease-in-out для t от 0 до 1
    private float easeInOutExpo(float t) {
        if (t == 0f) return 0f;
        if (t == 1f) return 1f;
        if (t < 0.5f) {
            return (float) (Math.pow(2, 20 * t - 10)) / 2f;
        } else {
            return (float) (2 - Math.pow(2, -20 * t + 10)) / 2f;
        }
    }

    private long lastUpdateTime = System.nanoTime();

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!isActive()) return;

        long now = System.nanoTime();
        float deltaSeconds = (now - lastUpdateTime) / 1_000_000_000f;
        lastUpdateTime = now;

        float animSpeed = 1.0f;
        if (lastTarget != null) {
            animationProgress = Math.min(animationProgress + animSpeed * deltaSeconds, 1.0f);
        } else {
            animationProgress = Math.max(animationProgress - animSpeed * deltaSeconds, 0.0f);
        }

        if (animationProgress <= 0f) return;

        if (lastTarget != null) {
            Vector3d targetScreenPos = worldToScreen(new Vector3d(
                    lastTarget.getX(),
                    lastTarget.getY() + (lastTarget.getHeight() / 1.7),
                    lastTarget.getZ()
            ));
            if (targetScreenPos != null) {
                double targetX = targetScreenPos.x;
                double targetY = targetScreenPos.y;
                double lerpFactor = 0.07;
                currentX = MathHelper.lerp(lerpFactor, currentX, targetX);
                currentY = MathHelper.lerp(lerpFactor, currentY, targetY);
            }

            double distance = MinecraftClient.getInstance().player.squaredDistanceTo(lastTarget);
            double computedSize = MathHelper.lerp(
                    (maxDistance.get() - Math.sqrt(distance)) / maxDistance.get(),
                    minBoxSize.get(),
                    maxBoxSize.get()
            );
            double lerpFactorSize = 0.05;
            currentSize = MathHelper.lerp(lerpFactorSize, currentSize, computedSize);
        }

        float easedProgress = easeInOutExpo(animationProgress);

        double effectiveSize = currentSize * easedProgress;
        float effectiveAlpha = easedProgress;

        rotationAngle += rotationSpeed.get().floatValue() * deltaSeconds;
        if (rotationAngle > 360f) rotationAngle -= 360f;

        drawRotatingTexture(
                event.drawContext,
                currentX,
                currentY,
                effectiveSize,
                rotationAngle,
                effectiveAlpha
        );
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {

        FightHelper aura = Modules.get().get(FightHelper.class);
        if (aura != null && aura.isActive()) {
            lastTarget = FightHelper.getCurrentTarget();
        } else {
            lastTarget = null;
        }
    }
}
