package com.sudarshanmod.item;

import com.sudarshanmod.SudarshanMod;
import com.sudarshanmod.entity.ChakraEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class SudarshanChakraItem extends Item {

    private static final int THROW_COOLDOWN_TICKS = 40;

    public SudarshanChakraItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient()) {
            ChakraEntity chakra = new ChakraEntity(world, player);
            chakra.setVelocity(player, player.getPitch(), player.getYaw(), 0f, 2.0f, 0.5f);
            world.spawnEntity(chakra);

            player.getItemCooldownManager().set(this, THROW_COOLDOWN_TICKS);

            if (!player.isCreative()) {
                stack.decrement(1);
            }
        }

        return TypedActionResult.success(stack, world.isClient());
    }
}
