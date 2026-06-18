package work.lclpnet.map_utils.dialog

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting.*
import net.minecraft.core.Holder
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.server.dialog.*
import net.minecraft.server.dialog.action.CustomAll
import net.minecraft.server.dialog.body.DialogBody
import net.minecraft.server.dialog.body.PlainMessage
import net.minecraft.server.level.ServerPlayer
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.data.DataInstance
import work.lclpnet.map_api.data.DataManager
import work.lclpnet.map_api.schema.DataDefinition
import work.lclpnet.map_api.schema.ListDataDefinition
import work.lclpnet.map_api.schema.SingleDataDefinition
import work.lclpnet.map_utils.editor.SessionManager
import work.lclpnet.map_utils.identifier
import work.lclpnet.map_utils.schema.SCHEMA_DIR
import work.lclpnet.map_utils.schema.SchemaManager
import work.lclpnet.map_utils.util.openConfirmDialog
import java.util.*
import kotlin.io.path.relativeTo

class SchemaSelectorDialog(
    val schemaManager: SchemaManager,
    val sessionManager: SessionManager,
    val translations: Translations
) {

    val dataManager: DataManager
        get() = schemaManager.dataManager

    fun open(player: ServerPlayer) {
        schemaManager.reloadSchemas().whenComplete { _, _ ->
            val schema = schemaManager.getSchema(player.level())

            if (schema == null) {
                openSelector(player)
            } else {
                openEditor(player)
            }
        }
    }

    private fun openSelector(player: ServerPlayer) {
        val title = translations.translateText("schema_selector.title").translateFor(player)

        val buttons = schemaManager.schemas.map { (id, schema) ->
            val nbt = CompoundTag()
            nbt.putString("id", id)

            ActionButton(
                CommonButtonData(Component.literal(schema.name), 150),
                Optional.of(CustomAll(SELECT_ID, Optional.of(nbt))))
        }

        val body = mutableListOf<DialogBody>()

        if (buttons.isEmpty()) {
            val schemaRelativePath = SCHEMA_DIR.toAbsolutePath()
                .relativeTo(FabricLoader.getInstance().gameDir.toAbsolutePath())

            body.add(
                PlainMessage(
                translations.translateText(
                    "schema_selector.no_schemas",
                    Component.literal(schemaRelativePath.toString())
                        .withStyle(YELLOW)
                ).withStyle(RED).translateFor(player),
                200
            ))
        }

        val commonData = CommonDialogData(
            title, Optional.empty(), true, true, DialogAction.CLOSE, body, listOf()
        )

        val dialog = if (buttons.isNotEmpty()) {
            MultiActionDialog(
                commonData,
                buttons,
                Optional.of(
                    ActionButton(
                    CommonButtonData(Component.translatable("gui.cancel"), 150),
                    Optional.empty()
                )),
                1
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

    fun selectSchema(player: ServerPlayer, nbt: CompoundTag) {
        val id = nbt.getString("id").orElse(null) ?: return

        val schema = schemaManager.schemas[id] ?: return

        schemaManager.setSchema(player.level(), schema)
        dataManager.getWorldData(player.level()).loadDefaults(schema)
        dataManager.save(player.level())

        openEditor(player)
    }

    private fun openEditor(player: ServerPlayer) {
        val schema = schemaManager.getSchema(player.level())

        if (schema == null) {
            openSelector(player)
            return
        }

        val buttons = mutableListOf<ActionButton>()

        for ((propertyId, dataDefinition) in schema.properties) {
            when (dataDefinition) {
                is SingleDataDefinition<*> -> addSingleData(player, propertyId, dataDefinition, buttons)
                is ListDataDefinition<*> -> addListData(player, propertyId, dataDefinition, buttons)
            }
        }

        val body = listOf<DialogBody>(
            PlainMessage(
                translations.translateText("schema_editor.unlink_schema")
                    .translateFor(player)
                    .withStyle() { it
                        .withColor(0xf7adad)
                        .withClickEvent(ClickEvent.Custom(UNLINK_ID, Optional.empty()))
                    },
                200
            )
        )

        val dialog = MultiActionDialog(
            CommonDialogData(
                Component.literal(schema.name), Optional.empty(), true, true, DialogAction.CLOSE, body, listOf()
            ),
            buttons,
            Optional.of(
                ActionButton(
                CommonButtonData(Component.translatable("gui.cancel"), 150),
                Optional.empty()
            )),
            2
        )

        player.openDialog(Holder.direct(dialog))
    }

    private fun addSingleData(
        player: ServerPlayer,
        propertyId: String,
        dataDefinition: SingleDataDefinition<*>,
        buttons: MutableList<ActionButton>
    ) {
        val exists = dataManager.hasData(player.level(), propertyId)
        val label = Component.literal(dataDefinition.name)
            .withStyle(if (exists) GREEN else if (dataDefinition.optional) GRAY else RED)

        val nbt = CompoundTag()
        nbt.putString("propertyId", propertyId)
        nbt.putString("definitionId", propertyId)

        buttons.add(
            ActionButton(
            CommonButtonData(label, 200),
            Optional.of(CustomAll(EDIT_PROPERTY_ID, Optional.of(nbt)))
        ))

        val actionLabel = Component.literal(if (exists) "\uD83D\uDD8A" else "+")

        buttons.add(
            ActionButton(
            CommonButtonData(actionLabel, 20),
            Optional.of(CustomAll(EDIT_PROPERTY_ID, Optional.of(nbt)))
        ))
    }

    private fun addListData(
        player: ServerPlayer,
        propertyId: String,
        dataDefinition: ListDataDefinition<*>,
        buttons: MutableList<ActionButton>
    ) {
        val worldData = dataManager.getWorldData(player.level())
        val instances = worldData.byRole(dataDefinition.role, dataDefinition.data)

        val label = Component.empty()
            .append(
                Component.literal(dataDefinition.name)
                .withStyle(GREEN))
            .append(" (")
            .append(
                Component.literal("${instances.size}")
                .withStyle(YELLOW))
            .append(")")

        val nbt = CompoundTag()
        nbt.putString("propertyId", propertyId)

        buttons.add(
            ActionButton(
            CommonButtonData(label, 200),
            Optional.of(CustomAll(LIST_PROPERTY_ID, Optional.of(nbt)))
        ))

        val actionLabel = Component.literal("\uD83D\uDD8A")

        buttons.add(
            ActionButton(
            CommonButtonData(actionLabel, 20),
            Optional.of(CustomAll(LIST_PROPERTY_ID, Optional.of(nbt)))
        ))
    }

    fun listProperty(player: ServerPlayer, nbt: CompoundTag) {
        val propertyId = nbt.getString("propertyId").orElse(null) ?: return

        val schema = schemaManager.getSchema(player.level()) ?: return
        val definition = schema.properties[propertyId] ?: return

        val worldData = dataManager.getWorldData(player.level())
        val entries = worldData.entriesByRole(definition.role, definition.data)

        val buttons = mutableListOf<ActionButton>()

        for ((i, entry) in entries.withIndex()) {
            val nbt = CompoundTag()
            nbt.putString("propertyId", entry.key)
            nbt.putString("definitionId", propertyId)

            buttons.add(
                ActionButton(
                    CommonButtonData(
                        Component.empty()
                            .append(
                                Component.literal("${entry.key} ")
                                .withStyle(YELLOW))
                            .append("(")
                            .append(translations.translateText("type.${definition.data.id()}")
                                .withStyle(AQUA)
                                .translateFor(player)
                                .append(Component.literal(" #${i + 1}").withStyle(YELLOW)))
                            .append(")"),
                        200
                    ),
                    Optional.of(CustomAll(EDIT_PROPERTY_ID, Optional.of(nbt)))
                )
            )
        }

        val createNbt = CompoundTag()
        createNbt.putString("propertyId", worldData.uniqueId(propertyId))
        createNbt.putString("definitionId", propertyId)

        buttons.add(
            ActionButton(
            CommonButtonData(
                translations.translateText("add").translateFor(player),
                200
            ),
            Optional.of(CustomAll(EDIT_PROPERTY_ID, Optional.of(createNbt)))
        ))

        val dialog = MultiActionDialog(
            CommonDialogData(
                Component.literal(definition.name), Optional.empty(), true, true, DialogAction.CLOSE, listOf(), listOf()
            ),
            buttons,
            Optional.of(
                ActionButton(
                CommonButtonData(Component.translatable("gui.cancel"), 150),
                Optional.empty()
            )),
            1
        )

        player.openDialog(Holder.direct(dialog))
    }

    fun editProperty(player: ServerPlayer, nbt: CompoundTag) {
        if (sessionManager.optSession(player)?.editor != null) {
            val msg = translations.translateText("create.active_editor").withStyle(YELLOW).translateFor(player)
            openConfirmDialog(player, translations, msg, CONFIRM_EDIT_PROPERTY_ID, payload = Optional.of(nbt))
            return
        }

        confirmEditProperty(player, nbt)
    }

    fun confirmEditProperty(player: ServerPlayer, nbt: CompoundTag) {
        val propertyId = nbt.getString("propertyId").orElse(null) ?: return
        val definitionId = nbt.getString("definitionId").orElse(null) ?: return
        val schema = schemaManager.getSchema(player.level()) ?: return

        val definition = schema.properties[definitionId] ?: return

        handleEditProperty(player, propertyId, definition)
    }

    private fun <T> handleEditProperty(
        player: ServerPlayer,
        propertyId: String,
        definition: DataDefinition<T, *>,
    ) {
        val value = dataManager.getData(player.level(), propertyId, definition.data, null)
        val session = sessionManager.getSession(player)
        session.destroyEditor()

        val editor = if (value == null) {
            session.createEditor(definition.data).also {
                it.propertyId = propertyId
                it.role = definition.role
            }
        } else {
            session.createEditorFrom(DataInstance(definition.data, value, definition.role), propertyId)
        }

        session.setEditor(editor)
    }

    fun unlink(player: ServerPlayer) {
        val msg = translations.translateText("schema_editor.confirm_unlink").withStyle(YELLOW).translateFor(player)
        val unlinkLabel = translations.translateText("unlink").withStyle(RED).translateFor(player)
        openConfirmDialog(player, translations, msg, CONFIRM_UNLINK_ID, unlinkLabel)
    }

    fun confirmUnlink(player: ServerPlayer) {
        schemaManager.setSchema(player.level(), null)
        dataManager.save(player.level())
    }

    companion object {
        val SELECTOR_ID = identifier("schema_selector")
        val SELECT_ID = identifier("schema_select")
        val EDIT_PROPERTY_ID = identifier("schema_edit_property")
        val CONFIRM_EDIT_PROPERTY_ID = identifier("schema_confirm_edit_property")
        val LIST_PROPERTY_ID = identifier("schema_list_property")
        val UNLINK_ID = identifier("schema_unlink")
        val CONFIRM_UNLINK_ID = identifier("schema_confirm_unlink")
    }
}