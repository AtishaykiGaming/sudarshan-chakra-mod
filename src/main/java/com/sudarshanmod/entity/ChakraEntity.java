package com.sudarshanmod.entity;

import com.sudarshanmod.SudarshanMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class ChakraEntity extends ProjectileEntity {

    private static final float  DAMAGE              = 12.0f;
    private static final float  AOE_DAMAGE          = 6.0f;
    private static final double AOE_RADIUS          = 3.5;
    private static final int    FIRE_TICKS          = 80;
    private static final int    MAX_FLIGHT_TICKS    = 120;
    private static final double RETURN_SPEED        = 1.4;
    private static final double RETURN_THRESHOLD_SQ = 1.5;

    private boolean returning   = false;
    private int     flightTicks = 0;

    public float spinAngle = 0f;

    public ChakraEntity(EntityType<? extends ChakraEntity> type, World world) {
        super(type, world);
        this.setNoGravity(false);
    }

    public ChakraEntity(World world, PlayerEntity thrower) {
        this(SudarshanMod.CHAKRA_ENTITY, world);
        this.setOwner(thrower);
        this.setPosition(thrower.getX(), thrower.getEyeY() - 0.1, thrower.getZ());
    }

    // Fix 1: required by 1.21.1 — just call super, no custom tracked data needed
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
    }

    @Override
    public void tick() {
        super.tick();
        flightTicks++;

        if (getWorld().isClient()) {
            spinAngle = (spinAngle + 18f) % 360f;
            spawnTrailParticlesClient();
            return;
        }

        spawnTrailParticlesServer();

        Entity owner = getOwner();

        if (!returning && flightTicks >= MAX_FLIGHT_TICKS) {
            returning = true;
        }

        if (returning) {
            handleReturn(owner);
        } else {
            handleFlight();
        }
    }

    private void handleFlight() {
        Vec3d vel = getVelocity();
        setVelocity(vel.x, vel.y - 0.03, vel.z);

        HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            returning = true;
            return;
        }

        Box searchBox = getBoundingBox().stretch(getVelocity()).expand(1.0);
        List<Entity> candidates = getWorld().getOtherEntities(this, searchBox,
                e -> e instanceof LivingEntity && e != getOwner());

        for (Entity candidate : candidates) {
            if (candidate instanceof LivingEntity target) {
                onEntityHit(target);
            }
        }

        Vec3d velocity = getVelocity();
        setPosition(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
        velocityDirty = true;
    }

    private void handleReturn(Entity owner) {
        if (owner == null) { this.discard(); return; }

        Vec3d toOwner = owner.getEyePos().subtract(getPos());

        if (toOwner.lengthSquared() <= RETURN_THRESHOLD_SQ) {
            onReturnToOwner(owner);
            return;
        }

        Vec3d returnVel = toOwner.normalize().multiply(RETURN_SPEED);
        setVelocity(returnVel);
        setPosition(getX() + returnVel.x, getY() + returnVel.y, getZ() + returnVel.z);

        Box searchBox = getBoundingBox().expand(0.8);
        List<Entity> nearby = getWorld().getOtherEntities(this, searchBox,
                e -> e instanceof LivingEntity && e != owner);
        for (Entity e : nearby) {
            onEntityHit((LivingEntity) e);
        }

        velocityDirty = true;
    }

    private void onEntityHit(LivingEntity target) {
        if (!(getWorld() instanceof ServerWorld sw)) return;

        DamageSource source = getDamageSources().thrown(this, getOwner());
        target.damage(source, DAMAGE);
        target.setOnFireFor(FIRE_TICKS / 20);

        Box aoeBox = target.getBoundingBox().expand(AOE_RADIUS);
        List<Entity> splashTargets = getWorld().getOtherEntities(this, aoeBox,
                e -> e instanceof LivingEntity && e != target && e != getOwner());
        for (Entity splash : splashTargets) {
            if (splash.squaredDistanceTo(target) <= AOE_RADIUS * AOE_RADIUS) {
                ((LivingEntity) splash).damage(source, AOE_DAMAGE);
                ((LivingEntity) splash).setOnFireFor(FIRE_TICKS / 20);
            }
        }

        sw.spawnParticles(ParticleTypes.FLAME,
                target.getX(), target.getY() + 1, target.getZ(),
                20, 0.5, 0.5, 0.5, 0.1);

        returning = true;
    }

    private void onReturnToOwner(Entity owner) {
        if (owner instanceof PlayerEntity player && !player.isCreative()) {
            player.getInventory().offerOrDrop(new ItemStack(SudarshanMod.SUDARSHAN_CHAKRA));
        }
        this.discard();
    }

    private void spawnTrailParticlesClient() {
        World world = getWorld();
        world.addParticle(ParticleTypes.FLAME,
                getX() + (world.random.nextDouble() - 0.5) * 0.2,
                getY() + (world.random.nextDouble() - 0.5) * 0.2,
                getZ() + (world.random.nextDouble() - 0.5) * 0.2,
                0, 0, 0);
        world.addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
    }

    private void spawnTrailParticlesServer() {
        if (!(getWorld() instanceof ServerWorld sw)) return;
        sw.spawnParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 3, 0.05, 0.05, 0.05, 0.01);
        sw.spawnParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        // handled manually above
    }

    @Override
    protected boolean canHit(Entity entity) {
        return entity instanceof LivingEntity && entity != getOwner() && super.canHit(entity);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        returning   = nbt.getBoolean("Returning");
        flightTicks = nbt.getInt("FlightTicks");
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putBoolean("Returning",   returning);
        nbt.putInt("FlightTicks",     flightTicks);
    }

    // Fix 2 & 3: correct 1.21.1 spawn packet signature
    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this, 0);
    }
}
