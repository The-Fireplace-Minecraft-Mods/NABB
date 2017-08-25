package the_fireplace.nabb;

import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.EnumBeeType;
import net.minecraft.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import the_fireplace.nabb.network.BeePopMessage;
import the_fireplace.nabb.network.PacketDispatcher;

/**
 * @author The_Fireplace
 */
@Mod(modid=NABB.MODID, name=NABB.MODNAME, dependencies = "required-after:forestry")
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
        ItemStack stack1 = event.getEntityLiving().getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        ItemStack stack2 = event.getEntityLiving().getItemStackFromSlot(EntityEquipmentSlot.OFFHAND);
        if(stack2.isEmpty() || stack1.isEmpty()) {
            return;
        }
        if(BeeManager.beeRoot.getType(stack1) != null && BeeManager.beeRoot.getType(stack2) != null) {
            if((BeeManager.beeRoot.isDrone(stack1) && BeeManager.beeRoot.getType(stack2) == EnumBeeType.PRINCESS) || (BeeManager.beeRoot.isDrone(stack2) && BeeManager.beeRoot.getType(stack1) == EnumBeeType.PRINCESS)) {
                if (event.getEntityLiving().world.isRemote) {
                    flag = true;
                }
            }
        }
    }

    private static boolean reverse = false;
    private static boolean flag = false;
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
                    PacketDispatcher.sendToServer(new BeePopMessage());
                }
            }
        }
    }

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
}
