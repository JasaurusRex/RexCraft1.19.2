package net.jrex.rexcraft.entity.custom;

import net.jrex.rexcraft.entity.ModEntityTypes;
import net.jrex.rexcraft.entity.variant.BucklandiiVariant;
import net.jrex.rexcraft.item.ModItems;
import net.jrex.rexcraft.sound.ModSounds;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.AnimationState;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

//Things for being angry:
//Use these things to be able to make this mob angry, make sure you have everything or it will not work.
import net.minecraft.world.entity.NeutralMob;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;

//TamableAnimal
public class BucklandiiEntity extends TamableAnimal implements IAnimatable, NeutralMob, PlayerRideableJumping, Saddleable {

    private static final EntityDataAccessor<Boolean> SITTING =
            SynchedEntityData.defineId(BucklandiiEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT =
            SynchedEntityData.defineId(BucklandiiEntity.class, EntityDataSerializers.INT);

    public static final Predicate<LivingEntity> PREY_SELECTOR = (p_30437_) -> {
        EntityType<?> entitytype = p_30437_.getType();
        return entitytype == EntityType.VILLAGER || entitytype == EntityType.COW || entitytype == EntityType.SHEEP || entitytype == EntityType.PIG
                || entitytype == EntityType.LLAMA || entitytype == EntityType.HORSE;
    };

    private static final EntityDataAccessor<Integer> DATA_REMAINING_ANGER_TIME = SynchedEntityData.defineId(BucklandiiEntity.class, EntityDataSerializers.INT);

    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 40);

    private static final EntityDataAccessor<Byte> DATA_ID_FLAGS = SynchedEntityData.defineId(BucklandiiEntity.class, EntityDataSerializers.BYTE);

    private static final int FLAG_SADDLE = 4;

    @Nullable
    private UUID persistentAngerTarget;

    //private int destroyBlocksTick;

    private AnimationFactory factory = new AnimationFactory(this);



    public BucklandiiEntity(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }


    @Nullable



    public static AttributeSupplier setAttributes() {

        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0f)
                .add(Attributes.ATTACK_SPEED, 1.0f)
                .add(Attributes.MOVEMENT_SPEED, 0.2f).build();
    }

    @Override
    protected void registerGoals() {

            this.goalSelector.addGoal(1, new FloatGoal(this));
            this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
            this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 2.0D, 10.0F, 6.0F, false));
            this.goalSelector.addGoal(2, new FollowParentGoal(this, 1.1D));
            this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 2.0D, false));
            this.goalSelector.addGoal(3, new BreedGoal(this, 1.0D));
            this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
            this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
            this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

            this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
            this.targetSelector.addGoal(3, (new HurtByTargetGoal(this)).setAlertOthers());
            //this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
            //this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Cow.class, true));
            //this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Pig.class, true));
            //this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Sheep.class, true));
            this.targetSelector.addGoal(4, new NonTameRandomTargetGoal<>(this, LivingEntity.class, false, PREY_SELECTOR));
            this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, true));
    }


    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        if (event.isMoving()) {
            if(this.isVehicle()){
                event.getController().setAnimation(new AnimationBuilder().addAnimation("vehicle_walk", true));
                return PlayState.CONTINUE;
            }
            else{
                event.getController().setAnimation(new AnimationBuilder().addAnimation("walk", true));
                return PlayState.CONTINUE;
            }

        }

        //if(this.isSprinting())
        if (this.isSitting()) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("sitting", true));
            return PlayState.CONTINUE;
        }

        //So this is working but not how I want it to. It's changing the number every tick and therefore cannot choose an animation.
        //I need to find a way to have it only call this when the animation ends. Maybe make a separate method to call?

        //Random rand = new Random();

        //int upperbound = 2;

        //int rand_int = rand.nextInt(upperbound);

        //if(event.getController().getCurrentAnimation() != null){
            //System.out.println("Animation Name = " + event.getController().getCurrentAnimation().toString());
        //}
        //System.out.println("Animation Name = " + event.getController().getCurrentAnimation().toString());

        event.getController().setAnimation(new AnimationBuilder().addAnimation("idle", true));
        return PlayState.CONTINUE;

//        if(event.getController().getCurrentAnimation().toString().equals("idle1")) {
//            event.getController().markNeedsReload();
//        }
        //return PlayState.CONTINUE;

    }

    //this could be useful!
    //can confirm, VERY useful!
    private PlayState attackPredicate(AnimationEvent event) {

        if(this.swinging && event.getController().getAnimationState().equals(AnimationState.Stopped)){
            event.getController().markNeedsReload();

            Random rand = new Random();

            int upperbound = 3;

            int rand_int = rand.nextInt(upperbound);


            //System.out.println("Attacking!");
            //this.playSound(ModSounds.BUCKLANDII_HURT.get(),0.9f,1.0f);
            //this.playSound(SoundEvents.CHICKEN_HURT,0.9f,1.0f);
            event.getController().setAnimation(new AnimationBuilder().addAnimation("attack" + rand_int, false));

            this.swinging = false;
        }

       // this.playSound(ModSounds.BUCKLANDII_HURT.get(),0.9f,1.0f);

        return PlayState.CONTINUE;
    }

//    @Deprecated //Forge: DO NOT USE use BlockState.canEntityDestroy
//    public static boolean canDestroy(BlockState pState) {
//        return !pState.isAir() && !pState.is(BlockTags.WITHER_IMMUNE);
//    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob mob) {
        BucklandiiEntity baby = ModEntityTypes.BUCKLANDII.get().create(serverLevel);
        BucklandiiVariant variant = Util.getRandom(BucklandiiVariant.values(), this.random);
        baby.setVariant(variant);
        return baby;
    }

    @Override
    public boolean isFood(ItemStack pStack){
        Item item = pStack.getItem();
        return item.isEdible() && pStack.getFoodProperties(this).isMeat();
    }

    //Used as the healing item, in the case of the gecko it's a cricket
    //look into wolf class to see how meat works
    public boolean isHeal(ItemStack pStack){
        return pStack.getItem() == ModItems.BLUEBERRY.get();
    }


    //DATA_ID_TYPE_VARIANT
//    public int getGeckoType() {
//        return this.entityData.get(DATA_ID_TYPE_VARIANT);
//    }


    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController(this,"controller",0,this::predicate));
        data.addAnimationController(new AnimationController(this,"attackController",0,this::attackPredicate));

    }


    @Override
    public AnimationFactory getFactory() {
        return factory;
    }


    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(SoundEvents.GRASS_STEP, 0.15F, 4.0F);
    }


    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.BUCKLANDII_HURT.get();
    }

    protected SoundEvent getDeathSound() {
        return ModSounds.BUCKLANDII_DEATH.get();
    }

    protected SoundEvent getAmbientSound() {

        if(this.isAngry()){
            return ModSounds.BUCKLANDII_ANGRY.get();
        }
        else {
            return ModSounds.BUCKLANDII_GROWL.get();
        }
    }

    protected SoundEvent getSwimSound() {
        return SoundEvents.GENERIC_SWIM;
    }

    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.GENERIC_SPLASH;
    }

    protected float getSoundVolume() {
        return 0.8F;
    }

    protected boolean isImmobile() {
        return super.isImmobile() && this.isVehicle() && this.isSaddled();
    }

    public void aiStep() {
        super.aiStep();

        if (!this.level.isClientSide && this.isAlive()) {
            if (this.random.nextInt(900) == 0 && this.deathTime == 0) {
                this.heal(1.0F);
            }
        }

        if (!this.level.isClientSide) {
            this.updatePersistentAnger((ServerLevel)this.level, true);
        }

    }

    protected void doPlayerRide(Player pPlayer) {
        if (!this.level.isClientSide) {
            pPlayer.setYRot(this.getYRot());
            pPlayer.setXRot(this.getXRot());
            pPlayer.startRiding(this);
        }

    }

    protected void updateContainerEquipment() {
        if (!this.level.isClientSide) {
            this.setFlag(4, this.isSaddled());
        }

    }

    public void positionRider(@NotNull Entity pPassenger) {
        if (this.hasPassenger(pPassenger)) {
            float f = Mth.cos(this.yBodyRot * ((float)Math.PI / 180F));
            float f1 = Mth.sin(this.yBodyRot * ((float)Math.PI / 180F));

            //offset upwards
            float f2 = 0.3F;
            pPassenger.setPos(this.getX() + (double)(0.3F * f1), this.getY() + this.getPassengersRidingOffset() + pPassenger.getMyRidingOffset() + f2, this.getZ() - (double)(0.3F * f));
        }
    }

    @javax.annotation.Nullable
    public LivingEntity getControllingPassenger() {
        if (this.isSaddled()) {
            Entity entity = this.getFirstPassenger();
            if (entity instanceof LivingEntity) {
                return (LivingEntity)entity;
            }
        }

        return null;
    }

    @javax.annotation.Nullable
    private Vec3 getDismountLocationInDirection(Vec3 pDirection, LivingEntity pPassenger) {
        double d0 = this.getX() + pDirection.x;
        double d1 = this.getBoundingBox().minY;
        double d2 = this.getZ() + pDirection.z;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for(Pose pose : pPassenger.getDismountPoses()) {
            blockpos$mutableblockpos.set(d0, d1, d2);
            double d3 = this.getBoundingBox().maxY + 0.75D;

            while(true) {
                double d4 = this.level.getBlockFloorHeight(blockpos$mutableblockpos);
                if ((double)blockpos$mutableblockpos.getY() + d4 > d3) {
                    break;
                }

                if (DismountHelper.isBlockFloorValid(d4)) {
                    AABB aabb = pPassenger.getLocalBoundsForPose(pose);
                    Vec3 vec3 = new Vec3(d0, (double)blockpos$mutableblockpos.getY() + d4, d2);
                    if (DismountHelper.canDismountTo(this.level, pPassenger, aabb.move(vec3))) {
                        pPassenger.setPose(pose);
                        return vec3;
                    }
                }

                blockpos$mutableblockpos.move(Direction.UP);
                if (!((double)blockpos$mutableblockpos.getY() < d3)) {
                    break;
                }
            }
        }

        return null;
    }

    public @NotNull Vec3 getDismountLocationForPassenger(LivingEntity pLivingEntity) {
        Vec3 vec3 = getCollisionHorizontalEscapeVector((double)this.getBbWidth(), (double)pLivingEntity.getBbWidth(), this.getYRot() + (pLivingEntity.getMainArm() == HumanoidArm.RIGHT ? 90.0F : -90.0F));
        Vec3 vec31 = this.getDismountLocationInDirection(vec3, pLivingEntity);
        if (vec31 != null) {
            return vec31;
        } else {
            Vec3 vec32 = getCollisionHorizontalEscapeVector((double)this.getBbWidth(), (double)pLivingEntity.getBbWidth(), this.getYRot() + (pLivingEntity.getMainArm() == HumanoidArm.LEFT ? 90.0F : -90.0F));
            Vec3 vec33 = this.getDismountLocationInDirection(vec32, pLivingEntity);
            return vec33 != null ? vec33 : this.position();
        }
    }


    /* TAMEABLE */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();

        Item itemForTaming = Items.EGG;

        //if the item "isFood", just use for taming
        if(isFood(itemstack)){
            return super.mobInteract(player, hand);
        }

        //if the item "isHeal" and the current health is less than the max health of the mob, eat the food and heal
        if(this.isHeal(itemstack) && this.getHealth() < this.getMaxHealth()){
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            this.heal(2);
            this.gameEvent(GameEvent.EAT, this);
            this.spawnTamingParticles(true);
            return InteractionResult.SUCCESS;

        }

        if (item == itemForTaming && !isTame()) {
            if (this.level.isClientSide) {
                return InteractionResult.CONSUME;
            } else {
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }

                if (!ForgeEventFactory.onAnimalTame(this, player)) {
                    if (!this.level.isClientSide) {
                        super.tame(player);
                        this.navigation.recomputePath();
                        this.setTarget(null);
                        this.level.broadcastEntityEvent(this, (byte)7);
                        setSitting(true);
                    }
                }

                return InteractionResult.SUCCESS;
            }
        }

        if (this.isVehicle()) {
            return super.mobInteract(player, hand);
        }

        if(item == Items.SADDLE && this.isTame() && !this.level.isClientSide && this.isSaddleable() && !this.isSaddled()){
            itemstack.shrink(1);
            this.equipSaddle(SoundSource.NEUTRAL);
            return InteractionResult.SUCCESS;
        }

        if(isTame() && !this.level.isClientSide && hand == InteractionHand.MAIN_HAND && player.isCrouching()) {
            setSitting(!isSitting());
            return InteractionResult.SUCCESS;
        }

        if(isTame() && !this.level.isClientSide && hand == InteractionHand.MAIN_HAND && !this.isSitting()) {
            this.doPlayerRide(player);
            return InteractionResult.SUCCESS;
        }

        if (itemstack.getItem() == itemForTaming) {
            return InteractionResult.PASS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSitting(tag.getBoolean("isSitting"));
        this.entityData.set(DATA_ID_TYPE_VARIANT,tag.getInt("Variant"));

        //check for saddle
        this.updateContainerEquipment();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("isSitting", this.isSitting());
        tag.putInt("Variant",this.getTypeVariant());
        //this.updateContainerEquipment();
        if (this.isSaddled()) {
            tag.put("SaddleItem", new CompoundTag());
        }
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SITTING, false);
        this.entityData.define(DATA_ID_TYPE_VARIANT,0);
        this.entityData.define(DATA_REMAINING_ANGER_TIME, 0);
        this.entityData.define(DATA_ID_FLAGS, (byte)0);
    }

    protected boolean getFlag(int pFlagId) {
        return (this.entityData.get(DATA_ID_FLAGS) & pFlagId) != 0;
    }

    protected void setFlag(int pFlagId, boolean pValue) {
        byte b0 = this.entityData.get(DATA_ID_FLAGS);
        if (pValue) {
            this.entityData.set(DATA_ID_FLAGS, (byte)(b0 | pFlagId));
        } else {
            this.entityData.set(DATA_ID_FLAGS, (byte)(b0 & ~pFlagId));
        }

    }

    public void setSitting(boolean sitting) {
        this.entityData.set(SITTING, sitting);
        this.setOrderedToSit(sitting);
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING);
    }

    @Override
    public Team getTeam() {
        return super.getTeam();
    }

    public boolean canBeLeashed(Player player) {
        return true;
    }

    @Override
    public void setTame(boolean tamed) {
        super.setTame(tamed);
        if (tamed) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(18.0D);
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.3D);
            getAttribute(Attributes.ATTACK_SPEED).setBaseValue(1.0f);
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2f);


        } else {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(15.0D);
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4D);
            getAttribute(Attributes.ATTACK_SPEED).setBaseValue(1.0f);
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2f);
        }
    }

    public void travel(@NotNull Vec3 pTravelVector) {

        if (this.isAlive()) {
            LivingEntity livingentity = this.getControllingPassenger();
            if (this.isVehicle() && livingentity != null) {
                this.setYRot(livingentity.getYRot());
                this.yRotO = this.getYRot();
                this.setXRot(livingentity.getXRot() * 0.5F);
                this.setRot(this.getYRot(), this.getXRot());
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.yBodyRot;
                float f = livingentity.xxa * 0.5F;
                float f1 = livingentity.zza;

                if (this.onGround) {
                    Vec3 vec3 = this.getDeltaMovement();
                    this.setDeltaMovement(vec3.x, 0, vec3.z);
                }

                if (this.isControlledByLocalInstance()) {
                    this.setSpeed((float)this.getAttributeValue(Attributes.MOVEMENT_SPEED));
                    super.travel(new Vec3((double)f, pTravelVector.y, (double)f1));
                } else if (livingentity instanceof Player) {
                    this.setDeltaMovement(Vec3.ZERO);
                }


                this.tryCheckInsideBlocks();
            } else {
                this.flyingSpeed = 0.02F;
                super.travel(pTravelVector);
            }
        }
    }

    /* VARIANTS */
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_146746_, DifficultyInstance p_146747_,
                                        MobSpawnType p_146748_, @Nullable SpawnGroupData p_146749_,
                                        @Nullable CompoundTag p_146750_) {
        BucklandiiVariant variant = Util.getRandom(BucklandiiVariant.values(), this.random);
        setVariant(variant);
        return super.finalizeSpawn(p_146746_, p_146747_, p_146748_, p_146749_, p_146750_);
    }

    public BucklandiiVariant getVariant() {
        return BucklandiiVariant.byId(this.getTypeVariant() & 255);
    }

    private int getTypeVariant() {
        return this.entityData.get(DATA_ID_TYPE_VARIANT);
    }

    private void setVariant(BucklandiiVariant variant) {
        this.entityData.set(DATA_ID_TYPE_VARIANT, variant.getId() & 255);
    }

    @Override
    public int getRemainingPersistentAngerTime() {
        return this.entityData.get(DATA_REMAINING_ANGER_TIME);
    }

    @Override
    public void setRemainingPersistentAngerTime(int pTime) {
        this.entityData.set(DATA_REMAINING_ANGER_TIME, pTime);
    }


    @Nullable
    @Override
    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@javax.annotation.Nullable UUID pTarget) {
        this.persistentAngerTarget = pTarget;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    @Override
    public void onPlayerJump(int pJumpPower) {

    }

    @Override
    public boolean canJump() {
        return false;
    }

    @Override
    public void handleStartJump(int pJumpPower) {

    }

    @Override
    public void handleStopJump() {

    }

    @Override
    public boolean isSaddleable() {
        return this.isAlive() && !this.isBaby() && this.isTame();
    }

    @Override
    public void equipSaddle(@javax.annotation.Nullable SoundSource pSource) {
        //this.inventory.setItem(0, new ItemStack(Items.SADDLE));
        //Seeing as it has no inventory, figure out how to equip the saddle by right-clicking the entity
        this.setFlag(FLAG_SADDLE,true);
        if (pSource != null) {
            this.level.playSound((Player)null, this, SoundEvents.HORSE_SADDLE, pSource, 0.5F, 1.0F);
        }

    }

    @Override
    public boolean isSaddled() {
        return this.getFlag(4);
    }

//    /**
//     * Loops through nearby animals and finds another animal of the same type that can be mated with. Returns the first
//     * valid mate found.
//     */
//    @javax.annotation.Nullable
//    private Animal getFreePartner() {
//        List<? extends Animal> list = this.level.getNearbyEntities(this.partnerClass, PARTNER_TARGETING, this.animal, this.animal.getBoundingBox().inflate(8.0D));
//        double d0 = Double.MAX_VALUE;
//        Animal animal = null;
//
//        for(Animal animal1 : list) {
//            if (this.animal.canMate(animal1) && this.animal.distanceToSqr(animal1) < d0) {
//                animal = animal1;
//                d0 = this.animal.distanceToSqr(animal1);
//            }
//        }
//
//        return animal;
//    }
}
