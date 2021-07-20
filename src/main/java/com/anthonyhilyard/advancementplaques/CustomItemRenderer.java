package com.anthonyhilyard.advancementplaques;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ModelManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;

public class CustomItemRenderer extends ItemRenderer
{
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LogManager.getLogger();

	private static Framebuffer iconFrameBuffer = null;
	private Minecraft mc;
	
	public CustomItemRenderer(TextureManager textureManagerIn, ModelManager modelManagerIn, ItemColors itemColorsIn, Minecraft mcIn)
	{
		super(textureManagerIn, modelManagerIn, itemColorsIn);
		mc = mcIn;

		// Initialize the icon framebuffer if needed.
		if (iconFrameBuffer == null)
		{
			// Use 96 x 96 pixels for the icon frame buffer so at 1.5 scale we get 4x resolution (for smooth icons on larger gui scales).
			iconFrameBuffer = new Framebuffer(96, 96, true, Minecraft.IS_RUNNING_ON_MAC);
			iconFrameBuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
		}
	}

	@SuppressWarnings("deprecation")
	public void renderItemModelIntoGUIWithAlpha(ItemStack stack, int x, int y, float alpha)
	{
		IBakedModel bakedModel = mc.getItemRenderer().getItemModelWithOverrides(stack, null, null);
		Framebuffer lastFrameBuffer = mc.getFramebuffer();

		// Bind the icon framebuffer so we can render to texture.
		iconFrameBuffer.framebufferClear(Minecraft.IS_RUNNING_ON_MAC);
		iconFrameBuffer.bindFramebuffer(true);
		
		RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.IS_RUNNING_ON_MAC);
		RenderSystem.matrixMode(GL11.GL_PROJECTION);
		RenderSystem.pushMatrix();
		RenderSystem.loadIdentity();
		RenderSystem.ortho(0.0D, iconFrameBuffer.framebufferWidth, iconFrameBuffer.framebufferHeight, 0.0D, 1000.0D, 3000.0D);
		RenderSystem.matrixMode(GL11.GL_MODELVIEW);
		RenderSystem.pushMatrix();
		RenderSystem.loadIdentity();
		RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
		RenderHelper.setupGui3DDiffuseLighting();

		mc.getTextureManager().bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
		mc.getTextureManager().getTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE).setBlurMipmapDirect(false, false);
		RenderSystem.enableRescaleNormal();
		RenderSystem.enableAlphaTest();
		RenderSystem.defaultAlphaFunc();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.translatef(48.0f, 48.0f, 150.0f + this.zLevel);
		RenderSystem.scalef(1.0f, -1.0f, 1.0f);
		RenderSystem.scalef(96.0f, 96.0f, 96.0f);
		MatrixStack matrixStack = new MatrixStack();
		IRenderTypeBuffer.Impl renderBuffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
		boolean isSideLit = !bakedModel.isSideLit();
		if (isSideLit)
		{
			RenderHelper.setupGuiFlatDiffuseLighting();
		}

		renderItem(stack, ItemCameraTransforms.TransformType.GUI, false, matrixStack, renderBuffer, 0xF000F0, OverlayTexture.NO_OVERLAY, bakedModel);
		renderBuffer.finish();
		RenderSystem.enableDepthTest();
		if (isSideLit)
		{
			RenderHelper.setupGui3DDiffuseLighting();
		}

		RenderSystem.disableRescaleNormal();
		RenderSystem.popMatrix();
		RenderSystem.matrixMode(GL11.GL_PROJECTION);
		RenderSystem.popMatrix();
		RenderSystem.matrixMode(GL11.GL_MODELVIEW);

		// Rebind the previous framebuffer, if there was one.
		if (lastFrameBuffer != null)
		{
			lastFrameBuffer.bindFramebuffer(true);

			// Blit from the texture we just rendered to, respecting the alpha value given.
			iconFrameBuffer.bindFramebufferTexture();
			RenderSystem.enableAlphaTest();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.defaultAlphaFunc();
			RenderSystem.disableCull();
			RenderSystem.color4f(1.0f, 1.0f, 1.0f, alpha);
			RenderSystem.scalef(1.0f, -1.0f, 1.0f);

			AbstractGui.blit(new MatrixStack(), x, y - 18, 16, 16, 0, 0, iconFrameBuffer.framebufferTextureWidth, iconFrameBuffer.framebufferTextureHeight, iconFrameBuffer.framebufferTextureWidth, iconFrameBuffer.framebufferTextureHeight);
		}
		else
		{
			iconFrameBuffer.unbindFramebuffer();
		}
	}
}