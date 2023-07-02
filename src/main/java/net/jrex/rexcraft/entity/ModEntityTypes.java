package net.jrex.rexcraft.entity;

import net.jrex.rexcraft.RexCraft;
import net.jrex.rexcraft.entity.custom.BucklandiiEntity;
import net.jrex.rexcraft.entity.custom.CricketEntity;
import net.jrex.rexcraft.entity.custom.GeckoEntity;
import net.jrex.rexcraft.entity.custom.HedgyEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RexCraft.MOD_ID);

    public static final RegistryObject<EntityType<GeckoEntity>> GECKO =
            ENTITY_TYPES.register("gecko",
                    () -> EntityType.Builder.of(GeckoEntity::new, MobCategory.CREATURE)
                            //Hitbox Size!
                            .sized(0.4f, 0.3f)
                            .build(new ResourceLocation(RexCraft.MOD_ID, "gecko").toString()));

    public static final RegistryObject<EntityType<HedgyEntity>> HEDGY =
            ENTITY_TYPES.register("hedgy",
                    () -> EntityType.Builder.of(HedgyEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 0.6f)
                            .build(new ResourceLocation(RexCraft.MOD_ID, "hedgy").toString()));

    public static final RegistryObject<EntityType<CricketEntity>> CRICKET =
            ENTITY_TYPES.register("cricket",
                    () -> EntityType.Builder.of(CricketEntity::new, MobCategory.CREATURE)
                            .sized(0.1f, 0.1f)
                            .build(new ResourceLocation(RexCraft.MOD_ID, "cricket").toString()));

    public static final RegistryObject<EntityType<BucklandiiEntity>> BUCKLANDII =
            ENTITY_TYPES.register("bucklandii",
                    () -> EntityType.Builder.of(BucklandiiEntity::new, MobCategory.CREATURE)
                            //Hitbox Size!
                            .sized(0.9f, 0.9f)
                            .build(new ResourceLocation(RexCraft.MOD_ID, "bucklandii").toString()));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
