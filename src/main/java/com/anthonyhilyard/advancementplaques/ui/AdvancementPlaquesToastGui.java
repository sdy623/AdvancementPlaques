package com.anthonyhilyard.advancementplaques.ui;

import java.util.Arrays;
import java.util.Deque;

import com.anthonyhilyard.advancementplaques.AdvancementPlaques;
import com.anthonyhilyard.advancementplaques.config.AdvancementPlaquesConfig;
import com.anthonyhilyard.advancementplaques.ui.render.AdvancementPlaque;
import com.anthonyhilyard.iceberg.renderer.CustomItemRenderer;
import com.google.common.collect.Queues;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.fabricmc.loader.api.FabricLoader;

public class AdvancementPlaquesToastGui extends ToastComponent
{
	private final AdvancementPlaque[] plaques = new AdvancementPlaque[1];
	private final Deque<AdvancementToast> advancementToastsQueue = Queues.newArrayDeque();
	private final Minecraft mc;
	private final CustomItemRenderer itemRenderer;

	public AdvancementPlaquesToastGui(Minecraft mcIn)
	{
		super(mcIn);
		mc = mcIn;
		itemRenderer = new CustomItemRenderer(mc.getTextureManager(), mc.getModelManager(), mc.itemColors, mc.getItemRenderer().blockEntityRenderer, mc);
	}

	@Override
	public void addToast(Toast toastIn)
	{
		if (toastIn instanceof AdvancementToast)
		{
			AdvancementToast advancementToast = (AdvancementToast)toastIn;
			if (AdvancementPlaquesConfig.showPlaqueForAdvancement(advancementToast.advancement))
			{
				// Special logic for advancement toasts.  Store them seperately since they will be displayed seperately.
				advancementToastsQueue.add((AdvancementToast)toastIn);
				return;
			}
		}

		super.addToast(toastIn);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void render(GuiGraphics graphics)
	{
		if (!mc.options.hideGui)
		{
			// Do toasts.
			super.render(graphics);

			try
			{
				// If Waila/Hwyla/Jade is installed, turn it off while the plaque is drawing if configured to do so.
				boolean wailaLoaded = FabricLoader.getInstance().isModLoaded("waila");
				boolean jadeLoaded = FabricLoader.getInstance().isModLoaded("jade");
				if (AdvancementPlaquesConfig.INSTANCE.hideWaila.get() && (wailaLoaded || jadeLoaded))
				{
					boolean anyPlaques = false;
					for (int i = 0; i < plaques.length; i++)
					{
						if (plaques[i] != null)
						{
							anyPlaques = true;
							break;
						}
					}

					if (anyPlaques)
					{
						if (wailaLoaded)
						{
							Class.forName("com.anthonyhilyard.advancementplaques.compat.WailaHandler").getMethod("disableWaila").invoke(null);
						}
						if (jadeLoaded)
						{
							Class.forName("com.anthonyhilyard.advancementplaques.compat.JadeHandler").getMethod("disableJade").invoke(null);
						}
					}
					else
					{
						if (wailaLoaded)
						{
							Class.forName("com.anthonyhilyard.advancementplaques.compat.WailaHandler").getMethod("enableWaila").invoke(null);
						}
						if (jadeLoaded)
						{
							Class.forName("com.anthonyhilyard.advancementplaques.compat.JadeHandler").getMethod("enableJade").invoke(null);
						}
					}
				}
			}
			catch (Exception e)
			{
				AdvancementPlaques.LOGGER.error(e);
			}

			// Do plaques.
			for (int i = 0; i < plaques.length; ++i)
			{
				AdvancementPlaque toastinstance = plaques[i];

				if (toastinstance != null && toastinstance.render(graphics.guiWidth(), i, graphics))
				{
					plaques[i] = null;
				}

				if (plaques[i] == null && !advancementToastsQueue.isEmpty())
				{
					plaques[i] = new AdvancementPlaque(advancementToastsQueue.removeFirst(), mc, itemRenderer);
				}
			}
		}
	}

	@Override
	public void clear()
	{
		super.clear();
		Arrays.fill(plaques, null);
		advancementToastsQueue.clear();
	}
}