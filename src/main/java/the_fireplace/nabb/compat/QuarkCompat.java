package the_fireplace.nabb.compat;

import net.minecraft.command.ICommandSender;
import vazkii.quark.base.Quark;

public class QuarkCompat implements IQuarkHandler {
	@Override
	public void playClapAnimation(ICommandSender sender) {
		Quark.proxy.doEmote(sender.getName(), "clap");
	}
}
