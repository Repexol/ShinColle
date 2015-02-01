package com.lulan.shincolle.ai;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityWaterMob;


/**GET TARGET WITHIN SPECIFIC RANGE
 * mode: 
 * 0:target between range1 and range2 only (只打外圈)
 * 1:target < range1 => < range2 (先打內圈, 再打外圈)
 * 2:target between range1 and range2 => < range1 (先打外圈, 再打內圈)
 * 
 * @parm host, target, range1, range2, mode 
 * @parm host, target, range proportion, mode
 */
public class EntityAIInRangeTarget extends EntityAITarget {
	
    private final Class targetClass;
    private final EntityAIInRangeTarget.Sorter targetSorter;
    private final IEntitySelector targetSelector;
    private BasicEntityShip host;
    private EntityLivingBase targetEntity;
    private int range1;
    private int range2;
    private int targetMode;
    

    //將maxRange 乘上一個比例當作range1
    public EntityAIInRangeTarget(EntityCreature host, float rangeProp, int mode) {
    	super(host, true, false);	//check onSight and not nearby(collision) only
    	this.host = (BasicEntityShip) host;
    	this.targetClass = EntityLiving.class;
        this.targetSorter = new EntityAIInRangeTarget.Sorter(host);
        this.setMutexBits(1);

        //範圍指定
        this.range2 = (int)this.host.getFinalHIT();
        this.range1 = (int)(rangeProp * (float)this.range2);       
        this.targetMode = mode;
        
        //檢查範圍, 使range2 > range1 > 1
        if(this.range1 < 1) {
        	this.range1 = 1;
        }
        if(this.range2 <= this.range1) {
        	this.range2 = this.range1 + 1;
        }
 
        //target selector init
        this.targetSelector = new IEntitySelector() {
            public boolean isEntityApplicable(Entity target2) {
            	if(target2 instanceof EntityMob || target2 instanceof EntitySlime ||
            	   target2 instanceof EntityBat || target2 instanceof EntityDragon ||
            	   target2 instanceof EntityFlying || target2 instanceof EntityWaterMob) {
            		return true;
            	}
            	return false;
            }
        };
    }

	public EntityAIInRangeTarget(EntityCreature host, Class targetClass, int range1, int range2, int mode) {
        super(host, true, false);	//check onSight and not nearby(collision) only
        this.host = (BasicEntityShip) host;
        this.targetClass = targetClass;	//target class
        this.targetSorter = new EntityAIInRangeTarget.Sorter(host);
        this.setMutexBits(1);
        
        //範圍指定
        this.range1 = range1;
        this.range2 = range2;
        this.targetMode = mode;
        
        //檢查範圍, 使range2 > range1 > 1
        if(this.range1 < 1) {
        	this.range1 = 1;
        }
        if(this.range2 <= this.range1) {
        	this.range2 = this.range1 + 1;
        }
  
        //target selector init
        this.targetSelector = new IEntitySelector() {
            public boolean isEntityApplicable(Entity target2) {
            	//若目標為可扣血的則true, 否則用isSuitableTarget判定(且target player = false)
                return !(target2 instanceof EntityLivingBase) ? 
                		false : EntityAIInRangeTarget.this.isSuitableTarget((EntityLivingBase)target2, false);
            }
        };
    }

    public boolean shouldExecute() {
    	//entity list < range1
        List list1 = this.taskOwner.worldObj.selectEntitiesWithinAABB(this.targetClass, 
        		this.taskOwner.boundingBox.expand(this.range1, this.range1 * 0.3D, this.range1), this.targetSelector);
        //entity list < range2
        List list2 = this.taskOwner.worldObj.selectEntitiesWithinAABB(this.targetClass, 
        		this.taskOwner.boundingBox.expand(this.range2, this.range2 * 0.3D, this.range2), this.targetSelector);
        //對目標做distance sort (increment)
        Collections.sort(list1, this.targetSorter);
        Collections.sort(list2, this.targetSorter);
		
        switch(this.targetMode) {
        case 0:  //mode 0:target between range1 and range2 only
        	list2.removeAll(list1);	 //list2排除range1以內的目標
        	if(list2.isEmpty()) {
                return false;
            }
            else {
                this.targetEntity = (EntityLivingBase)list2.get(0);
                return true;
            }
		case 1:  //mode 1:target < range1 => < range2
			if(list1.isEmpty()) {	//range1以內沒有目標, 則找range2
				if(list2.isEmpty()) {
	                return false;
	            }
				else {				//range2以內有目標
					this.targetEntity = (EntityLivingBase)list2.get(0);
	                return true;
				}
            }
            else {					//range1以內有目標
                this.targetEntity = (EntityLivingBase)list1.get(0);
                return true;
            }
        case 2:  //mode 2:target between range1 and range2 => < range1
        	list2.removeAll(list1);	 //list2排除range1以內的目標
        	if(list2.isEmpty()) {	 //range2~range1中沒有目標, 改找range1以內
        		if(list1.isEmpty()) {
                    return false;
                }
        		else {				 //range1以內有目標
        			this.targetEntity = (EntityLivingBase)list1.get(0);
                    return true;
        		}
            }
            else {					 //range2以內有目標
                this.targetEntity = (EntityLivingBase)list2.get(0);
                return true;
            }
        }
     
        return false;
    }

    public void startExecuting() {
        this.taskOwner.setAttackTarget(this.targetEntity);
        super.startExecuting();
    }

    /**SORTER CLASS
     */
    public static class Sorter implements Comparator {
        private final Entity targetEntity;

        public Sorter(Entity entity) {
            this.targetEntity = entity;
        }
        
        public int compare(Object target1, Object target2) {
            return this.compare((Entity)target1, (Entity)target2);
        }

        //負值會排在list前面, 值越大越後面, list(0)會是距離最近的目標
        public int compare(Entity target1, Entity target2) {
            double d0 = this.targetEntity.getDistanceSqToEntity(target1);
            double d1 = this.targetEntity.getDistanceSqToEntity(target2);
        //    return d0 < d1 ? -1 : (d0 > d1 ? 1 : 0);
            return (int)(d0 - d1);
        }       
    }//end sorter class
  
}
