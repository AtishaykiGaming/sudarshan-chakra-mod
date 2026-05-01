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
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class ChakraEntity extends ProjectileEntity {

    // ── Tuning constants ──────────────────────────────────────────────────
    private static final float  DAMAGE              = 12.0f;  // direct hit damage
    private static final float  AOE_DAMAGE          = 6.0f;   // splash damage
    private static final double AOE_RADIUS          = 3.5;    // blocks
    private static final int    FIRE_TICKS          = 80;     // 4 seconds
    private static final int    MAX_FLIGHT_TICKS    = 120;    // 6 s before forced return
    private static final double RETURN_SPEED        = 1.4;    // blocks/tick while returning
    private static final double RETURN_THRESHOLD_SQ = 1.5;    // sq-distance to "arrive"

    // ── State ──────────────────────────────────────────────────────────
    private boolean returning    = false;
    private int     flightTicks  = 0;

    /** Rotation angle for rendering — incremented client-side */
    public float spinAngle = 0f;

    public ChakraEntity(EntityType<? extends ChakraEntity> type, World world) {
        super(type, world);
        this.setNoGravity(false);
    }

    /** Convenience constructor used when the player throws the chakra. */
    public ChakraEntity(World world, PlayerEntity thrower) {
        this(SudarshanMod.CHAKRA_ENTITY, world);
        this.setOwner(thrower);
        // Start just in front of the player's face
        this.setPosition(
            thrower.getX(),
            thrower.getEyeY() - 0.1,
            thrower.getZ()
        );
    }

    // ── Data Tracker ───────────────────────────────────────────────────────

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        // Add any custom data trackers here if needed
    }

    // ── Tick logic ────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        flightTicks++;

        // ── Client side: spin animation ───────────────────────────────────
        if (getWorld().isClient()) {
            spinAngle = (spinAngle + 18f) % 360f; // 20 ticks per full rotation
            spawnTrailParticles();
            return;
        }

        // ── Server side ───────────────────────────────────────────────────
        spawnServerParticles();

        Entity owner = getOwner();

        // Force return after max flight time
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
        // Apply slight gravity when not yet returning
        Vec3d vel = getVelocity();
        setVelocity(vel.x, vel.y - 0.03, vel.z);

        // Check collision with blocks
        HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            // Hit a wall — start returning immediately
            returning = true;
            return;
        }

        // Check entity hits (within a slightly expanded box for reliability)
        Vec3d nextPos = getPos().add(getVelocity());
        Box searchBox = getBoundingBox().stretch(getVelocity()).expand(1.0);
        List<Entity> candidates = getWorld().getOtherEntities(this, searchBox, e -> e instanceof LivingEntity && e != getOwner());

        for (Entity candidate : candidates) {
            if (candidate instanceof LivingEntity target) {
                onEntityHit(target);
            }
        }

        // Move
        Vec3d velocity = getVelocity();
        setPosition(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
        velocityDirty = true;
    }

    private void handleReturn(Entity owner) {
        if (owner == null) {
            this.discard();
            return;
        }

        Vec3d toOwner = owner.getEyePos().subtract(getPos());

        // Arrived at owner — give item back and discard
        if (toOwner.lengthSquared() <= RETURN_THRESHOLD_SQ) {
            onReturnToOwner(owner);
            return;
        }

        // Steer towards owner at constant speed
        Vec3d returnVel = toOwner.normalize().multiply(RETURN_SPEED);
        setVelocity(returnVel);
        setPosition(getX() + returnVel.x, getY() + returnVel.y, getZ() + returnVel.z);

        // Hit enemies on the way back too
        Box searchBox = getBoundingBox().expand(0.8);
        List<Entity> nearby = getWorld().getOtherEntities(this, searchBox, e -> e instanceof LivingEntity && e != owner);
        for (Entity e : nearby) {
            onEntityHit((LivingEntity) e);
        }

        velocityDirty = true;
    }

    private void onEntityHit(LivingEntity target) {
        ServerWorld serverWorld = (ServerWorld) getWorld();

        // Direct damage
        DamageSource source = getDamageSources().thrown(this, getOwner());
        target.damage(source, DAMAGE);
        target.setOnFireFor(FIRE_TICKS / 20); // seconds

        // AoE splash damage
        Box aoeBox = target.getBoundingBox().expand(AOE_RADIUS);
        List<Entity> splashTargets = getWorld().getOtherEntities(this, aoeBox,
            e -> e instanceof LivingEntity && e != target && e != getOwner());
        for (Entity splash : splashTargets) {
            double dist = splash.squaredDistanceTo(target);
            if (dist <= AOE_RADIUS * AOE_RADIUS) {
                ((LivingEntity) splash).damage(source, AOE_DAMAGE);
                ((LivingEntity) splash).setOnFireFor(FIRE_TICKS / 20);
            }
        }

        // Spawn hit particles and sound (server-side; client syncs via packet)
        serverWorld.spawnParticles(ParticleTypes.FLAME,
            target.getX(), target.getY() + 1, target.getZ(),
            20, 0.5, 0.5, 0.5, 0.1);
        serverWorld.spawnParticles(ParticleTypes.LAVA,
            target.getX(), target.getY() + 1, target.getZ(),
            8, 0.3, 0.3, 0.3, 0.05);
        getWorld().playSound(null, target.getX(), target.getY(), target.getZ(),
            SudarshanMod.CHAKRA_HIT, SoundCategory.HOSTILE, 1.0f, 1.0f);

        // Begin returning after first successful hit
        returning = true;
    }

    private void onReturnToOwner(Entity owner) {
        if (owner instanceof PlayerEntity player && !player.isCreative()) {
            player.getInventory().offerOrDrop(new ItemStack(SudarshanMod.SUDARSHAN_CHAKRA));
        }
        getWorld().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
            SudarshanMod.CHAKRA_RETURN, SoundCategory.PLAYERS, 0.8f, 1.2f);
        this.discard();
    }

    // ── Particles ────────────────────────────────────────────────���────────

    /** Spawns a golden/fire trail visible to the nearby client (client tick). */
    private void spawnTrailParticles() {
        World world = getWorld();
        world.addParticle(ParticleTypes.FLAME,
            getX() + (world.random.nextDouble() - 0.5) * 0.2,
            getY() + (world.random.nextDouble() - 0.5) * 0.2,
            getZ() + (world.random.nextDouble() - 0.5) * 0.2,
            0, 0, 0);
        world.addParticle(ParticleTypes.END_ROD,
            getX(), getY(), getZ(), 0, 0, 0);
    }

    /** Server-side broadcast for the golden trail. */
    private void spawnServerParticles() {
        if (!(getWorld() instanceof ServerWorld sw)) return;
        sw.spawnParticles(ParticleTypes.FLAME,
            getX(), getY(), getZ(), 3,
            0.05, 0.05, 0.05, 0.01);
        sw.spawnParticles(ParticleTypes.END_ROD,
            getX(), getY(), getZ(), 1,
            0.0, 0.0, 0.0, 0.0);
    }

    // ── Projectile helpers ────────────────────────────────────────────────

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        // Handled manually in handleFlight() for multi-hit AoE
    }

    @Override
    protected boolean canHit(Entity entity) {
        return entity instanceof LivingEntity && entity != getOwner() && super.canHit(entity);
    }

    // ── NBT persistence ───────────────────────────────────────────────────

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

    // ── Spawn packet ──────────────────────────────────────────────────────

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
}
