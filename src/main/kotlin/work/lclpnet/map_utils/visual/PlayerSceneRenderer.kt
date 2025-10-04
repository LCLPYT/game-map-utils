package work.lclpnet.map_utils.visual

import work.lclpnet.gaco.dynamic_entities.DynamicEntityManager
import work.lclpnet.gaco.scene.Scene
import work.lclpnet.map_utils.editor.SessionArgs

class PlayerSceneRenderer(val args: SessionArgs, dynamicEntityManager: DynamicEntityManager) : SceneRenderer {
    override val scene = Scene(
        PlayerMountContext(args.world, dynamicEntityManager, args.player().uuid)
    )
}