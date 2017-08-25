package the_fireplace.nabb.network;

import com.mojang.authlib.GameProfile;
import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.EnumBeeType;
import forestry.api.apiculture.IBee;
import forestry.apiculture.PluginApiculture;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import the_fireplace.nabb.NABB;

/**
 * @author The_Fireplace
 */
public class BeePopMessage implements IMessage {

    public BeePopMessage() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {

    }

    @Override
    public void toBytes(ByteBuf buf) {

    }

    public static class Handler extends AbstractServerMessageHandler<BeePopMessage> {
        @Override
        public IMessage handleServerMessage(EntityPlayer player, BeePopMessage message, MessageContext ctx) {
            FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
                ItemStack stack1 = player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
                ItemStack stack2 = player.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND);
                if(stack2.isEmpty() || stack1.isEmpty()) {
                    return;
                }
                stack1 = stack1.copy();
                stack2 = stack2.copy();
                if (BeeManager.beeRoot.getType(stack1) != null && BeeManager.beeRoot.getType(stack2) != null && !player.world.isRemote) {
                    if ((BeeManager.beeRoot.isDrone(stack1) && BeeManager.beeRoot.getType(stack2) == EnumBeeType.PRINCESS)) {
                        player.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, breed(stack1, stack2, player.world, player.getGameProfile()));
                        player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    } else if ((BeeManager.beeRoot.isDrone(stack2) && BeeManager.beeRoot.getType(stack1) == EnumBeeType.PRINCESS)) {
                        player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, breed(stack2, stack1, player.world, player.getGameProfile()));
                        player.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    } else if(NABB.ConfigValues.recreational_bee_smushing_kills) {
	                    player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
	                    player.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    }
                }
                player.world.playSound(player, player.getPosition(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
            });
            return null;
        }

        public ItemStack breed(ItemStack droneStack, ItemStack princessStack, World world, GameProfile player){
            IBee princess = BeeManager.beeRoot.getMember(princessStack);
            IBee drone = BeeManager.beeRoot.getMember(droneStack);
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            if(princess != null && drone != null) {
                princess.mate(drone);

                princess.writeToNBT(nbttagcompound);
            }
            ItemStack queenStack = new ItemStack(PluginApiculture.getItems().beeQueenGE);
            queenStack.setTagCompound(nbttagcompound);

            // Register the new queen with the breeding tracker
            BeeManager.beeRoot.getBreedingTracker(world, player).registerQueen(princess);

            return queenStack;
        }
    }
}
