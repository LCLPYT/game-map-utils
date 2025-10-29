package work.lclpnet.map_utils.dialog

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
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting.*
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

    fun open(player: ServerPlayerEntity) {
        schemaManager.reloadSchemas().whenComplete { _, _ ->
            val schema = schemaManager.getSchema(player.entityWorld)

            if (schema == null) {
                openSelector(player)
            } else {
                openEditor(player)
            }
        }
    }

    private fun openSelector(player: ServerPlayerEntity) {
        val title = translations.translateText("schema_selector.title").translateFor(player)

        val buttons = schemaManager.schemas.map { (id, schema) ->
            val nbt = NbtCompound()
            nbt.putString("id", id)

            DialogActionButtonData(
                DialogButtonData(Text.literal(schema.name), 150),
                Optional.of(DynamicCustomDialogAction(SELECT_ID, Optional.of(nbt))))
        }

        val body = mutableListOf<DialogBody>()

        if (buttons.isEmpty()) {
            val schemaRelativePath = SCHEMA_DIR.toAbsolutePath()
                .relativeTo(FabricLoader.getInstance().gameDir.toAbsolutePath())

            body.add(PlainMessageDialogBody(
                translations.translateText(
                    "schema_selector.no_schemas",
                    Text.literal(schemaRelativePath.toString())
                        .formatted(YELLOW)
                ).formatted(RED).translateFor(player),
                200
            ))
        }

        val commonData = DialogCommonData(
            title, Optional.empty(), true, true, AfterAction.CLOSE, body, listOf()
        )

        val dialog = if (buttons.isNotEmpty()) {
            MultiActionDialog(
                commonData,
                buttons,
                Optional.of(DialogActionButtonData(
                    DialogButtonData(Text.translatable("gui.cancel"), 150),
                    Optional.empty()
                )),
                1
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

    fun selectSchema(player: ServerPlayerEntity, nbt: NbtCompound) {
        val id = nbt.getString("id", null) ?: return

        val schema = schemaManager.schemas[id] ?: return

        schemaManager.setSchema(player.entityWorld, schema)
        dataManager.getWorldData(player.entityWorld).loadDefaults(schema)
        dataManager.save(player.entityWorld)

        openEditor(player)
    }

    private fun openEditor(player: ServerPlayerEntity) {
        val schema = schemaManager.getSchema(player.entityWorld)

        if (schema == null) {
            openSelector(player)
            return
        }

        val buttons = mutableListOf<DialogActionButtonData>()

        for ((propertyId, dataDefinition) in schema.properties) {
            when (dataDefinition) {
                is SingleDataDefinition<*> -> addSingleData(player, propertyId, dataDefinition, buttons)
                is ListDataDefinition<*> -> addListData(player, propertyId, dataDefinition, buttons)
            }
        }

        val body = listOf<DialogBody>(
            PlainMessageDialogBody(
                translations.translateText("schema_editor.unlink_schema")
                    .translateFor(player)
                    .styled { it
                        .withColor(0xf7adad)
                        .withClickEvent(ClickEvent.Custom(UNLINK_ID, Optional.empty()))
                    },
                200
            )
        )

        val dialog = MultiActionDialog(
            DialogCommonData(
                Text.literal(schema.name), Optional.empty(), true, true, AfterAction.CLOSE, body, listOf()
            ),
            buttons,
            Optional.of(DialogActionButtonData(
                DialogButtonData(Text.translatable("gui.cancel"), 150),
                Optional.empty()
            )),
            2
        )

        player.openDialog(RegistryEntry.of(dialog))
    }

    private fun addSingleData(
        player: ServerPlayerEntity,
        propertyId: String,
        dataDefinition: SingleDataDefinition<*>,
        buttons: MutableList<DialogActionButtonData>
    ) {
        val exists = dataManager.hasData(player.entityWorld, propertyId)
        val label = Text.literal(dataDefinition.name)
            .formatted(if (exists) GREEN else if (dataDefinition.optional) GRAY else RED)

        val nbt = NbtCompound()
        nbt.putString("propertyId", propertyId)
        nbt.putString("definitionId", propertyId)

        buttons.add(DialogActionButtonData(
            DialogButtonData(label, 200),
            Optional.of(DynamicCustomDialogAction(EDIT_PROPERTY_ID, Optional.of(nbt)))
        ))

        val actionLabel = Text.literal(if (exists) "\uD83D\uDD8A" else "+")

        buttons.add(DialogActionButtonData(
            DialogButtonData(actionLabel, 20),
            Optional.of(DynamicCustomDialogAction(EDIT_PROPERTY_ID, Optional.of(nbt)))
        ))
    }

    private fun addListData(
        player: ServerPlayerEntity,
        propertyId: String,
        dataDefinition: ListDataDefinition<*>,
        buttons: MutableList<DialogActionButtonData>
    ) {
        val worldData = dataManager.getWorldData(player.entityWorld)
        val instances = worldData.byRole(dataDefinition.role, dataDefinition.data)

        val label = Text.empty()
            .append(Text.literal(dataDefinition.name)
                .formatted(GREEN))
            .append(" (")
            .append(Text.literal("${instances.size}")
                .formatted(YELLOW))
            .append(")")

        val nbt = NbtCompound()
        nbt.putString("propertyId", propertyId)

        buttons.add(DialogActionButtonData(
            DialogButtonData(label, 200),
            Optional.of(DynamicCustomDialogAction(LIST_PROPERTY_ID, Optional.of(nbt)))
        ))

        val actionLabel = Text.literal("\uD83D\uDD8A")

        buttons.add(DialogActionButtonData(
            DialogButtonData(actionLabel, 20),
            Optional.of(DynamicCustomDialogAction(LIST_PROPERTY_ID, Optional.of(nbt)))
        ))
    }

    fun listProperty(player: ServerPlayerEntity, nbt: NbtCompound) {
        val propertyId = nbt.getString("propertyId", null) ?: return

        val schema = schemaManager.getSchema(player.entityWorld) ?: return
        val definition = schema.properties[propertyId] ?: return

        val worldData = dataManager.getWorldData(player.entityWorld)
        val entries = worldData.entriesByRole(definition.role, definition.data)

        val buttons = mutableListOf<DialogActionButtonData>()

        for ((i, entry) in entries.withIndex()) {
            val nbt = NbtCompound()
            nbt.putString("propertyId", entry.key)
            nbt.putString("definitionId", propertyId)

            buttons.add(
                DialogActionButtonData(
                    DialogButtonData(
                        Text.empty()
                            .append(Text.literal("${entry.key} ")
                                .formatted(YELLOW))
                            .append("(")
                            .append(translations.translateText("type.${definition.data.id()}")
                                .formatted(AQUA)
                                .translateFor(player)
                                .append(Text.literal(" #${i + 1}").formatted(YELLOW)))
                            .append(")"),
                        200
                    ),
                    Optional.of(DynamicCustomDialogAction(EDIT_PROPERTY_ID, Optional.of(nbt)))
                )
            )
        }

        val createNbt = NbtCompound()
        createNbt.putString("propertyId", worldData.uniqueId(propertyId))
        createNbt.putString("definitionId", propertyId)

        buttons.add(DialogActionButtonData(
            DialogButtonData(
                translations.translateText("add").translateFor(player),
                200
            ),
            Optional.of(DynamicCustomDialogAction(EDIT_PROPERTY_ID, Optional.of(createNbt)))
        ))

        val dialog = MultiActionDialog(
            DialogCommonData(
                Text.literal(definition.name), Optional.empty(), true, true, AfterAction.CLOSE, listOf(), listOf()
            ),
            buttons,
            Optional.of(DialogActionButtonData(
                DialogButtonData(Text.translatable("gui.cancel"), 150),
                Optional.empty()
            )),
            1
        )

        player.openDialog(RegistryEntry.of(dialog))
    }

    fun editProperty(player: ServerPlayerEntity, nbt: NbtCompound) {
        if (sessionManager.optSession(player)?.editor != null) {
            val msg = translations.translateText("create.active_editor").formatted(YELLOW).translateFor(player)
            openConfirmDialog(player, translations, msg, CONFIRM_EDIT_PROPERTY_ID, payload = Optional.of(nbt))
            return
        }

        confirmEditProperty(player, nbt)
    }

    fun confirmEditProperty(player: ServerPlayerEntity, nbt: NbtCompound) {
        val propertyId = nbt.getString("propertyId", null) ?: return
        val definitionId = nbt.getString("definitionId", null) ?: return
        val schema = schemaManager.getSchema(player.entityWorld) ?: return

        val definition = schema.properties[definitionId] ?: return

        handleEditProperty(player, propertyId, definition)
    }

    private fun <T> handleEditProperty(
        player: ServerPlayerEntity,
        propertyId: String,
        definition: DataDefinition<T, *>,
    ) {
        val value = dataManager.getData(player.entityWorld, propertyId, definition.data, null)
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

    fun unlink(player: ServerPlayerEntity) {
        val msg = translations.translateText("schema_editor.confirm_unlink").formatted(YELLOW).translateFor(player)
        val unlinkLabel = translations.translateText("unlink").formatted(RED).translateFor(player)
        openConfirmDialog(player, translations, msg, CONFIRM_UNLINK_ID, unlinkLabel)
    }

    fun confirmUnlink(player: ServerPlayerEntity) {
        schemaManager.setSchema(player.entityWorld, null)
        dataManager.save(player.entityWorld)
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