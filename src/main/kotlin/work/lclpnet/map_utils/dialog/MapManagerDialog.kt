package work.lclpnet.map_utils.dialog

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting.*
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.dialog.*
import net.minecraft.server.dialog.action.CustomAll
import net.minecraft.server.dialog.body.DialogBody
import net.minecraft.server.dialog.body.PlainMessage
import net.minecraft.server.dialog.input.TextInput
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.LevelResource
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
            val pending = pendingTeleport.remove(world.dimension().identifier()) ?: return@MapDataLoadedCallback

            for (uuid in pending) {
                val player = world.server?.playerList?.getPlayer(uuid) ?: continue
                teleportTo(player, world)
            }
        })

        hooks.registerHook(ServerWorldHooks.UNLOAD, ServerWorldEvents.Unload { server, world ->
            val pending = pendingUnload.remove(world.dimension().identifier()) ?: return@Unload

            for (uuid in pending) {
                val player = server.playerList.getPlayer(uuid) ?: continue

                open(player, CompoundTag())
            }
        })
    }

    fun open(player: ServerPlayer, nbt: CompoundTag) {
        val server = player.level().server ?: return

        CompletableFuture.supplyAsync { getAvailableWorlds(server) }.whenComplete { worldIds, err ->
            if (err != null) {
                logger.error("Failed to get available world ids", err)
                return@whenComplete
            }

            showMapList(worldIds.toList(), player, nbt)
        }
    }

    private fun showMapList(worldIds: List<Identifier>, player: ServerPlayer, inputNbt: CompoundTag) {
        val server = player.level().server ?: return
        val search = inputNbt.getStringOr("search", "")

        val filtered = applySearch(worldIds, search)

        val (loaded, notLoaded) = filtered.partition {
            server.getLevel(ResourceKey.create(Registries.DIMENSION, it.first)) != null
        }

        val buttons = mutableListOf<ActionButton>()
        val worldManager = KibuWorlds.getInstance().getWorldManager(server)

        for ((worldId, matches) in loaded) {
            val world = server.getLevel(ResourceKey.create(Registries.DIMENSION, worldId))

            val nbt = CompoundTag()
            nbt.putString("id", worldId.toString())

            val label = markMatches(worldId.toString(), matches, search.length)
                .withStyle(GREEN)

            buttons.add(
                ActionButton(
                    CommonButtonData(
                        label,
                        200
                    ),
                    Optional.of(CustomAll(TELEPORT_ID, Optional.of(nbt)))
                )
            )

            val saveText = Component.literal("❌")

            if (worldManager.getRuntimeWorldHandle(world).isPresent) {
                saveText.withColor(0xfc6a6a)
            } else {
                saveText.withStyle(DARK_GRAY)
            }

            buttons.add(
                ActionButton(
                    CommonButtonData(saveText, 20),
                    Optional.of(CustomAll(CLOSE_ID, Optional.of(nbt)))
                )
            )

            buttons.add(
                ActionButton(
                    CommonButtonData(Component.literal("\uD83D\uDCBE").withStyle(AQUA), 20),
                    Optional.of(CustomAll(EXPORT_ID, Optional.of(nbt)))
                )
            )
        }

        for ((worldId, matches) in notLoaded) {
            val nbt = CompoundTag()
            nbt.putString("id", worldId.toString())

            val label = markMatches(worldId.toString(), matches, search.length)
                .withStyle(GRAY)

            buttons.add(
                ActionButton(
                    CommonButtonData(
                        label,
                        200
                    ),
                    Optional.of(CustomAll(TELEPORT_ID, Optional.of(nbt)))
                )
            )

            buttons.add(
                ActionButton(
                    CommonButtonData(Component.literal("\uD83D\uDCE5"), 20),
                    Optional.of(CustomAll(LOAD_ID, Optional.of(nbt)))
                )
            )

            buttons.add(
                ActionButton(
                    CommonButtonData(Component.literal("\uD83D\uDCBE").withStyle(AQUA), 20),
                    Optional.of(CustomAll(EXPORT_ID, Optional.of(nbt)))
                )
            )
        }

        val body = mutableListOf<DialogBody>()
        val inputs = mutableListOf<Input>()

        if (buttons.isEmpty()) {
            body.add(
                PlainMessage(
                    translations.translateText("map_manager.no_maps")
                        .formatted(RED).translateFor(player),
                    200
                )
            )
        }

        buttons.addFirst(
            ActionButton(
            CommonButtonData(translations.translateText("search").translateFor(player), 200),
            Optional.of(CustomAll(ID, Optional.empty()))
        ))

        buttons.add(1, ActionButton(
            CommonButtonData(Component.literal("\uD83D\uDD0D"), 20),
            Optional.of(CustomAll(ID, Optional.empty()))
        ))

        buttons.add(2, ActionButton(
            CommonButtonData(Component.literal("\uD83D\uDD0D"), 20),
            Optional.of(CustomAll(ID, Optional.empty()))
        ))

        inputs.add(
            Input("search", TextInput(
            200,
            translations.translateText("map_manager.search").translateFor(player),
            true,
            search,
            128,
            Optional.empty()
        )))

        val title = translations.translateText("manage_maps").translateFor(player)
        val commonData = CommonDialogData(
            title, Optional.empty(), true, false, DialogAction.NONE, body, inputs
        )

        val dialog = if (buttons.isNotEmpty()) {
            MultiActionDialog(
                commonData,
                buttons,
                Optional.of(
                    ActionButton(
                        CommonButtonData(Component.translatable("gui.cancel"), 150),
                        Optional.empty()
                    )
                ),
                3
            )
        } else NoticeDialog(
            commonData,
            ActionButton(
                CommonButtonData(Component.translatable("gui.cancel"), 150),
                Optional.empty()
            )
        )

        player.openDialog(Holder.direct(dialog))
    }

    fun markMatches(input: String, indexes: List<Int>, matchLength: Int): MutableComponent {
        val root = Component.empty()

        var current = 0
        val sortedIndexes = indexes.sorted()

        for (i in sortedIndexes) {
            if (i >= input.length) break

            val pre = input.substring(current, i)

            if (pre.isNotEmpty()) {
                root.append(pre)
            }

            val end = (i + matchLength).coerceAtMost(input.length)
            val match = input.substring(i, end)

            root.append(Component.literal(match).withStyle(YELLOW))

            current = end
        }

        if (current < input.length) {
            root.append(input.substring(current))
        }

        return root
    }

    private fun applySearch(worldIds: List<Identifier>, query: String): List<Pair<Identifier, List<Int>>> {
        if (query.isBlank()) {
            return worldIds.sortedBy { it.toString() }.map { it to emptyList() }
        }

        return worldIds
            .map {
                val matches = mutableListOf<Int>()

                val str = it.toString()
                var index = 0

                do {
                    index = str.indexOf(query, index)

                    if (index == -1) break

                    matches.add(index)

                    index += query.length
                } while (true)

                it to matches
            }
            .filter { it.second.isNotEmpty() }
            .sortedBy { it.first.toString() }
    }

    fun getAvailableWorlds(server: MinecraftServer): Set<Identifier> {
        val session = (server as MinecraftServerAccessor).storageSource
        val root = session.getLevelPath(LevelResource.ROOT).resolve("dimensions").normalize()

        val worldIds = mutableSetOf<Identifier>()

        server.allLevels.forEach { worldIds.add(it.dimension().identifier()) }

        if (!root.isDirectory()) {
            return worldIds
        }

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

                    val id = Identifier.fromNamespaceAndPath(namespace, pathBuilder.toString())
                    worldIds.add(id)

                    return FileVisitResult.SKIP_SUBTREE
                }
            })

        return worldIds
    }

    fun teleport(player: ServerPlayer, nbt: CompoundTag) {
        val id = nbt.getString("id").orElse(null) ?: return
        val worldId = Identifier.tryParse(id) ?: return
        val server = player.level().server ?: return

        val world = server.getLevel(ResourceKey.create(Registries.DIMENSION, worldId))

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

    fun loadWorld(player: ServerPlayer, worldId: Identifier): RuntimeWorldHandle? {
        val worldManager = KibuWorlds.getInstance().getWorldManager(player.level().server)
        val handle = worldManager.openPersistentWorld(worldId).orElse(null)

        if (handle == null) {
            translations.translateText(
                "map_manager.load_failed",
                styled(worldId, YELLOW)
            ).formatted(RED).sendTo(player)
        }

        return handle
    }

    fun teleportTo(player: ServerPlayer, world: ServerLevel) {
        val worldData = dataManager.getWorldData(world)

        val spawn = worldData.get("spawn", PositionData) ?: findSpawnPos(player, world)

        player.teleportTo(world, spawn.x(), spawn.y(), spawn.z(), emptySet(), spawn.yaw, spawn.pitch, true)

        translations.translateText(
            "map_manager.teleported",
            styled(world.dimension().identifier(), YELLOW)
        ).formatted(GREEN).sendTo(player)
    }

    private fun findSpawnPos(player: ServerPlayer, world: ServerLevel): PositionRotation {
        val spawnPos = player.adjustSpawnLocation(world, world.respawnData.pos())

        return PositionRotation(
            spawnPos.x + 0.5, spawnPos.y + 0.5, spawnPos.z + 0.5, world.respawnData.yaw, world.respawnData.pitch
        )
    }

    fun closeWorld(player: ServerPlayer, nbt: CompoundTag) {
        val id = nbt.getString("id").orElse(null) ?: return
        val worldId = Identifier.tryParse(id) ?: return
        val server = player.level().server ?: return
        val world = server.getLevel(ResourceKey.create(Registries.DIMENSION, worldId)) ?: return

        val worldManager = KibuWorlds.getInstance().getWorldManager(server)
        val handle = worldManager.getRuntimeWorldHandle(world).orElse(null)

        if (handle == null) {
            open(player, nbt)
            return
        }

        world.save(null, true, false)

        pendingUnload.computeIfAbsent(worldId) { mutableSetOf() }.add(player.uuid)

        handle.unload()

        translations.translateText(
            "map_manager.closed",
            styled(world.dimension().identifier(), YELLOW)
        ).formatted(GREEN).sendTo(player)
    }

    fun exportWorld(player: ServerPlayer, nbt: CompoundTag) {
        val id = nbt.getString("id").orElse(null) ?: return
        val worldId = Identifier.tryParse(id) ?: return
        val server = player.level().server ?: return

        val session = (server as MinecraftServerAccessor).storageSource
        val worldDir = session.getDimensionPath(ResourceKey.create(Registries.DIMENSION, worldId))

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

        open(player, nbt)
    }

    fun exportMap(worldDir: Path, worldId: Identifier) {
        if (!worldDir.isDirectory()) return

        val outputFile = MAP_EXPORT_DIR.resolve(worldId.namespace).resolve("${worldId.path}.tar.xz")

        mapArchiver.createArchive(worldDir, outputFile)
    }

    fun loadWorld(player: ServerPlayer, nbt: CompoundTag) {
        val id = nbt.getString("id").orElse(null) ?: return
        val worldId = Identifier.tryParse(id) ?: return

        loadWorld(player, worldId) ?: return

        translations.translateText(
            "map_manager.loaded_tp",
            styled(worldId, YELLOW),
            Component.literal("[")
                .append(translations.translateText("map_manager.teleport").translateFor(player))
                .append("]")
                .withStyle { it
                    .applyFormat(AQUA)
                    .withHoverEvent(
                        HoverEvent.ShowText(translations.translateText("map_manager.click_tp")
                        .formatted(AQUA)
                        .translateFor(player)))
                    .withClickEvent(ClickEvent.Custom(TELEPORT_ID, Optional.of(nbt)))
                }
        ).formatted(GREEN).sendTo(player)

        open(player, nbt)
    }

    companion object {
        val ID = identifier("map_manager")
        val TELEPORT_ID = identifier("map_manager_teleport")
        val CLOSE_ID = identifier("map_manager_close")
        val EXPORT_ID = identifier("map_manager_export")
        val LOAD_ID = identifier("map_manager_load")
    }
}