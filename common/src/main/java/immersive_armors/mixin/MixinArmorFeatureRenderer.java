package immersive_armors.mixin;

import immersive_armors.Main;
import immersive_armors.item.ExtendedArmorItem;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ArmorFeatureRenderer.class)
public abstract class MixinArmorFeatureRenderer<T extends LivingEntity, M extends BipedEntityModel<T>, A extends BipedEntityModel<T>> extends FeatureRenderer<T, M> {
    public MixinArmorFeatureRenderer(FeatureRendererContext<T, M> context) {
        super(context);
    }

    float tickDelta;
    ItemStack equippedStack;
    T entity;

    @ModifyVariable(method = "renderArmor", at = @At("STORE"), ordinal = 0)
    private ItemStack fetchItemStack(ItemStack itemStack) {
        this.equippedStack = itemStack;
        return itemStack;
    }

    @Inject(method = "renderArmorParts", at = @At("HEAD"), cancellable = true)
    void renderArmorParts(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ArmorItem item, boolean usesSecondLayer, A model, boolean legs, float red, float green, float blue, String overlay, CallbackInfo ci) {
        if (!Main.FORGE && equippedStack.getItem() == item && item instanceof ExtendedArmorItem armorItem) {
            renderPieces(matrices, vertexConsumers, light, armorItem);
            ci.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T entity, float f, float g, float tickDelta, float j, float k, float l, CallbackInfo ci) {
        this.tickDelta = tickDelta;
        this.entity = entity;

        // Forge removed renderArmorParts calls
        if (Main.FORGE) {
            renderPieces(matrixStack, vertexConsumerProvider, i, EquipmentSlot.HEAD);
            renderPieces(matrixStack, vertexConsumerProvider, i, EquipmentSlot.CHEST);
            renderPieces(matrixStack, vertexConsumerProvider, i, EquipmentSlot.LEGS);
            renderPieces(matrixStack, vertexConsumerProvider, i, EquipmentSlot.FEET);
            ci.cancel();
        }
    }

    private void renderPieces(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, EquipmentSlot armorSlot) {
        equippedStack = entity.getEquippedStack(armorSlot);
        if (equippedStack.getItem() instanceof ExtendedArmorItem armorItem) {
            renderPieces(matrices, vertexConsumers, light, armorItem);
        }
    }

    private void renderPieces(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ExtendedArmorItem item) {
        item.getMaterial().getPieces(item.getSlotType()).forEach(piece -> {
            //noinspection unchecked
            piece.render(matrices, vertexConsumers, light, entity, equippedStack, tickDelta, item.getSlotType(), (BipedEntityModel<LivingEntity>)getContextModel());
        });
    }
}
