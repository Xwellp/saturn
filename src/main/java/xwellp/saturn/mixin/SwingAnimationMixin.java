package xwellp.saturn.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xwellp.saturn.modules.SwingAnimation;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(HeldItemRenderer.class)
public class SwingAnimationMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void replaceSwingAnimation(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgressParam, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        SwingAnimation module = Modules.get().get(SwingAnimation.class);

        if (module == null || !module.isActive() || player == null || !player.equals(mc.player)) {
            return;
        }

        if (item.isEmpty()) {
            return;
        }

        ci.cancel();
        matrices.push();

        if (hand == Hand.MAIN_HAND && player.getMainArm() == Arm.RIGHT || hand == Hand.OFF_HAND && player.getMainArm() == Arm.LEFT) {
            applyMainHandTransformations(matrices, module);
        } else {
            applyOffHandTransformations(matrices, module);
        }

        applyAnimation(matrices, module, hand, player.getMainArm());

        HeldItemRenderer renderer = (HeldItemRenderer) (Object) this;
        renderer.renderItem(player, item, ModelTransformationMode.FIRST_PERSON_RIGHT_HAND, false, matrices, vertexConsumers, light);

        matrices.pop();
    }

    private void applyMainHandTransformations(MatrixStack matrices, SwingAnimation module) {
        matrices.translate(module.mainPosX.get(), module.mainPosY.get(), module.mainPosZ.get());
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(module.mainRotX.get().floatValue()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(module.mainRotY.get().floatValue()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(module.mainRotZ.get().floatValue()));
    }

    private void applyOffHandTransformations(MatrixStack matrices, SwingAnimation module) {
        matrices.translate(module.offPosX.get(), module.offPosY.get(), module.offPosZ.get());
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(module.offRotX.get().floatValue()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(module.offRotY.get().floatValue()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(module.offRotZ.get().floatValue()));
    }

    private void applyAnimation(MatrixStack matrices, SwingAnimation module, Hand hand, Arm mainArm) {
        boolean isMainHand = (hand == Hand.MAIN_HAND && mainArm == Arm.RIGHT) || (hand == Hand.OFF_HAND && mainArm == Arm.LEFT);

        if (isMainHand) {
            float anim = (float) Math.sin(module.getSwingProgress() * Math.PI);
            switch (module.animationType.get()) {
                case Swipe -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) (anim * (-module.strength.get()*12))));
                case Whirl -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) (anim * (-module.strength.get()*12))));
                case Third -> matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) (anim * (-module.strength.get()*12))));
            }
        }
    }
}