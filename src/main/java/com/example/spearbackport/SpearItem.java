package com.example.spearbackport;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A spear.
 * - Jab: normal left click, like a sword.
 * - Charge: hold right click, then run into enemies. Damage grows with how fast you are moving toward them.
 *
 * The numbers below are approximations, tune them to taste.
 */
public class SpearItem extends TieredItem {
    // ---- Tuning (edit these) ----
    /** Ticks you must hold right click before the charge becomes active (20 ticks = 1 second). */
    public static final int WINDUP_TICKS = 10;
    /** Minimum closing speed (blocks per second) needed to hit with a charge. */
    public static final double MIN_SPEED = 3.0;
    /** Extra damage per block/second above (MIN_SPEED - 2). */
    public static final double SPEED_DAMAGE_FACTOR = 0.8;
    /** Maximum extra damage from speed. */
    public static final double MAX_SPEED_BONUS = 20.0;
    /** How far in front of you the charge reaches (blocks). */
    public static final double REACH = 3.5;
    /** Ticks before the same target can be hit by a charge again. */
    public static final long HIT_COOLDOWN_TICKS = 10;
    /**
     * Using an item normally slows you to 20% speed. The client multiplies movement by this
     * while charging, so the net speed is 1.3x walking (about sprint speed).
     */
    public static final float CHARGE_MOVE_BOOST = 6.5F;
    // -----------------------------

    private static final int USE_DURATION = 72000;

    private static final String KEY_TIME = "spearbackport_t";
    private static final String KEY_X = "spearbackport_x";
    private static final String KEY_Z = "spearbackport_z";
    private static final String KEY_HIT = "spearbackport_hit";

    // Same ids vanilla uses, so the tooltip shows the numbers correctly.
    private static final ResourceLocation BASE_DAMAGE_ID = ResourceLocation.withDefaultNamespace("base_attack_damage");
    private static final ResourceLocation BASE_SPEED_ID = ResourceLocation.withDefaultNamespace("base_attack_speed");

    /** Attack damage added by this item (player's own base damage of 1 is on top of this). */
    private final float weaponDamage;

    public SpearItem(Tier tier, Item.Properties properties) {
        super(tier, properties
                .durability(tier.getUses())
                .attributes(createAttributes(tier)));
        this.weaponDamage = damageFor(tier);
    }

    private static float damageFor(Tier tier) {
        return 1.0F + tier.getAttackDamageBonus();
    }

    private static ItemAttributeModifiers createAttributes(Tier tier) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_DAMAGE_ID, damageFor(tier), AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    // ----- Jab (normal attack) -----

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        return true;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    // ----- Charge (hold right click) -----

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    // The signature of this method differs between versions, so both forms are provided.
    // Only the one that really exists in the parent class is used by the game.
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Measure how fast the user (or the mount) moved since the last tick, in the look direction.
        net.minecraft.world.entity.Entity mover = user.getVehicle() != null ? user.getVehicle() : user;
        CompoundTag data = user.getPersistentData();
        long now = serverLevel.getGameTime();
        Vec3 look = user.getLookAngle();
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);

        boolean hasPrevious = data.contains(KEY_TIME) && data.getLong(KEY_TIME) == now - 1;
        double forwardSpeed = 0.0;
        if (hasPrevious && horizontal > 1.0E-4) {
            double dx = mover.getX() - data.getDouble(KEY_X);
            double dz = mover.getZ() - data.getDouble(KEY_Z);
            forwardSpeed = (dx * look.x + dz * look.z) / horizontal * 20.0; // blocks per second
        }
        data.putLong(KEY_TIME, now);
        data.putDouble(KEY_X, mover.getX());
        data.putDouble(KEY_Z, mover.getZ());

        int chargeTicks = USE_DURATION - remainingUseDuration;
        if (!hasPrevious || chargeTicks < WINDUP_TICKS || forwardSpeed < MIN_SPEED || horizontal <= 1.0E-4) {
            return;
        }

        Vec3 eye = user.getEyePosition();
        Vec3 end = eye.add(look.scale(REACH));
        AABB search = user.getBoundingBox().expandTowards(look.scale(REACH)).inflate(1.0);

        List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(LivingEntity.class, search,
                e -> e != user && e.isAlive() && !e.isSpectator() && e.isPickable()
                        && !user.isPassengerOfSameVehicle(e) && !user.isAlliedTo(e)
                        && !(e instanceof TamableAnimal pet && pet.isOwnedBy(user)));

        EquipmentSlot slot = user.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        DamageSource source = user instanceof Player player
                ? serverLevel.damageSources().playerAttack(player)
                : serverLevel.damageSources().mobAttack(user);

        for (LivingEntity target : candidates) {
            AABB box = target.getBoundingBox().inflate(0.3);
            if (!box.contains(eye) && box.clip(eye, end).isEmpty()) {
                continue; // not in the line of the spear
            }

            CompoundTag targetData = target.getPersistentData();
            if (targetData.contains(KEY_HIT) && now - targetData.getLong(KEY_HIT) < HIT_COOLDOWN_TICKS) {
                continue;
            }

            Vec3 tv = target.getDeltaMovement();
            double closingSpeed = forwardSpeed - (tv.x * look.x + tv.z * look.z) / horizontal * 20.0;
            if (closingSpeed < MIN_SPEED) {
                continue;
            }

            double bonus = Math.min(MAX_SPEED_BONUS, (closingSpeed - (MIN_SPEED - 2.0)) * SPEED_DAMAGE_FACTOR);
            float damage = (float) (1.0F + weaponDamage + bonus);
            damage = EnchantmentHelper.modifyDamage(serverLevel, stack, target, source, damage);

            if (target.hurt(source, damage)) {
                targetData.putLong(KEY_HIT, now);
                target.knockback(0.4 + Math.min(1.5, closingSpeed * 0.05), -look.x, -look.z);
                EnchantmentHelper.doPostAttackEffects(serverLevel, target, source);
                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_STRONG, user.getSoundSource(), 1.0F, 1.0F);
                stack.hurtAndBreak(1, user, slot);
                if (stack.isEmpty()) {
                    return;
                }
            }
        }
    }
}
