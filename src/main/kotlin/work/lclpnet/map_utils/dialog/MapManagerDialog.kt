package work.lclpnet.map_utils.dialog

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.dialog.AfterAction
import net.minecraft.dialog.DialogActionButtonData
import net.minecraft.dialog.DialogButtonData
import net.minecraft.dialog.DialogCommonData
import net.minecraft.dialog.action.DynamicCustomDialogAction
import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.body.PlainMessageDialogBody
import net.minecraft.dialog.type.MultiActionDialog
import net.minecraft.dialog.type.NoticeDialog
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting.*
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import org.slf4j.Logger
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.util.PositionRotation
import work.lclpnet.kibu.hook.world.ServerWorldHooks
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.kibu.translate.text.FormatWrapper.styled
import work.lclpnet.kibu.world.KibuWorlds
import work.lclpnet.map_api.data.DataManager
import work.lclpnet.map_api.data.type.PositionData
import work.lclpnet.map_api.hook.MapDataLoadedCallback
import work.lclpnet.map_api.mixin.MinecraftServerAccessor
import work.lclpnet.map_utils.MOD_ID
import work.lclpnet.map_utils.identifier
import work.lclpnet.map_utils.util.MapArchiver
import xyz.nucleoid.fantasy.RuntimeWorldHandle
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.io.path.isDirectory

val MAP_EXPORT_DIR: Path = FabricLoader.getInstance().configDir
    .resolve(MOD_ID)
    .resolve("map_export")

class MapManagerDialog(
    val translations: Translations,
    val dataManager: DataManager,
    val mapArchiver: MapArchiver,
    val logger: Logger
) {

    private var pendingTeleport = mutableMapOf<Identifier, MutableSet<UUID>>()
    private var pendingUnload = mutableMapOf<Identifier, MutableSet<UUID>>()

    fun init(hooks: HookRegistrar) {
        hooks.registerHook(MapDataLoadedCallback.HOOK, MapDataLoadedCallback { world, _ ->
            val pending = pendingTeleport.remove(world.registryKey.value) ?: return@MapDataLoadedCallback

            for (uuid in pending) {
                val player = world.server.playerManager.getPlayer(uuid) ?: continue
                teleportTo(player, world)
            }
        })

        hooks.registerHook(ServerWorldHooks.UNLOAD, ServerWorldEvents.Unload { server, world ->
            val pending = pendingUnload.remove(world.registryKey.value) ?: return@Unload

            for (uuid in pending) {
                val player = server.playerManager.getPlayer(uuid) ?: continue

                open(player)
            }
        })
    }

    fun open(player: ServerPlayerEntity) {
        val server = player.server ?: return

        CompletableFuture.supplyAsync { getAvailableWorlds(server) }.whenComplete { worldIds, err ->
            if (err != null) {
                logger.error("Failed to get available world ids", err)
                return@whenComplete
            }

            showMapList(worldIds.toList(), player)
        }
    }

    private fun showMapList(worldIds: List<Identifier>, player: ServerPlayerEntity) {
        val server = player.server ?: return

        val (loaded, notLoaded) = worldIds.sortedBy { it.toString() }.partition {
            server.getWorld(RegistryKey.of(RegistryKeys.WORLD, it)) != null
        }

        val buttons = mutableListOf<DialogActionButtonData>()
        val worldManager = KibuWorlds.getInstance().getWorldManager(server)

        for (worldId in loaded) {
            val world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, worldId))

            val nbt = NbtCompound()
            nbt.putString("id", worldId.toString())

            buttons.add(
                DialogActionButtonData(
                    DialogButtonData(
                        Text.literal(worldId.toString()).formatted(GREEN),
                        200
                    ),
                    Optional.of(DynamicCustomDialogAction(TELEPORT_ID, Optional.of(nbt)))
                )
            )

            val saveText = Text.literal("❌")

            if (worldManager.getRuntimeWorldHandle(world).isPresent) {
                saveText.withColor(0xfc6a6a)
            } else {
                saveText.formatted(DARK_GRAY)
            }

            buttons.add(
                DialogActionButtonData(
                    DialogButtonData(saveText, 20),
                    Optional.of(DynamicCustomDialogAction(CLOSE_ID, Optional.of(nbt)))
                )
            )

            buttons.add(
                DialogActionButtonData(
                    DialogButtonData(Text.literal("\uD83D\uDCBE").formatted(AQUA), 20),
                    Optional.of(DynamicCustomDialogAction(EXPORT_ID, Optional.of(nbt)))
                )
            )
        }

        for (worldId in notLoaded) {
            val nbt = NbtCompound()
            nbt.putString("id", worldId.toString())

            buttons.add(
                DialogActionButtonData(
                    DialogButtonData(
                        Text.literal(worldId.toString()).formatted(GRAY),
                        200
                    ),
                    Optional.of(DynamicCustomDialogAction(TELEPORT_ID, Optional.of(nbt)))
                )
            )

            buttons.add(
                DialogActionButtonData(
                    DialogButtonData(Text.literal("\uD83D\uDCE5"), 20),
                    Optional.of(DynamicCustomDialogAction(LOAD_ID, Optional.of(nbt)))
                )
            )

            buttons.add(
                DialogActionButtonData(
                    DialogButtonData(Text.literal("\uD83D\uDCBE").formatted(AQUA), 20),
                    Optional.of(DynamicCustomDialogAction(EXPORT_ID, Optional.of(nbt)))
                )
            )
        }

        val body = mutableListOf<DialogBody>()

        if (buttons.isEmpty()) {
            body.add(
                PlainMessageDialogBody(
                    translations.translateText("map_manager.no_maps")
                        .formatted(RED).translateFor(player),
                    200
                )
            )
        }

        val title = translations.translateText("manage_maps").translateFor(player)
        val commonData = DialogCommonData(
            title, Optional.empty(), true, true, AfterAction.CLOSE, body, listOf()
        )

        val dialog = if (buttons.isNotEmpty()) {
            MultiActionDialog(
                commonData,
                buttons,
                Optional.of(
                    DialogActionButtonData(
                        DialogButtonData(Text.translatable("gui.cancel"), 150),
                        Optional.empty()
                    )
                ),
                3
            )
        } else NoticeDialog(
            commonData,
            DialogActionButtonData(
                DialogButtonData(Text.translatable("gui.cancel"), 150),
                Optional.empty()
            )
        )

        player.openDialog(RegistryEntry.of(dialog))
    }

    fun getAvailableWorlds(server: MinecraftServer): Set<Identifier> {
        val session = (server as MinecraftServerAccessor).session
        val root = session.getDirectory(WorldSavePath.ROOT).resolve("dimensions").normalize()

        if (!root.isDirectory()) {
            return emptySet()
        }

        val worldIds = mutableSetOf<Identifier>()

        server.worlds.forEach { worldIds.add(it.registryKey.value) }

        Files.walkFileTree(
            root,
            EnumSet.noneOf(FileVisitOption::class.java),
            16,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (!Files.isRegularFile(dir.resolve("level.dat"))) {
                        return FileVisitResult.CONTINUE
                    }

                    val rel: Path = root.relativize(dir)

                    if (rel.nameCount < 2) {
                        return FileVisitResult.CONTINUE
                    }

                    val it = rel.iterator()
                    val namespace = it.next().toString()
                    val pathBuilder = StringBuilder()

                    while (it.hasNext()) {
                        if (!pathBuilder.isEmpty()) pathBuilder.append('/')

                        pathBuilder.append(it.next())
                    }

                    val id = Identifier.of(namespace, pathBuilder.toString())
                    worldIds.add(id)

                    return FileVisitResult.SKIP_SUBTREE
                }
            })

        return worldIds
    }

    fun teleport(player: ServerPlayerEntity, nbt: NbtCompound) {
        val id = nbt.getString("id", null) ?: return
        val worldId = Identifier.tryParse(id) ?: return
        val server = player.server ?: return

        val world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, worldId))

        if (world != null) {
            teleportTo(player, world)
            return
        }

        pendingTeleport.computeIfAbsent(worldId) { mutableSetOf() }.add(player.uuid)

        loadWorld(player, worldId) ?: return

        translations.translateText(
            "map_manager.loaded",
            styled(worldId, YELLOW)
        ).formatted(GREEN).sendTo(player)
    }

    fun loadWorld(player: ServerPlayerEntity, worldId: Identifier): RuntimeWorldHandle? {
        val worldManager = KibuWorlds.getInstance().getWorldManager(player.server)
        val handle = worldManager.openPersistentWorld(worldId).orElse(null)

        if (handle == null) {
            translations.translateText(
                "map_manager.load_failed",
                styled(worldId, YELLOW)
            ).formatted(RED).sendTo(player)
        }

        return handle
    }

    fun teleportTo(player: ServerPlayerEntity, world: ServerWorld) {
        val worldData = dataManager.getWorldData(world)

        val spawn = worldData.get("spawn", PositionData) ?: findSpawnPos(player, world)

        player.teleport(world, spawn.x, spawn.y, spawn.z, emptySet(), spawn.yaw, spawn.pitch, true)

        translations.translateText(
            "map_manager.teleported",
            styled(world.registryKey.value, YELLOW)
        ).formatted(GREEN).sendTo(player)
    }

    private fun findSpawnPos(player: ServerPlayerEntity, world: ServerWorld): PositionRotation {
        val spawnPos = player.getWorldSpawnPos(world, world.spawnPos)

        return PositionRotation(
            spawnPos.x + 0.5, spawnPos.y + 0.5, spawnPos.z + 0.5, world.spawnAngle, 0f
        )
    }

    fun closeWorld(player: ServerPlayerEntity, nbt: NbtCompound) {
        val id = nbt.getString("id", null) ?: return
        val worldId = Identifier.tryParse(id) ?: return
        val server = player.server ?: return
        val world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, worldId)) ?: return

        val worldManager = KibuWorlds.getInstance().getWorldManager(server)
        val handle = worldManager.getRuntimeWorldHandle(world).orElse(null)

        if (handle == null) {
            open(player)
            return
        }

        world.save(null, true, false)

        pendingUnload.computeIfAbsent(worldId) { mutableSetOf() }.add(player.uuid)

        handle.unload()

        translations.translateText(
            "map_manager.closed",
            styled(world.registryKey.value, YELLOW)
        ).formatted(GREEN).sendTo(player)
    }

    fun exportWorld(player: ServerPlayerEntity, nbt: NbtCompound) {
        val id = nbt.getString("id", null) ?: return
        val worldId = Identifier.tryParse(id) ?: return
        val server = player.server ?: return

        val session = (server as MinecraftServerAccessor).session
        val worldDir = session.getWorldDirectory(RegistryKey.of(RegistryKeys.WORLD, worldId))

        CompletableFuture.runAsync { exportMap(worldDir, worldId) }.whenComplete { _, err ->
            if (err != null) {
                logger.error("Failed to export map archive", err)

                translations.translateText(
                    "map_manager.export_failed",
                    styled(worldId, YELLOW)
                ).formatted(RED).sendTo(player)
            } else {
                translations.translateText(
                    "map_manager.exported",
                    styled(worldId, YELLOW)
                ).formatted(GREEN).sendTo(player)
            }
        }

        open(player)
    }

    fun exportMap(worldDir: Path, worldId: Identifier) {
        if (!worldDir.isDirectory()) return

        val outputFile = MAP_EXPORT_DIR.resolve(worldId.namespace).resolve("${worldId.path}.tar.xz")

        mapArchiver.createArchive(worldDir, outputFile)
    }

    fun loadWorld(player: ServerPlayerEntity, nbt: NbtCompound) {
        val id = nbt.getString("id", null) ?: return
        val worldId = Identifier.tryParse(id) ?: return

        loadWorld(player, worldId) ?: return

        translations.translateText(
            "map_manager.loaded_tp",
            styled(worldId, YELLOW),
            Text.literal("[")
                .append(translations.translateText("map_manager.teleport").translateFor(player))
                .append("]")
                .styled { it
                    .withFormatting(AQUA)
                    .withHoverEvent(HoverEvent.ShowText(translations.translateText("map_manager.click_tp")
                        .formatted(AQUA)
                        .translateFor(player)))
                    .withClickEvent(ClickEvent.Custom(TELEPORT_ID, Optional.of(nbt)))
                }
        ).formatted(GREEN).sendTo(player)

        open(player)
    }

    companion object {
        val ID = identifier("map_manager")
        val TELEPORT_ID = identifier("map_manager_teleport")
        val CLOSE_ID = identifier("map_manager_close")
        val EXPORT_ID = identifier("map_manager_export")
        val LOAD_ID = identifier("map_manager_load")
    }
}