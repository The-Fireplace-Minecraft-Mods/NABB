package the_fireplace.nabb;

import com.mojang.authlib.GameProfile;
import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.EnumBeeType;
import forestry.api.apiculture.IBee;
import forestry.apiculture.PluginApiculture;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * @author The_Fireplace
 */
@Mod(modid="nabb", name="New Age Bee Breeding")
public class NABB {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event){
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void itemRightClick(PlayerInteractEvent.RightClickItem event){
        ItemStack stack1 = event.getEntityLiving().getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        ItemStack stack2 = event.getEntityLiving().getItemStackFromSlot(EntityEquipmentSlot.OFFHAND);
        if(event.getItemStack() == null || event.getEntityLiving().worldObj.isRemote || stack2 == null || stack1 == null)
            return;
        stack1 = stack1.copy();
        stack2 = stack2.copy();
        if(BeeManager.beeRoot.getType(stack1) != null && BeeManager.beeRoot.getType(stack2) != null){
            if((BeeManager.beeRoot.isDrone(stack1) && BeeManager.beeRoot.getType(stack2) == EnumBeeType.PRINCESS)){
                event.getEntityLiving().setItemStackToSlot(EntityEquipmentSlot.OFFHAND, breed(stack1, stack2, event.getWorld(), event.getEntityPlayer().getGameProfile()));
                event.getEntityLiving().setItemStackToSlot(EntityEquipmentSlot.MAINHAND, null);
            }else if((BeeManager.beeRoot.isDrone(stack2) && BeeManager.beeRoot.getType(stack1) == EnumBeeType.PRINCESS)){
                event.getEntityLiving().setItemStackToSlot(EntityEquipmentSlot.MAINHAND, breed(stack2, stack1, event.getWorld(), event.getEntityPlayer().getGameProfile()));
                event.getEntityLiving().setItemStackToSlot(EntityEquipmentSlot.OFFHAND, null);
            }
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
}
