package the_fireplace.nabb.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import the_fireplace.nabb.NABB;

/**
 * @author The_Fireplace
 */
public class PlayBeeAnimation implements IMessage {

    public PlayBeeAnimation() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {

    }

    @Override
    public void toBytes(ByteBuf buf) {

    }

    public static class Handler extends AbstractClientMessageHandler<PlayBeeAnimation> {
        @Override
        public IMessage handleClientMessage(EntityPlayer player, PlayBeeAnimation message, MessageContext ctx) {
            NABB.flag = true;
            return null;
        }
    }
}
