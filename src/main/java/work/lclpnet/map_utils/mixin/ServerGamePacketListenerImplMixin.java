package work.lclpnet.map_utils.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.map_utils.hook.VirtualEntityInteractCallback;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(
            method = "handleInteract",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;setShiftKeyDown(Z)V",
                    shift = At.Shift.AFTER
            )
    )
    private void gameMapUtils$onInteractWithEntity(ServerboundInteractPacket packet, CallbackInfo ci, @Local Entity entity) {
        if (entity != null) return;

        int entityId = ((ServerboundInteractPacketAccessor) packet).getEntityId();

        var handler = VirtualEntityInteractCallback.HOOK.invoker().provideHandler(player, entityId);

        if (handler != null) {
            packet.dispatch(handler);
        }
    }
}
