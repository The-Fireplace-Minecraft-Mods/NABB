package the_fireplace.nabb;

import com.mojang.authlib.GameProfile;
import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.EnumBeeType;
import forestry.api.apiculture.IBee;
import forestry.apiculture.PluginApiculture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author The_Fireplace
 */
@Mod(modid=NABB.MODID, name=NABB.MODNAME, dependencies = "required-after:forestry")
public class NABB {
    public static final String MODID = "nabb";
    public static final String MODNAME = "New Age Bee Breeding";

    ResourceLocation location = new ResourceLocation(MODID, "bee_squelch");
    SoundEvent beeSquelchSound = new SoundEvent(location);

    @Mod.EventHandler
    public void init(FMLInitializationEvent event){
        GameRegistry.register(beeSquelchSound, location);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void itemRightClick(PlayerInteractEvent.RightClickItem event){
        ItemStack stack1 = event.getEntityLiving().getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        ItemStack stack2 = event.getEntityLiving().getItemStackFromSlot(EntityEquipmentSlot.OFFHAND);
        if(stack2 == null || stack1 == null) {
            return;
        }
        if(BeeManager.beeRoot.getType(stack1) != null && BeeManager.beeRoot.getType(stack2) != null) {
            if((BeeManager.beeRoot.isDrone(stack1) && BeeManager.beeRoot.getType(stack2) == EnumBeeType.PRINCESS) || (BeeManager.beeRoot.isDrone(stack2) && BeeManager.beeRoot.getType(stack1) == EnumBeeType.PRINCESS)) {
                if (event.getEntityLiving().world.isRemote && !popBee) {
                    flag = true;
                }
                popBee = true;
            }
        }
    }

    private int ticksWaiting = 0;
    private boolean popBee = false;
    @SubscribeEvent
    public void livingUpdate(LivingEvent.LivingUpdateEvent event){
        if(!event.getEntityLiving().getEntityWorld().isRemote && event.getEntityLiving() instanceof EntityPlayer && popBee){
            ItemStack stack1 = event.getEntityLiving().getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
            ItemStack stack2 = event.getEntityLiving().getItemStackFromSlot(EntityEquipmentSlot.OFFHAND);
            if(stack2 == null || stack1 == null) {
                popBee = false;
                ticksWaiting = 0;
                return;
            }
            if(ticksWaiting >= 130) {
                stack1 = stack1.copy();
                stack2 = stack2.copy();
                if (BeeManager.beeRoot.getType(stack1) != null && BeeManager.beeRoot.getType(stack2) != null && !event.getEntityLiving().world.isRemote) {
                    if ((BeeManager.beeRoot.isDrone(stack1) && BeeManager.beeRoot.getType(stack2) == EnumBeeType.PRINCESS)) {
                        event.getEntityLiving().setItemStackToSlot(EntityEquipmentSlot.OFFHAND, breed(stack1, stack2, ((EntityPlayer) event.getEntityLiving()).world, ((EntityPlayer) event.getEntityLiving()).getGameProfile()));
                        event.getEntityLiving().setItemStackToSlot(EntityEquipmentSlot.MAINHAND, null);
                    } else if ((BeeManager.beeRoot.isDrone(stack2) && BeeManager.beeRoot.getType(stack1) == EnumBeeType.PRINCESS)) {
                        event.getEntityLiving().setItemStackToSlot(EntityEquipmentSlot.MAINHAND, breed(stack2, stack1, ((EntityPlayer) event.getEntityLiving()).world, ((EntityPlayer) event.getEntityLiving()).getGameProfile()));
                        event.getEntityLiving().setItemStackToSlot(EntityEquipmentSlot.OFFHAND, null);
                    }
                }
                Minecraft.getMinecraft().world.playSound(Minecraft.getMinecraft().player.getPosition(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F, false);
                popBee = false;
                ticksWaiting = 0;
            }
            ticksWaiting++;
        }
    }

    public ItemStack breed(ItemStack droneStack, ItemStack princessStack, World world, GameProfile player){
        IBee princess = BeeManager.beeRoot.getMember(princessStack);
        IBee drone = BeeManager.beeRoot.getMember(droneStack);
        princess.mate(drone);

        NBTTagCompound nbttagcompound = new NBTTagCompound();
        princess.writeToNBT(nbttagcompound);
        ItemStack queenStack = new ItemStack(PluginApiculture.items.beeQueenGE);
        queenStack.setTagCompound(nbttagcompound);

        // Register the new queen with the breeding tracker
        BeeManager.beeRoot.getBreedingTracker(world, player).registerQueen(princess);

        return queenStack;
    }

    private boolean reverse = false;
    private boolean flag = false;
    private int iter = 0;
    private int ticksUsing = 0;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void renderHand(RenderHandEvent event){
        if(flag){
            if(iter % 2 == 0)
                transformEatFirstPerson(event.getPartialTicks(), EnumHandSide.LEFT);
            else
                transformEatFirstPerson(event.getPartialTicks(), EnumHandSide.RIGHT);
            //End hand move code
            if(!reverse)
                ticksUsing++;
            else
                ticksUsing--;

            if(ticksUsing >= 32){
                Minecraft.getMinecraft().world.playSound(Minecraft.getMinecraft().player.getPosition(), this.beeSquelchSound, SoundCategory.PLAYERS, 1.0F, 0.5F+Minecraft.getMinecraft().world.rand.nextFloat(), false);
                reverse = true;
            }else if(ticksUsing <= 0){
                Minecraft.getMinecraft().world.playSound(Minecraft.getMinecraft().player.getPosition(), this.beeSquelchSound, SoundCategory.PLAYERS, 1.0F, 0.5F+Minecraft.getMinecraft().world.rand.nextFloat(), false);
                reverse = false;
                if(iter < 2)
                    iter++;
                else{
                    flag=false;
                    iter = 0;
                }
            }
        }
    }

    private void transformEatFirstPerson(float partialTicks, EnumHandSide handSide)
    {
        float f = (float)ticksUsing - partialTicks + 1.0F;
        float f1 = f / (float)33;

        float f3 = 1.0F - (float)Math.pow((double)f1, 27.0D);
        int i = handSide == EnumHandSide.RIGHT ? 1 : -1;
        GlStateManager.translate(f3 * 0.6F * (float)i, f3 * -0.5F, f3 * 0.0F);
        GlStateManager.rotate((float)i * f3 * 90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f3 * 10.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate((float)i * f3 * 30.0F, 0.0F, 0.0F, 1.0F);
    }
}
