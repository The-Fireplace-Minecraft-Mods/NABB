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
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author The_Fireplace
 */
@Mod(modid="nabb", name="New Age Bee Breeding")
public class NABB {
    @Mod.Instance("nabb")
    public static NABB instance;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event){
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void itemRightClick(PlayerInteractEvent.RightClickItem event){
        ItemStack stack1 = event.getEntityLiving().getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        ItemStack stack2 = event.getEntityLiving().getItemStackFromSlot(EntityEquipmentSlot.OFFHAND);
        if(event.getItemStack() == null || stack2 == null || stack1 == null)
            return;
        stack1 = stack1.copy();
        stack2 = stack2.copy();
        if(BeeManager.beeRoot.getType(stack1) != null && BeeManager.beeRoot.getType(stack2) != null && !event.getEntityLiving().worldObj.isRemote){
            if((BeeManager.beeRoot.isDrone(stack1) && BeeManager.beeRoot.getType(stack2) == EnumBeeType.PRINCESS)){
                event.getEntityLiving().setItemStackToSlot(EntityEquipmentSlot.OFFHAND, breed(stack1, stack2, event.getWorld(), event.getEntityPlayer().getGameProfile()));
                event.getEntityLiving().setItemStackToSlot(EntityEquipmentSlot.MAINHAND, null);
                return;
            }else if((BeeManager.beeRoot.isDrone(stack2) && BeeManager.beeRoot.getType(stack1) == EnumBeeType.PRINCESS)){
                event.getEntityLiving().setItemStackToSlot(EntityEquipmentSlot.MAINHAND, breed(stack2, stack1, event.getWorld(), event.getEntityPlayer().getGameProfile()));
                event.getEntityLiving().setItemStackToSlot(EntityEquipmentSlot.OFFHAND, null);
                return;
            }
        }
        if(event.getEntityLiving().worldObj.isRemote){
            flag = true;
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
    private boolean a1 = false;
    private boolean a2 = false;
    private boolean a3 = false;
    public int ticksUsing = 0;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void renderHand(RenderHandEvent event){
        if(flag){
            //TODO Hand move code here

            //End hand move code
            if(!reverse)
                ticksUsing++;
            else
                ticksUsing--;
        }
        if(ticksUsing >= 32){
            reverse = true;
        }else if(ticksUsing <= 0){
            reverse = false;
            if(!a1)
                a1=true;
            else if(!a2)
                a2=true;
            else if(!a3)
                a3=true;
            else{
                flag=false;
                a1=false;
                a2=false;
                a3=false;
            }
        }
    }
}
