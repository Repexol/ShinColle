package com.lulan.shincolle.entity;

import java.util.List;
import java.util.UUID;

import com.lulan.shincolle.client.particle.EntityFXTexts;
import com.lulan.shincolle.client.particle.EntityFXSpray;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public abstract class BasicEntityAirplane extends EntityLiving {

	protected BasicEntityShip hostEntity;  	//host target
	protected EntityLivingBase targetEntity;	//onImpact target (for entity)
	protected World world;
    
    //attributes
    public float atk;				//damage
    public float atkSpeed;			//attack speed
    public float kbValue;			//knockback value
    
    //AI flag
    public int numAmmoLight;
    public int numAmmoHeavy;
    public boolean useAmmoLight;
    public boolean useAmmoHeavy;
    public boolean backHome;		//clear target, back to carrier
    private final IEntitySelector targetSelector;
	
    public BasicEntityAirplane(World world) {
        super(world);
        this.backHome = false;
        
        //target selector init
        this.targetSelector = new IEntitySelector() {
            public boolean isEntityApplicable(Entity target2) {
            	if((target2 instanceof EntityMob || target2 instanceof EntitySlime ||
            	   target2 instanceof EntityBat || target2 instanceof EntityDragon ||
            	   target2 instanceof EntityFlying || target2 instanceof EntityWaterMob) &&
            	   !target2.isDead) {
            		return true;
            	}
            	return false;
            }
        };
    }
    
    @Override
	public boolean isAIEnabled() {
		return true;
	}
  	
    //clear AI
  	protected void clearAITasks() {
  	   tasks.taskEntries.clear();
  	}
  	
  	//clear target AI
  	protected void clearAITargetTasks() {
  	   targetTasks.taskEntries.clear();
  	}

    //�T����󱼸��p��
    protected void fall(float world) {}
    protected void updateFallState(double par1, boolean par2) {}
    public boolean isOnLadder() {
        return false;
    }
    
    public EntityLivingBase getOwner() {
        return this.hostEntity;
    }
    
    public EntityLivingBase getTarget() {
        return this.targetEntity;
    }

    //���ʭp��, �h��gravity����
    public void moveEntityWithHeading(float movX, float movZ) {
        if(this.isInWater() || this.handleLavaMovement()) {
            this.moveFlying(movX, movZ, 0.04F);
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.8D;
            this.motionY *= 0.8D;
            this.motionZ *= 0.8D;
        }
        else {
            float f2 = 0.91F;

            if(this.onGround) {
                f2 = this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.boundingBox.minY) - 1, MathHelper.floor_double(this.posZ)).slipperiness * 0.91F;
            }

            float f3 = 0.16277136F / (f2 * f2 * f2);
            this.moveFlying(movX, movZ, this.onGround ? 0.1F * f3 : 0.02F);
            f2 = 0.91F;

            if(this.onGround) {
                f2 = this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.boundingBox.minY) - 1, MathHelper.floor_double(this.posZ)).slipperiness * 0.91F;
            }

            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            this.motionX *= (double)f2;
            this.motionY *= (double)f2;
            this.motionZ *= (double)f2;
        }

        this.prevLimbSwingAmount = this.limbSwingAmount;
        double d1 = this.posX - this.prevPosX;
        double d0 = this.posZ - this.prevPosZ;
        float f4 = MathHelper.sqrt_double(d1 * d1 + d0 * d0) * 4.0F;

        if(f4 > 1.0F) {
            f4 = 1.0F;
        }

        this.limbSwingAmount += (f4 - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }

	@Override
	public void onUpdate() {
		//server side
		if(!this.worldObj.isRemote) {
			//owner����(�q�`�Oserver restart)
			if(this.getOwner() == null) {
				this.setDead();
			}
			else {
				//�k�v
				if(this.backHome && !this.isDead) {
					if(this.getDistanceToEntity(this.getOwner()) > 2.7F) {
						double speed = this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getBaseValue();
						double distX = this.getOwner().posX - this.posX;
						double distY = this.getOwner().posY + 2.3D - this.posY;
						double distZ = this.getOwner().posZ - this.posZ;
						double distSqrt = MathHelper.sqrt_double(distX*distX + distY*distY + distZ*distZ);
						
						this.motionX = distX / distSqrt * speed * 1.0D;
						this.motionY = distY / distSqrt * speed * 1.0D;
						this.motionZ = distZ / distSqrt * speed * 1.0D;
					}
					else {	//�k�ٳѾl�u�� (���Ogrudge���k��)
						this.setDead();
						this.hostEntity.setStateMinor(ID.N.NumAmmoLight, this.hostEntity.getStateMinor(ID.N.NumAmmoLight) + this.numAmmoLight);
						this.hostEntity.setStateMinor(ID.N.NumAmmoHeavy, this.hostEntity.getStateMinor(ID.N.NumAmmoHeavy) + this.numAmmoHeavy);
					}
				}
				
				//�e�X�����u���ؼв���
				if(this.ticksExisted < 30 && this.getAttackTarget() != null) {
					double distX = this.getAttackTarget().posX - this.posX;
					double distZ = this.getAttackTarget().posZ - this.posZ;
					double distSqrt = MathHelper.sqrt_double(distX*distX + distZ*distZ);
					
					this.motionX = distX / distSqrt * 0.375D;
					this.motionZ = distZ / distSqrt * 0.375D;
					this.motionY = 0.05D;
				}
				
				//�W�L60���۰ʮ���
				if(this.ticksExisted > 1200) {
					this.setDead();
				}
				
				//�����ؼЮ���, �����ؼ� or �]��host�ثe�ؼ�
				if(!this.backHome && (this.getAttackTarget() == null || this.getAttackTarget().isDead) && this.hostEntity != null && this.ticksExisted % 2 == 0) {
					//entity list < range1
			        List list = this.worldObj.selectEntitiesWithinAABB(EntityLivingBase.class, 
			        this.boundingBox.expand(20, 15, 20), this.targetSelector);
			        
			        if(list.isEmpty()) {
			        	//���䤣��ؼЫh��host�ؼ�, ���Ohost�ؼХ����b64�椺
			        	EntityLivingBase newTarget = this.hostEntity.getAttackTarget();
			        	
			        	if(newTarget != null && newTarget.isEntityAlive() && this.getDistanceToEntity(newTarget) < 40F) {
			        		this.setAttackTarget(this.hostEntity.getAttackTarget());
			        	}
			        	else {
			        		this.backHome = true;
			        	}
			        }
			        else {
			        	this.setAttackTarget((EntityLivingBase)list.get(list.size()/2));	
			        }	
				}
				
				if(this.isInWater() && this.ticksExisted % 100 == 0) {
					this.setAir(300);
				}
			}	
		}
		
		if(this.ticksExisted % 4 == 0) {
			//���V�p�� (for both side)
			float[] degree = EntityHelper.getLookDegree(posX - prevPosX, posY - prevPosY, posZ - prevPosZ);
			this.rotationYaw = degree[0];
			this.rotationPitch = degree[1];
		}

		super.onUpdate();
	}

	//light attack
	public boolean attackEntityWithAmmo(Entity target) {
		float atkLight = this.atk;
		float kbValue = 0.03F;

		//play cannon fire sound at attacker
        playSound(Reference.MOD_ID+":ship-machinegun", 0.4F, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //attack particle
        TargetPoint point0 = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
		CommonProxy.channel.sendToAllAround(new S2CSpawnParticle(this, 8, false), point0);
		
		//calc miss chance, if not miss, calc cri/multi hit
		float missChance = 0.25F - 0.001F * this.hostEntity.getStateMinor(ID.N.ShipLevel);
        missChance -= this.hostEntity.getEffectEquip(ID.EF_MISS);	//equip miss reduce
        if(missChance > 0.35F) missChance = 0.35F;
		
        //calc miss chance
        if(this.rand.nextFloat() < missChance) {
        	atkLight = 0;	//still attack, but no damage
        	//spawn miss particle
    		EntityFX particleMiss = new EntityFXTexts(worldObj, 
    		          this.hostEntity.posX, this.hostEntity.posY+this.hostEntity.height, this.hostEntity.posZ, 1F, 0);	    
    		Minecraft.getMinecraft().effectRenderer.addEffect(particleMiss);
        }
        else {
        	//roll cri -> roll double hit -> roll triple hit (triple hit more rare)
        	//calc critical
        	if(this.rand.nextFloat() < this.hostEntity.getEffectEquip(ID.EF_CRI)) {
        		atk *= 1.5F;
        		//spawn critical particle
        		EntityFX particleCri = new EntityFXTexts(worldObj, 
        		          this.posX, this.posY+this.height, this.posZ, 1F, 1);	    
        		Minecraft.getMinecraft().effectRenderer.addEffect(particleCri);
        	}
        	else {
        		//calc double hit
            	if(this.rand.nextFloat() < this.hostEntity.getEffectEquip(ID.EF_DHIT)) {
            		atk *= 2F;
            		//spawn double hit particle
            		EntityFX particleDhit = new EntityFXTexts(worldObj, 
            		          this.posX, this.posY+this.height, this.posZ, 1F, 2);	    
            		Minecraft.getMinecraft().effectRenderer.addEffect(particleDhit);
            	}
            	else {
            		//calc double hit
                	if(this.rand.nextFloat() < this.hostEntity.getEffectEquip(ID.EF_THIT)) {
                		atk *= 3F;
                		//spawn triple hit particle
                		EntityFX particleThit = new EntityFXTexts(worldObj, 
                		          this.posX, this.posY+this.height, this.posZ, 1F, 3);	    
                		Minecraft.getMinecraft().effectRenderer.addEffect(particleThit);
                	}
            	}
        	}
        }
        
	    //�Natk��attacker�ǵ��ؼЪ�attackEntityFrom��k, �b�ؼ�class���p��ˮ`
	    //�åB�^�ǬO�_���\�ˮ`��ؼ�
	    boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this), atkLight);

	    //if attack success
	    if(isTargetHurt) {
	    	//calc kb effect
	        if(kbValue > 0) {
	            target.addVelocity((double)(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue), 
	                   0.1D, (double)(MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue));
	        }
	        
        	//send packet to client for display partical effect  
	        TargetPoint point1 = new TargetPoint(this.dimension, target.posX, target.posY, target.posZ, 64D);
			CommonProxy.channel.sendToAllAround(new S2CSpawnParticle(target, 0, false), point1);
        }
	    
	    //���Ӽu�ĭp��
  		if(numAmmoLight > 0) {
  			numAmmoLight--;
  			
  			if(numAmmoLight <= 0) {
  				this.setDead();
  			}
  		}

	    return isTargetHurt;
	}

	public boolean attackEntityWithHeavyAmmo(Entity target) {
		//get attack value
		float atkHeavy = this.atk;
		//set knockback value (testing)
		float kbValue = 0.08F;

		//play cannon fire sound at attacker
        this.playSound(Reference.MOD_ID+":ship-fireheavy", 0.4F, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        
		//calc miss chance, if not miss, calc cri/multi hit
        float missChance = 0.25F - 0.001F * this.hostEntity.getStateMinor(ID.N.ShipLevel);
        missChance -= this.hostEntity.getEffectEquip(ID.EF_MISS);	//equip miss reduce
        if(missChance > 0.35F) missChance = 0.35F;
		
        //calc miss chance
        if(this.rand.nextFloat() < missChance) {
        	atkHeavy = 0;	//still attack, but no damage
        	//spawn miss particle
    		EntityFX particleMiss = new EntityFXTexts(worldObj, 
    		          this.hostEntity.posX, this.hostEntity.posY+this.hostEntity.height, this.hostEntity.posZ, 1F, 0);	    
    		Minecraft.getMinecraft().effectRenderer.addEffect(particleMiss);
        }

        //spawn missile
        EntityAbyssMissile missile = new EntityAbyssMissile(this.worldObj, this, 
        		(float)target.posX, (float)(target.posY+target.height*0.2F), (float)target.posZ, (float)(this.posY-0.8F), atkHeavy, kbValue, true);
        this.worldObj.spawnEntityInWorld(missile);
        
        //���Ӽu�ĭp��
  		if(numAmmoHeavy > 0) {
  			numAmmoHeavy--;
  			
  			if(numAmmoHeavy <= 0) {
  				this.setDead();
  			}
  		}
  		
        return true;
	}

}
