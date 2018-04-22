package the_fireplace.nabb;

import com.mojang.authlib.GameProfile;
import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.EnumBeeType;
import forestry.api.apiculture.IBee;
import forestry.apiculture.ModuleApiculture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import the_fireplace.nabb.network.PacketDispatcher;
import the_fireplace.nabb.network.PlayBeeAnimation;

/**
 * @author The_Fireplace
 */
@Mod(modid=NABB.MODID, name=NABB.MODNAME, dependencies = "required-after:forestry", guiFactory = "the_fireplace.nabb.NABBConfigFactory", acceptedMinecraftVersions = "[1.12,)")
@Mod.EventBusSubscriber
public class NABB {
    public static final String MODID = "nabb";
    public static final String MODNAME = "New Age Bee Breeding";

    private static final ResourceLocation location = new ResourceLocation(MODID, "bee_squelch");
    public static final SoundEvent beeSquelchSound = new SoundEvent(location).setRegistryName(location);

    @SidedProxy(clientSide = "the_fireplace."+MODID+".client.ClientProxy", serverSide = "the_fireplace."+MODID+".CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event){
        PacketDispatcher.registerPackets();
    }

    @SubscribeEvent
    public static void registerSound(RegistryEvent.Register<SoundEvent> event){
    	event.getRegistry().register(beeSquelchSound);
    }

    @SubscribeEvent
    public static void itemRightClick(PlayerInteractEvent.RightClickItem event){
        if(event.getWorld().isRemote)
            return;

        ItemStack stack1 = event.getEntityLiving().getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        ItemStack stack2 = event.getEntityLiving().getItemStackFromSlot(EntityEquipmentSlot.OFFHAND);
        if(stack2.isEmpty() || stack1.isEmpty())
            return;
        if(BeeManager.beeRoot.getType(stack1) != null && BeeManager.beeRoot.getType(stack2) != null) {
            if(ConfigValues.recreational_bee_smushing || ((BeeManager.beeRoot.isDrone(stack1) && BeeManager.beeRoot.getType(stack2) == EnumBeeType.PRINCESS) || (BeeManager.beeRoot.isDrone(stack2) && BeeManager.beeRoot.getType(stack1) == EnumBeeType.PRINCESS))) {
                new Thread(()->{
                    if(!ConfigValues.instant_smush) {
                        PacketDispatcher.sendTo(new PlayBeeAnimation(), (EntityPlayerMP) event.getEntityPlayer());
                        try {
                            Thread.sleep(32 * 3 * 50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
                        ItemStack stack1a = stack1.copy();
                        ItemStack stack2a = stack2.copy();
                        EntityPlayer player = event.getEntityPlayer();
                        if (BeeManager.beeRoot.getType(stack1a) != null && BeeManager.beeRoot.getType(stack2a) != null && !player.world.isRemote) {
                            if ((BeeManager.beeRoot.isDrone(stack1a) && BeeManager.beeRoot.getType(stack2a) == EnumBeeType.PRINCESS)) {
                                player.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, breed(stack1a, stack2a, player.world, player.getGameProfile()));
                                player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
                            } else if ((BeeManager.beeRoot.isDrone(stack2a) && BeeManager.beeRoot.getType(stack1a) == EnumBeeType.PRINCESS)) {
                                player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, breed(stack2a, stack1a, player.world, player.getGameProfile()));
                                player.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, ItemStack.EMPTY);
                            } else if(NABB.ConfigValues.recreational_bee_smushing_kills) {
                                player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
                                player.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, ItemStack.EMPTY);
                            }
                        }
                        player.world.playSound(player, player.getPosition(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
                    });
                }).start();
            }
        }
    }

    @SubscribeEvent
    public static void configChanged(ConfigChangedEvent.OnConfigChangedEvent event){
    	if(event.getModID().equals(MODID))
		    ConfigManager.sync(MODID, Config.Type.INSTANCE);
    }

    @Config(modid=MODID, name=MODNAME)
    public static class ConfigValues {
    	@Config.Comment("Enabling this lets you smush any two bees together. Queens will only be produced if the two bees being smushed can produce a Queen.")
	    @Config.LangKey("recreational_bee_smushing")
    	public static boolean recreational_bee_smushing = false;
    	@Config.Comment("Does Recreational Bee Smushing kill the bees?")
	    @Config.LangKey("recreational_bee_smushing_kills")
	    public static boolean recreational_bee_smushing_kills = true;
    	@Config.Comment("If this is true, bees will be smushed instantly, with no animation.")
        @Config.LangKey("instant_smush")
    	public static boolean instant_smush = false;
    }

    /***********************************************
     **   Client Animation Code past this point   **
     ***********************************************/

    private static boolean reverse = false;
    public static boolean flag = false;
    private static int iter = 0;
    private static int ticksUsing = 0;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void renderHand(RenderHandEvent event){
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
                Minecraft.getMinecraft().world.playSound(Minecraft.getMinecraft().player.getPosition(), beeSquelchSound, SoundCategory.PLAYERS, 1.0F, 0.5F+Minecraft.getMinecraft().world.rand.nextFloat(), false);
                reverse = true;
            }else if(ticksUsing <= 0){
                Minecraft.getMinecraft().world.playSound(Minecraft.getMinecraft().player.getPosition(), beeSquelchSound, SoundCategory.PLAYERS, 1.0F, 0.5F+Minecraft.getMinecraft().world.rand.nextFloat(), false);
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

    @SideOnly(Side.CLIENT)
    private static void transformEatFirstPerson(float partialTicks, EnumHandSide handSide)
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

    public static ItemStack breed(ItemStack droneStack, ItemStack princessStack, World world, GameProfile player){
        IBee princess = BeeManager.beeRoot.getMember(princessStack);
        IBee drone = BeeManager.beeRoot.getMember(droneStack);
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        if(princess != null && drone != null) {
            princess.mate(drone);

            princess.writeToNBT(nbttagcompound);
        }
        ItemStack queenStack = new ItemStack(ModuleApiculture.getItems().beeQueenGE);
        queenStack.setTagCompound(nbttagcompound);

        // Register the new queen with the breeding tracker
        if(princess != null)
            BeeManager.beeRoot.getBreedingTracker(world, player).registerQueen(princess);

        return queenStack;
    }
}
