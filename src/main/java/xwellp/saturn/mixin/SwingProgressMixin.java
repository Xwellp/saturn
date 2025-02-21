package xwellp.saturn.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xwellp.saturn.modules.SwingAnimation;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(LivingEntity.class)
public abstract class SwingProgressMixin {

    @Inject(method = "getHandSwingProgress", at = @At("HEAD"), cancellable = true)
    private void overrideSwingProgress(float tickDelta, CallbackInfoReturnable<Float> cir) {
        SwingAnimation module = Modules.get().get(SwingAnimation.class);
        if (module != null && module.isActive() && mc.player != null && mc.player.equals((LivingEntity)(Object)this)) {
            ItemStack mainHandItem = mc.player.getStackInHand(Hand.MAIN_HAND);
            ItemStack offHandItem = mc.player.getStackInHand(Hand.OFF_HAND);

            if (mainHandItem.isEmpty() && offHandItem.isEmpty()) return;
            cir.setReturnValue(0.0f);
        }
    }
}
