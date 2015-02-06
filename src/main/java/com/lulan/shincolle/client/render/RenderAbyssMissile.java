package com.lulan.shincolle.client.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import com.lulan.shincolle.client.model.ModelAbyssMissile;
import com.lulan.shincolle.entity.EntityAbyssMissile;
import com.lulan.shincolle.reference.Reference;


@SideOnly(Side.CLIENT)
public class RenderAbyssMissile extends Render {
    
	//�K���ɸ��|
	private static final ResourceLocation entityTexture = new ResourceLocation(Reference.TEXTURES_ENTITY+"EntityAbyssMissile.png");
	private ModelBase model;
	private float entityScale;	//�ҫ��j�p

    public RenderAbyssMissile(ModelBase model, float scale) {   
    	this.model = new ModelAbyssMissile();
    	this.entityScale = scale;
	}
    
    @Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return entityTexture;
	}

    public void doRender(EntityAbyssMissile entity, double offsetX, double offsetY, double offsetZ, float p_76986_8_, float p_76986_9_) {
    	//bind texture
        this.bindEntityTexture(entity);  		//call getEntityTexture
        
        //render start
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_CULL_FACE);	//�O��model�������e�X��, ���O�u�e�ݱo�쪺��
        
        //model position set to center
        GL11.glTranslatef((float)offsetX, (float)offsetY+0.3F, (float)offsetZ);
        
        //apply model scale
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);	//�Nscale�վ�Ҧ��]��normal      		
        GL11.glScalef(this.entityScale, this.entityScale, this.entityScale);   //�վ�model�j�p

        //parm: entity, f�̲��ʳt��, f1�̲��ʳt��, f2���W, f3���k����, f4�W�U����, f5(scale)
        this.model.render(entity, 0F, 0F, 0F, entity.rotationYaw, entity.rotationPitch, 0.0625F);
        
        //render end
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();       
    }

    //�ǤJentity�����নabyssmissile
    public void doRender(Entity entity, double offsetX, double offsetY, double offsetZ, float p_76986_8_, float p_76986_9_) {
        this.doRender((EntityAbyssMissile)entity, offsetX, offsetY, offsetZ, p_76986_8_, p_76986_9_);
    }
}