package com.sudarshanmod.item;

import com.sudarshanmod.SudarshanMod;
import com.sudarshanmod.entity.ChakraEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class SudarshanChakraItem extends Item {

    /** Cooldown in ticks (20 ticks = 1 second).
     *  The chakra must fully return before it can be thrown again. */
    private static final int THROW_COOLDOWN_TICKS = 40;

    public SudarshanChakraItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        // Don't throw if already in flight or on cooldown
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient()) {
            // Spawn the chakra entity
            ChakraEntity chakra = new ChakraEntity(world, player);
            chakra.setVelocity(player, player.getPitch(), player.getYaw(), 0f, 2.0f, 0.5f);
            world.spawnEntity(chakra);

            // Play throw sound
            world.playSound(
                null, player.getX(), player.getY(), player.getZ(),
                SudarshanMod.CHAKRA_THROW, SoundCategory.PLAYERS,
                1.0f, 0.9f + world.getRandom().nextFloat() * 0.2f
            );

            // Apply cooldown so the player can't spam-throw
            player.getItemCooldownManager().set(this, THROW_COOLDOWN_TICKS);

            // Consume item from non-creative players while in flight
            // (returned by ChakraEntity when it comes back)
            if (!player.isCreative()) {
                stack.decrement(1);
            }
        }

        return TypedActionResult.success(stack, world.isClient());
    }
}
