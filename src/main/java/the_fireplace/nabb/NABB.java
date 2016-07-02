package the_fireplace.nabb;

import com.mojang.authlib.GameProfile;
import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.EnumBeeType;
import forestry.api.apiculture.IBee;
import forestry.apiculture.PluginApiculture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
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
    float equippedProgress = 0.0F;
    float prevEquippedProgress = 0.0F;
    public int ticksUsing = 0;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void renderHand(RenderHandEvent event){
        if(flag){
            //Hand move code here//Not gonna lie, most of the code I am trying here starts off copy-pasted from ItemRenderer.class.
            /*AbstractClientPlayer abstractclientplayer = Minecraft.getMinecraft().thePlayer;
            float par3 = abstractclientplayer.prevRotationPitch + (abstractclientplayer.rotationPitch - abstractclientplayer.prevRotationPitch) * event.getPartialTicks();//TODO determine these 3 variables
            float par7 = 1.0F - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * event.getPartialTicks());
            float par5 = abstractclientplayer.getSwingProgress(event.getPartialTicks());
            float f = MathHelper.sqrt_float(par5);
            float f1 = -0.2F * MathHelper.sin(par5 * (float)Math.PI);
            float f2 = -0.4F * MathHelper.sin(f * (float)Math.PI);
            GlStateManager.translate(0.0F, -f1 / 2.0F, f2);
            float f3 = getMapAngleFromPitch(par3);
            GlStateManager.translate(0.0F, 0.04F + par7 * -1.2F + f3 * -0.5F, -0.72F);
            GlStateManager.rotate(f3 * -85.0F, 1.0F, 0.0F, 0.0F);
            renderArms();
            float f4 = MathHelper.sin(f * (float)Math.PI);
            GlStateManager.rotate(f4 * 20.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(2.0F, 2.0F, 2.0F);*/
            transformEatFirstPerson(event.getPartialTicks(), EnumHandSide.LEFT);
            transformEatFirstPerson(event.getPartialTicks(), EnumHandSide.RIGHT);
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
        prevEquippedProgress = equippedProgress;
        float f = 0.4F;
        equippedProgress+= MathHelper.clamp_float((f * f * f) - equippedProgress, -0.4F, 0.4F);
    }
    private float getMapAngleFromPitch(float pitch)
    {
        float f = 1.0F - pitch / 45.0F + 0.1F;
        f = MathHelper.clamp_float(f, 0.0F, 1.0F);
        f = -MathHelper.cos(f * (float)Math.PI) * 0.5F + 0.5F;
        return f;
    }
    private void renderArms()
    {
        if (!Minecraft.getMinecraft().thePlayer.isInvisible())
        {
            GlStateManager.disableCull();
            GlStateManager.pushMatrix();
            GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
            renderArm(EnumHandSide.RIGHT);
            renderArm(EnumHandSide.LEFT);
            GlStateManager.popMatrix();
            GlStateManager.enableCull();
        }
    }
    private void renderArm(EnumHandSide p_187455_1_)
    {
        Minecraft.getMinecraft().getTextureManager().bindTexture(Minecraft.getMinecraft().thePlayer.getLocationSkin());
        Render<AbstractClientPlayer> render = Minecraft.getMinecraft().getRenderManager().<AbstractClientPlayer>getEntityRenderObject(Minecraft.getMinecraft().thePlayer);
        RenderPlayer renderplayer = (RenderPlayer)render;
        GlStateManager.pushMatrix();
        float f = p_187455_1_ == EnumHandSide.RIGHT ? 1.0F : -1.0F;
        GlStateManager.rotate(92.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(45.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(f * -41.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(f * 0.3F, -1.1F, 0.45F);

        if (p_187455_1_ == EnumHandSide.RIGHT)
        {
            renderplayer.renderRightArm(Minecraft.getMinecraft().thePlayer);
        }
        else
        {
            renderplayer.renderLeftArm(Minecraft.getMinecraft().thePlayer);
        }

        GlStateManager.popMatrix();
    }

    private void transformEatFirstPerson(float partialTicks, EnumHandSide p_187454_2_)
    {
        float f = (float)ticksUsing - partialTicks + 1.0F;
        float f1 = f / (float)33;//max use duration = 33

        if (f1 < 0.8F)
        {
            float f2 = MathHelper.abs(MathHelper.cos(f / 4.0F * (float)Math.PI) * 0.1F);
            GlStateManager.translate(0.0F, f2, 0.0F);
        }

        float f3 = 1.0F - (float)Math.pow((double)f1, 27.0D);
        int i = p_187454_2_ == EnumHandSide.RIGHT ? 1 : -1;
        GlStateManager.translate(f3 * 0.6F * (float)i, f3 * -0.5F, f3 * 0.0F);
        GlStateManager.rotate((float)i * f3 * 90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f3 * 10.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate((float)i * f3 * 30.0F, 0.0F, 0.0F, 1.0F);
    }
}
