package com.districtlife.phone.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.AttachFace;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;

public class PhoneFixBlock extends Block {

    public static final DirectionProperty        FACING = HorizontalBlock.FACING;
    public static final EnumProperty<AttachFace> FACE   = EnumProperty.create("face", AttachFace.class);

    // Mural : 8x12x2 px colle contre le mur
    private static final VoxelShape WALL_SOUTH = Block.box( 4, 2,  0, 12, 14,  2);
    private static final VoxelShape WALL_NORTH = Block.box( 4, 2, 14, 12, 14, 16);
    private static final VoxelShape WALL_EAST  = Block.box( 0, 2,  4,  2, 14, 12);
    private static final VoxelShape WALL_WEST  = Block.box(14, 2,  4, 16, 14, 12);

    // Sol : 8x2x12 px pose a plat (memes dimensions que le mural, juste couche)
    // NS = FACING nord/sud, EW = FACING est/ouest (rotation 90 deg)
    private static final VoxelShape FLOOR_NS   = Block.box( 4, 0,  2, 12,  2, 14);
    private static final VoxelShape FLOOR_EW   = Block.box( 2, 0,  4, 14,  2, 12);

    // Plafond : meme chose mais en haut
    private static final VoxelShape CEILING_NS = Block.box( 4, 14,  2, 12, 16, 14);
    private static final VoxelShape CEILING_EW = Block.box( 2, 14,  4, 14, 16, 12);

    public PhoneFixBlock() {
        super(Properties.of(Material.STONE)
                .strength(1.5f, 6.0f)
                .sound(SoundType.STONE)
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACE,   AttachFace.WALL)
                .setValue(FACING, Direction.SOUTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext ctx) {
        Direction facing = state.getValue(FACING);
        boolean ns = facing == Direction.NORTH || facing == Direction.SOUTH;

        switch (state.getValue(FACE)) {
            case FLOOR:   return ns ? FLOOR_NS   : FLOOR_EW;
            case CEILING: return ns ? CEILING_NS : CEILING_EW;
            default: // WALL
                switch (facing) {
                    case NORTH: return WALL_NORTH;
                    case EAST:  return WALL_EAST;
                    case WEST:  return WALL_WEST;
                    default:    return WALL_SOUTH;
                }
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext ctx) {
        Direction clicked = ctx.getClickedFace();
        AttachFace face;
        Direction facing;

        if (clicked == Direction.UP) {
            face   = AttachFace.FLOOR;
            facing = ctx.getHorizontalDirection();
        } else if (clicked == Direction.DOWN) {
            face   = AttachFace.CEILING;
            facing = ctx.getHorizontalDirection();
        } else {
            face   = AttachFace.WALL;
            facing = clicked;
        }

        return this.defaultBlockState()
                .setValue(FACE,   face)
                .setValue(FACING, facing);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING);
    }
}
