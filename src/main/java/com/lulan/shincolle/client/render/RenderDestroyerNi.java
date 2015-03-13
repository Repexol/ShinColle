package com.lulan.shincolle.client.render;

import com.lulan.shincolle.reference.Reference;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;


@SideOnly(Side.CLIENT)
public class RenderDestroyerNi extends RenderLiving {
	
	//�K���ɸ��|
	private static final ResourceLocation mobTextures = new ResourceLocation(Reference.TEXTURES_ENTITY+"EntityDestroyerNi.png");

	public RenderDestroyerNi(ModelBase par1, float par2) {
		super(par1, par2);	
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity par1Entity) {
		return mobTextures;
	}

}


