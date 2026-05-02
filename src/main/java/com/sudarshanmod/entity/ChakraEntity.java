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

    // Required by 1.21.1 — no custom tracked data, so body is empty
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // no custom tracked data needed
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
        if (!returning && flightTicks >= MAX_FLIGHT_TICKS) returning = true;

        if (returning) handleReturn(owner);
        else handleFlight();
    }

    private void handleFlight() {
        Vec3d vel = getVelocity();
        setVelocity(vel.x, vel.y - 0.03, vel.z);

        HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
        if (hit.getType() == HitResult.Type.BLOCK) { returning = true; return; }

        Box searchBox = getBoundingBox().stretch(getVelocity()).expand(1.0);
        for (Entity e : getWorld().getOtherEntities(this, searchBox,
                en -> en instanceof LivingEntity && en != getOwner())) {
            onEntityHit((LivingEntity) e);
        }

        Vec3d v = getVelocity();
        setPosition(getX() + v.x, getY() + v.y, getZ() + v.z);
        velocityDirty = true;
    }

    private void handleReturn(Entity owner) {
        if (owner == null) { discard(); return; }

        Vec3d toOwner = owner.getEyePos().subtract(getPos());
        if (toOwner.lengthSquared() <= RETURN_THRESHOLD_SQ) { onReturnToOwner(owner); return; }

        Vec3d rv = toOwner.normalize().multiply(RETURN_SPEED);
        setVelocity(rv);
        setPosition(getX() + rv.x, getY() + rv.y, getZ() + rv.z);

        for (Entity e : getWorld().getOtherEntities(this, getBoundingBox().expand(0.8),
                en -> en instanceof LivingEntity && en != owner)) {
            onEntityHit((LivingEntity) e);
        }
        velocityDirty = true;
    }

    private void onEntityHit(LivingEntity target) {
        if (!(getWorld() instanceof ServerWorld sw)) return;

        DamageSource src = getDamageSources().thrown(this, getOwner());
        target.damage(src, DAMAGE);
        target.setOnFireFor(FIRE_TICKS / 20);

        for (Entity e : getWorld().getOtherEntities(this,
                target.getBoundingBox().expand(AOE_RADIUS),
                en -> en instanceof LivingEntity && en != target && en != getOwner())) {
            if (e.squaredDistanceTo(target) <= AOE_RADIUS * AOE_RADIUS) {
                ((LivingEntity) e).damage(src, AOE_DAMAGE);
                ((LivingEntity) e).setOnFireFor(FIRE_TICKS / 20);
            }
        }

        sw.spawnParticles(ParticleTypes.FLAME,
                target.getX(), target.getY() + 1, target.getZ(),
                20, 0.5, 0.5, 0.5, 0.1);
        returning = true;
    }

    private void onReturnToOwner(Entity owner) {
        if (owner instanceof PlayerEntity p && !p.isCreative())
            p.getInventory().offerOrDrop(new ItemStack(SudarshanMod.SUDARSHAN_CHAKRA));
        discard();
    }

    private void spawnTrailParticlesClient() {
        World w = getWorld();
        w.addParticle(ParticleTypes.FLAME,
                getX() + (w.random.nextDouble() - 0.5) * 0.2,
                getY() + (w.random.nextDouble() - 0.5) * 0.2,
                getZ() + (w.random.nextDouble() - 0.5) * 0.2, 0, 0, 0);
        w.addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
    }

    private void spawnTrailParticlesServer() {
        if (!(getWorld() instanceof ServerWorld sw)) return;
        sw.spawnParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 3, 0.05, 0.05, 0.05, 0.01);
        sw.spawnParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
    }

    // ProjectileEntity handles createSpawnPacket in 1.21.1 — do NOT override it
    @Override protected void onEntityHit(EntityHitResult r) { /* handled manually */ }

    @Override
    protected boolean canHit(Entity e) {
        return e instanceof LivingEntity && e != getOwner() && super.canHit(e);
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
}
