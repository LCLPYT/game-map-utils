package work.lclpnet.map_utils.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.map_utils.hook.VirtualEntityInteractCallback;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(
            method = "onPlayerInteractEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayerEntity;setSneaking(Z)V",
                    shift = At.Shift.AFTER
            )
    )
    private void gameMapUtils$onInteractWithEntity(PlayerInteractEntityC2SPacket packet, CallbackInfo ci, @Local Entity entity) {
        if (entity != null) return;

        int entityId = ((PlayerInteractEntityC2SPacketAccessor) packet).getEntityId();

        var handler = VirtualEntityInteractCallback.HOOK.invoker().provideHandler(player, entityId);

        if (handler != null) {
            packet.handle(handler);
        }
    }
}
