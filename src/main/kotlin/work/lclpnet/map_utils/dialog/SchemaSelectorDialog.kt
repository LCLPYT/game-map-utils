package work.lclpnet.map_utils.dialog

import net.minecraft.dialog.AfterAction
import net.minecraft.dialog.DialogActionButtonData
import net.minecraft.dialog.DialogButtonData
import net.minecraft.dialog.DialogCommonData
import net.minecraft.dialog.action.DynamicCustomDialogAction
import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.body.PlainMessageDialogBody
import net.minecraft.dialog.type.MultiActionDialog
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting.*
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.data.DataInstance
import work.lclpnet.map_api.data.DataManager
import work.lclpnet.map_api.schema.ListDataDefinition
import work.lclpnet.map_api.schema.SingleDataDefinition
import work.lclpnet.map_utils.editor.SessionManager
import work.lclpnet.map_utils.identifier
import work.lclpnet.map_utils.schema.SchemaManager
import work.lclpnet.map_utils.util.openConfirmDialog
import java.util.*

class SchemaSelectorDialog(
    val schemaManager: SchemaManager,
    val sessionManager: SessionManager,
    val translations: Translations
) {

    val dataManager: DataManager
        get() = schemaManager.dataManager

    fun open(player: ServerPlayerEntity) {
        val schema = schemaManager.getSchema(player.world)

        if (schema == null) {
            openSelector(player)
        } else {
            openEditor(player)
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

        val dialog = MultiActionDialog(
            DialogCommonData(
                title, Optional.empty(), true, true, AfterAction.CLOSE, listOf(), listOf()
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

    fun selectSchema(player: ServerPlayerEntity, nbt: NbtCompound) {
        val id = nbt.getString("id", null) ?: return

        val schema = schemaManager.schemas[id] ?: return

        schemaManager.setSchema(player.world, schema)

        openEditor(player)
    }

    private fun openEditor(player: ServerPlayerEntity) {
        val schema = schemaManager.getSchema(player.world)

        if (schema == null) {
            openSelector(player)
            return
        }

        val buttons = mutableListOf<DialogActionButtonData>()

        for ((propertyId, dataDefinition) in schema.properties) {
            when (dataDefinition) {
                is SingleDataDefinition<*> -> addSingleData(player, propertyId, dataDefinition, buttons)
                is ListDataDefinition<*> -> addListData(dataDefinition, buttons)
                else -> {}
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
        val exists = dataManager.hasData(player.world, propertyId)
        val label = Text.literal(dataDefinition.name)
            .formatted(if (exists || dataDefinition.optional) GREEN else RED)

        val nbt = NbtCompound()
        nbt.putString("propertyId", propertyId)

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
        dataDefinition: ListDataDefinition<*>,
        buttons: MutableList<DialogActionButtonData>
    ) {
        val label = Text.literal(dataDefinition.name)
            .formatted(GREEN)

        val nbt = NbtCompound()
        nbt.putString("role", dataDefinition.role)

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
        val schema = schemaManager.getSchema(player.world) ?: return

        val definition = schema.properties[propertyId] ?: return

        if (definition !is SingleDataDefinition<*>) return

        handleEditProperty(player, propertyId, definition)
    }

    private fun <T> handleEditProperty(
        player: ServerPlayerEntity,
        propertyId: String,
        definition: SingleDataDefinition<T>
    ) {
        val value = dataManager.getData(player.world, propertyId, definition.data, null)
        val session = sessionManager.getSession(player)
        session.destroyEditor()

        val editor = if (value == null) {
            session.createEditor(definition.data).also { it.propertyId = propertyId }
        } else {
            session.createEditorFrom(DataInstance(definition.data, value, definition.role), propertyId)
        }

        session.setEditor(editor)
    }

    fun listProperty(player: ServerPlayerEntity, nbt: NbtCompound) {
        val role = nbt.getString("role", null) ?: return

        // TODO implement
    }

    fun unlink(player: ServerPlayerEntity) {
        val msg = translations.translateText("schema_editor.confirm_unlink").formatted(YELLOW).translateFor(player)
        val unlinkLabel = translations.translateText("unlink").formatted(RED).translateFor(player)
        openConfirmDialog(player, translations, msg, CONFIRM_UNLINK_ID, unlinkLabel)
    }

    fun confirmUnlink(player: ServerPlayerEntity) {
        schemaManager.setSchema(player.world, null)
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