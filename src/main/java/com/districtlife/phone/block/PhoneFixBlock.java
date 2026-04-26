package com.districtlife.phone.block;

import com.districtlife.phone.item.PhoneFixItem;
import com.districtlife.phone.network.PacketHandler;
import com.districtlife.phone.network.PacketOpenPhoneFix;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.AttachFace;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import java.util.List;

public class PhoneFixBlock extends Block {

    public static final DirectionProperty        FACING = HorizontalBlock.FACING;
    public static final EnumProperty<AttachFace> FACE   = EnumProperty.create("face", AttachFace.class);

    // Mural : 8x12x2 px colle contre le mur
    private static final VoxelShape WALL_SOUTH = Block.box( 4, 2,  0, 12, 14,  2);
    private static final VoxelShape WALL_NORTH = Block.box( 4, 2, 14, 12, 14, 16);
    private static final VoxelShape WALL_EAST  = Block.box( 0, 2,  4,  2, 14, 12);
    private static final VoxelShape WALL_WEST  = Block.box(14, 2,  4, 16, 14, 12);

    // Sol : 8x2x12 px pose a plat
    private static final VoxelShape FLOOR_NS   = Block.box( 4, 0,  2, 12,  2, 14);
    private static final VoxelShape FLOOR_EW   = Block.box( 2, 0,  4, 14,  2, 12);

    // Plafond
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

    // -------------------------------------------------------------------------
    // TileEntity
    // -------------------------------------------------------------------------

    @Override
    public boolean hasTileEntity(BlockState state) { return true; }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new PhoneFixTileEntity();
    }

    // -------------------------------------------------------------------------
    // Clic droit : ouvre l'ecran de composition
    // -------------------------------------------------------------------------

    @Override
    public ActionResultType use(BlockState state, World level, BlockPos pos,
                                net.minecraft.entity.player.PlayerEntity player,
                                Hand hand, BlockRayTraceResult hit) {
        if (!level.isClientSide()) {
            TileEntity te = level.getBlockEntity(pos);
            if (te instanceof PhoneFixTileEntity && player instanceof ServerPlayerEntity) {
                PhoneFixTileEntity fixTE = (PhoneFixTileEntity) te;
                PacketHandler.sendToPlayer(
                        new PacketOpenPhoneFix(
                                fixTE.getPhoneNumber(),
                                fixTE.getPendingCaller(),
                                fixTE.getActiveCall(),
                                pos),
                        (ServerPlayerEntity) player);
            }
        }
        return ActionResultType.sidedSuccess(level.isClientSide());
    }

    // -------------------------------------------------------------------------
    // Drop avec conservation du numero de telephone
    // -------------------------------------------------------------------------

    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        TileEntity te = builder.getOptionalParameter(LootParameters.BLOCK_ENTITY);
        if (te instanceof PhoneFixTileEntity) {
            String phone = ((PhoneFixTileEntity) te).getPhoneNumber();
            if (!phone.isEmpty()) {
                for (ItemStack stack : drops) {
                    if (stack.getItem() instanceof PhoneFixItem) {
                        PhoneFixItem.setPhoneNumber(stack, phone);
                    }
                }
            }
        }
        return drops;
    }

    // -------------------------------------------------------------------------
    // Hitbox
    // -------------------------------------------------------------------------

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
