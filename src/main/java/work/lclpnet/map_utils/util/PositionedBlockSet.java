package work.lclpnet.map_utils.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PositionedBlockSet implements Iterable<PositionedBlockSet.PositionedBlock> {

    public static final Codec<PositionedBlockSet> CODEC = PositionedBlock.CODEC.listOf().xmap(
            PositionedBlockSet::new,
            set -> set.blocks
    );

    private final List<PositionedBlock> blocks;

    public PositionedBlockSet(Map<BlockPos, BlockState> blocks) {
        this(blocks.entrySet().stream()
                .map(e -> new PositionedBlock(e.getKey(), e.getValue()))
                .toList());
    }

    public PositionedBlockSet(Collection<PositionedBlock> blocks) {
        this.blocks = List.copyOf(blocks);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PositionedBlockSet that = (PositionedBlockSet) o;
        return Objects.equals(blocks, that.blocks);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(blocks);
    }

    @Override
    public @NotNull Iterator<PositionedBlock> iterator() {
        return blocks.iterator();
    }

    public record PositionedBlock(BlockPos pos, BlockState state) {
        public static final Codec<PositionedBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(PositionedBlock::pos),
                BlockState.CODEC.fieldOf("state").forGetter(PositionedBlock::state)
        ).apply(instance, PositionedBlock::new));

        public BlockPos component1() {
            return pos;
        }

        public BlockState component2() {
            return state;
        }
    }
}
