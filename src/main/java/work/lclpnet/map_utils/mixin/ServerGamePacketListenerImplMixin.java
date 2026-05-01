package work.lclpnet.map_utils.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.map_utils.hook.VirtualEntityAttackCallback;
import work.lclpnet.map_utils.hook.VirtualEntityInteractCallback;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(
            method = "handleAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V",
                    shift = At.Shift.AFTER
            )
    )
    private void gameMapUtils$onAttackEntity(ServerboundAttackPacket packet, CallbackInfo ci, @Local(name = "target") Entity target) {
        if (target != null) return;

        int entityId = packet.entityId();

        VirtualEntityAttackCallback.HOOK.invoker().onAttack(player, entityId);
    }

    @Inject(
            method = "handleInteract",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;setShiftKeyDown(Z)V",
                    shift = At.Shift.AFTER
            )
    )
    private void gameMapUtils$onInteractWithEntity(ServerboundInteractPacket packet, CallbackInfo ci, @Local(name = "target") Entity target) {
        if (target != null) return;

        int entityId = packet.entityId();

        VirtualEntityInteractCallback.HOOK.invoker().onInteract(player, entityId, packet.hand(), packet.location());
    }
}
