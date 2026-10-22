package com.lach.noted;

import com.lach.noted.net.OpenNotesPayload;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class StickyNoteBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<StickyNoteBlock> CODEC = simpleCodec(StickyNoteBlock::new);

    static final SoundType SOUND = new SoundType(1.0f, 1.4f, SoundEvents.BOOK_PAGE_TURN, SoundEvents.WOOL_STEP,
            SoundEvents.SLIME_BLOCK_PLACE, SoundEvents.WOOL_HIT, SoundEvents.WOOL_FALL);
    static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOL).noCollission().noOcclusion().instabreak()
            .sound(SOUND).pushReaction(PushReaction.DESTROY).ignitedByLava();

    private static final VoxelShape FLOOR = box(0, 0, 0, 16, 1, 16);
    private static final VoxelShape CEILING = box(0, 15, 0, 16, 16, 16);
    private static final VoxelShape NORTH = box(0, 0, 15, 16, 16, 16);
    private static final VoxelShape SOUTH = box(0, 0, 0, 16, 16, 1);
    private static final VoxelShape EAST = box(0, 0, 0, 1, 16, 16);
    private static final VoxelShape WEST = box(15, 0, 0, 16, 16, 16);

    public StickyNoteBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.WALL));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACE, FACING);
    }

    /** Rotation taking the face-local frame (x right, y up, +z out of the surface) into block space. */
    public static Quaternionf faceRotation(BlockState state) {
        float yRot = state.getValue(FACING).toYRot();
        Quaternionf q = new Quaternionf();
        switch (state.getValue(FACE)) {
            case WALL -> q.mul(Axis.YP.rotationDegrees(-yRot));
            case FLOOR -> q.mul(Axis.YP.rotationDegrees(180 - yRot)).mul(Axis.XP.rotationDegrees(-90));
            case CEILING -> q.mul(Axis.YP.rotationDegrees(-yRot)).mul(Axis.XP.rotationDegrees(90));
        }
        return q;
    }

    /** Grid slot (0-8, row-major from top-left as seen from the front) under a world-space point. */
    public static int slotAt(BlockState state, BlockPos pos, Vec3 point) {
        Vector3f d = new Vector3f((float) (point.x - pos.getX() - 0.5), (float) (point.y - pos.getY() - 0.5), (float) (point.z - pos.getZ() - 0.5));
        faceRotation(state).conjugate().transform(d);
        int col = Mth.clamp((int) Math.floor((d.x + 0.5f) * 3), 0, 2);
        int row = Mth.clamp((int) Math.floor((0.5f - d.y) * 3), 0, 2);
        return row * 3 + col;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> FLOOR;
            case CEILING -> CEILING;
            case WALL -> switch (state.getValue(FACING)) {
                case SOUTH -> SOUTH;
                case EAST -> EAST;
                case WEST -> WEST;
                default -> NORTH;
            };
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof StickyNoteBlockEntity be && !be.getNotes().isEmpty()) {
            Note n = be.getNotes().get(0);
            int slot = 4;
            if (sp.pick(sp.blockInteractionRange(), 0, false) instanceof BlockHitResult hit && hit.getBlockPos().equals(pos))
                slot = slotAt(state, pos, hit.getLocation());
            be.set(0, n.withAuthor(sp.getGameProfile().getName()).withSlot(slot));
            if (n.isBlank()) PacketDistributor.sendToPlayer(sp, new OpenNotesPayload(pos, 0));
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof StickyNoteBlockEntity be) || be.getNotes().isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int clicked = be.indexAt(slotAt(state, pos, hit.getLocation()));
        if (stack.is(Noted.NOTE_ITEM.get())) {
            int slot = be.freeSlot(slotAt(state, pos, hit.getLocation()));
            if (slot < 0) {
                player.displayClientMessage(Component.translatable("message.noted.full"), true);
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
            if (player instanceof ServerPlayer sp) {
                Note n = Note.fromStack(stack, sp.getGameProfile().getName(), slot);
                int index = be.add(n);
                stack.consume(1, player);
                if (n.isBlank() && index >= 0) PacketDistributor.sendToPlayer(sp, new OpenNotesPayload(pos, index));
            }
            level.playSound(player, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 0.7f, 1.4f);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof DyeItem dye) {
            if (clicked < 0 || be.getNotes().get(clicked).color() == dye.getDyeColor()) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            if (!level.isClientSide) be.set(clicked, be.getNotes().get(clicked).withColor(dye.getDyeColor()));
            level.playSound(player, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1f, 1f);
            stack.consume(1, player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(Items.GLOW_INK_SAC) || stack.is(Items.INK_SAC)) {
            boolean glow = stack.is(Items.GLOW_INK_SAC);
            if (be.isGlowing() == glow) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            if (!level.isClientSide) be.setGlowing(glow);
            level.playSound(player, pos, glow ? SoundEvents.GLOW_INK_SAC_USE : SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 1f, 1f);
            stack.consume(1, player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return stack.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof StickyNoteBlockEntity be && !be.getNotes().isEmpty())
            PacketDistributor.sendToPlayer(sp, new OpenNotesPayload(pos, Math.max(0, be.indexAt(slotAt(state, pos, hit.getLocation())))));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> out = new ArrayList<>();
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof StickyNoteBlockEntity be)
            for (Note n : be.getNotes()) out.add(n.toStack());
        return out;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof StickyNoteBlockEntity be && !be.getNotes().isEmpty())
            return Noted.noteStack(be.getNotes().get(0).color());
        return Noted.NOTE_ITEM.toStack();
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StickyNoteBlockEntity(pos, state);
    }
}
