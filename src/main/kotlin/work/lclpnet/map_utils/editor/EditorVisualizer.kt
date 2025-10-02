package work.lclpnet.map_utils.editor

import net.minecraft.entity.Entity
import work.lclpnet.gaco.dynamic_entities.DynamicEntity
import work.lclpnet.gaco.dynamic_entities.DynamicEntityManager
import work.lclpnet.gaco.dynamic_entities.PlayerSpecificDynamicEntity

class EditorVisualizer(val args: SessionArgs, val dynamicEntityManager: DynamicEntityManager) : Visualizer {

    val entities = mutableSetOf<DynamicEntity>()

    override fun addEntity(entity: Entity) {
        val dynamicEntity = PlayerSpecificDynamicEntity(entity, args.player().uuid)

        entities.add(dynamicEntity)

        dynamicEntityManager.add(dynamicEntity)
    }

    fun destroy() {
        for (entity in entities) {
            dynamicEntityManager.remove(entity)
        }
    }
}