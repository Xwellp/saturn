//package xwellp.saturn.modules;
//
//import com.mojang.blaze3d.systems.RenderSystem;
//import meteordevelopment.meteorclient.events.render.Render3DEvent;
//import meteordevelopment.meteorclient.events.world.TickEvent;
//import meteordevelopment.meteorclient.systems.modules.Module;
//import meteordevelopment.meteorclient.systems.modules.Categories;
//import meteordevelopment.orbit.EventHandler;
//import net.minecraft.client.MinecraftClient;
//import net.minecraft.client.render.*;
//import net.minecraft.client.util.math.MatrixStack;
//import net.minecraft.util.Identifier;
//import net.minecraft.util.math.Vec3d;
//import org.joml.Matrix4f;
//
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//
//public class JumpCircles extends Module {
//    private boolean wasOnGround = true;
//    private final List<CircleEffect> circles = new ArrayList<>();
//    private static final Identifier CIRCLE_TEXTURE = Identifier.of("yourmodid", "textures/circle.png");
//    private final MinecraftClient mc = MinecraftClient.getInstance();
//
//    public JumpCircles() {
//        super(Categories.Render, "jump-circles", "Создаёт круг при прыжке.");
//    }
//
//    @EventHandler
//    private void onTick(TickEvent.Post event) {
//        if (mc.player == null) return;
//
//        if (wasOnGround && !mc.player.isOnGround() && mc.player.getVelocity().y > 0) {
//            spawnCircle(mc.player.getPos());
//        }
//
//        wasOnGround = mc.player.isOnGround();
//    }
//
//    private void spawnCircle(Vec3d pos) {
//        circles.add(new CircleEffect(pos, 0, 3.0f)); // Начальный радиус 0, максимальный 3 блока
//    }
//
//    @EventHandler
//    private void onRender(Render3DEvent event) {
//        if (mc.player == null || circles.isEmpty()) return;
//
//        MatrixStack matrices = event.matrices;
//        Iterator<CircleEffect> iterator = circles.iterator();
//
//        while (iterator.hasNext()) {
//            CircleEffect circle = iterator.next();
//            circle.update();
//
//            if (circle.isFinished()) {
//                iterator.remove();
//            } else {
//                circle.render(matrices, event.tickDelta);
//            }
//        }
//    }
//
//    private class CircleEffect {
//        private final Vec3d position;
//        private float radius;
//        private final float maxRadius;
//        private float alpha;
//
//        public CircleEffect(Vec3d position, float startRadius, float maxRadius) {
//            this.position = position;
//            this.radius = startRadius;
//            this.maxRadius = maxRadius;
//            this.alpha = 1.0f;
//        }
//
//        public void update() {
//            radius += 0.1f; // Увеличение радиуса каждый тик
//            alpha -= 0.01f; // Плавное исчезновение
//        }
//
//        public boolean isFinished() {
//            return alpha <= 0;
//        }
//
//        public void render(MatrixStack matrices, float tickDelta) {
//            if (alpha <= 0) return;
//
//            Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
//            double x = position.x - cameraPos.x;
//            double y = position.y - cameraPos.y + 0.01; // Немного выше уровня земли
//            double z = position.z - cameraPos.z;
//
//            RenderSystem.enableBlend();
//            RenderSystem.defaultBlendFunc();
//            RenderSystem.setShader(GameRenderer::getPositionTexShader);
//            RenderSystem.setShaderTexture(0, CIRCLE_TEXTURE);
//            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
//
//            matrices.push();
//            matrices.translate(x, y, z);
//            matrices.multiply(mc.gameRenderer.getCamera().getRotation());
//            matrices.scale(radius, -1.0f, radius);
//
//            Matrix4f matrix = matrices.peek().getPositionMatrix();
//            Tessellator tessellator = Tessellator.getInstance();
//            BufferBuilder buffer = tessellator.getBuffer();
//
//            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
//            buffer.vertex(matrix, -1.0f, 0.0f, -1.0f).texture(0.0f, 0.0f).next();
//            buffer.vertex(matrix, -1.0f, 0.0f, 1.0f).texture(0.0f, 1.0f).next();
//            buffer.vertex(matrix, 1.0f, 0.0f, 1.0f).texture(1.0f, 1.0f).next();
//            buffer.vertex(matrix, 1.0f, 0.0f, -1.0f).texture(1.0f, 0.0f).next();
//            tessellator.draw();
//
//            matrices.pop();
//            RenderSystem.disableBlend();
//        }
//    }
//}
