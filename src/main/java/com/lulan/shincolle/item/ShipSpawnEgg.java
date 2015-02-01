package com.lulan.shincolle.item;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Facing;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.lulan.shincolle.crafting.ShipCalc;
import com.lulan.shincolle.creativetab.CreativeTabSC;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**custom spawn egg
*  read egg NBTdata to spawn different ship
*  metadata:0: all small ship, 1: all large ship 2+:specific ship for debug
**/
public class ShipSpawnEgg extends Item {
	
	Random rand;
	private static final int ShipNumber = 2;
	
	@SideOnly(Side.CLIENT)
    private IIcon[] iconEgg = new IIcon[3];	//egg icon
	
	private static final String[] subNames = {		//for egg display names
    	"smallship", "largeship", "debugship"
    };
	
    private EntityLiving entityToSpawn = null;
    private String entityToSpawnName = null;

    public ShipSpawnEgg() {
        super();
        this.setHasSubtypes(true);	//true for enable metadata
        this.setCreativeTab(CreativeTabSC.SC_TAB);
        this.maxStackSize = 1;
        rand = new Random();
        
    }
  	
    //format: item.MOD_ID:EGG_NAME.name
    //item name for different metadata
  	@Override
  	public String getUnlocalizedName(ItemStack itemstack) {
  		int metaid = itemstack.getItemDamage();		//get metadata
  		
  		switch(metaid) {
  		case 0:	  //small ship
  			return String.format("item."+Reference.MOD_ID+":smallegg");
  		case 1:   //large ship
  			return String.format("item."+Reference.MOD_ID+":largeegg");
  		default:  //default case = debug ship
  			return String.format("item."+Reference.MOD_ID+":debugegg");
  		}		
  	}
  	
  	//egg icon register
  	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister iconRegister) {  		
  		for (int i=0; i<3; i++) {
  	        iconEgg[i] = iconRegister.registerIcon(Reference.MOD_ID + ":shipegg_" + i);
  	    }
	}
  	
  	//egg icon from metadata
  	@Override
  	@SideOnly(Side.CLIENT)
  	public IIcon getIconFromDamage(int meta) {
  	    if (meta > 2) meta = 2;		//(meta>=2) -> debug egg
  	    
  	    return iconEgg[meta];
  	}
  	
  	//for list all same id items
  	@Override
  	@SideOnly(Side.CLIENT)
  	public void getSubItems(Item item, CreativeTabs tab, List list) {
  	    for (int i=0; i<ShipNumber+2; i++) {
  	        list.add(new ItemStack(item, 1, i));
  	    }
  	}
  	
  	/** Get entity name from metadata
  	 *  (entity name from ModEntity.class)
  	 *  small egg: all non-hime ship
  	 *  large egg: all ship
  	 *  specific egg: specific ship
  	 */
  	private String getEntityToSpawnName(int meta) {
  		switch(meta) {
  		case 0:	//small egg
  			return "shincolle.EntityDestroyerI";
  		case 1:	//large egg
  			return "shincolle.EntityDestroyerI";
  		case 2:	//Destroyer I
  			return "shincolle.EntityDestroyerI";
  		default:
  			return "shincolle.EntityDestroyerI";
  		}
  		
  	}
  	
  	/** VANILLA SPAWN METHOD edited by Jabelar
     * Spawns the creature specified by the egg's type in the location specified by 
     * the last three parameters.
     * Parameters: world, metadata, x, y, z
     */
  	private Entity spawnEntity(World parWorld,int meta, double parX, double parY, double parZ) {
         	
        if (!parWorld.isRemote) {	// never spawn entity on client side 
            entityToSpawnName = getEntityToSpawnName(meta);
            
            if (EntityList.stringToClassMapping.containsKey(entityToSpawnName)) {
                entityToSpawn = (EntityLiving) EntityList.createEntityByName(entityToSpawnName, parWorld);
                entityToSpawn.setLocationAndAngles(parX, parY, parZ, MathHelper.wrapAngleTo180_float(parWorld.rand.nextFloat()* 360.0F), 0.0F);
                parWorld.spawnEntityInWorld(entityToSpawn);
                entityToSpawn.onSpawnWithEgg((IEntityLivingData)null);	//for vanilla random spawn, disable
                entityToSpawn.playLivingSound();
            } 
            else {
                LogHelper.info("Entity not found "+entityToSpawnName);	//debug
            }
        }
        
        return entityToSpawn;
    }
  	
  	/**CALC ENTITY RANDOM BONUS ATTRIBUTE
  	 * calc materials amount and random gen the bonus attributes
	 * @parm spawn egg item, player, entity
	 */
  	private void initEntityAttribute(ItemStack itemstack, EntityPlayer player, BasicEntityShip entity) {
  		//set owner
  		entity.setOwner(player.getDisplayName());
  		
  		//calc HP ATK DEF SPD MOV HIT bonus point
  		byte[] bonuspoint = new byte[6];	 
  		bonuspoint = ShipCalc.getBonusPoints(itemstack);
  		
  		//set bonus point
  		entity.setBonusHP(bonuspoint[AttrID.HP]);
  		entity.setBonusATK(bonuspoint[AttrID.ATK]);
  		entity.setBonusDEF(bonuspoint[AttrID.DEF]);
  		entity.setBonusSPD(bonuspoint[AttrID.SPD]);
  		entity.setBonusMOV(bonuspoint[AttrID.MOV]);
  		entity.setBonusHIT(bonuspoint[AttrID.HIT]);
  		
  		//calc ship attribute and save to nbt: hp atk def ...
  		LogHelper.info("DEBUG : spawn egg set ship attribute");
  		entity.calcShipAttributes(entity.getShipID());
  		
  	}
  	
  	/** VANILLA SPAWN EGG onItemUse event (use item to block)
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    @Override
    public boolean onItemUse(ItemStack itemstack, EntityPlayer player, World world, int par4, int par5, int par6, int par7, float par8, float par9, float par10) {
        
    	if (world.isRemote) {	//client side
            return true;
        }
        else {						//server side
            Block block = world.getBlock(par4, par5, par6);		//get spawn position
            par4 += Facing.offsetsXForSide[par7];
            par5 += Facing.offsetsYForSide[par7];
            par6 += Facing.offsetsZForSide[par7];
            double d0 = 0.0D;

            if (par7 == 1 && block.getRenderType() == 11) {			//type11 = fence
                d0 = 0.5D;
            }
            
            //spawn entity in front of player (1 block)
            BasicEntityShip entity = (BasicEntityShip) spawnEntity(world, itemstack.getItemDamage(), par4 + 0.5D, par5 + d0, par6 + 0.5D);

            if (entity != null) {
            	//calc bonus point, set custom name and owner name
            	initEntityAttribute(itemstack, player, entity);
         	
            	//for egg with nameTag
                if (itemstack.hasDisplayName()) {
                    entity.setCustomNameTag(itemstack.getDisplayName());    
                }
                //if creative mode = item not consume
                if (!player.capabilities.isCreativeMode)
                {
                    --itemstack.stackSize;
                }
            }

            return true;
        }
    }
    
    /** VANILLA SPAWN EGG onItemRightClick event (use item to air)
     * Called whenever this item is equipped and the right mouse button is pressed. Args: itemStack, world, entityPlayer
     */
    @Override
    public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer player) {
        
    	if (world.isRemote) {	//client side
            return itemstack;
        }
        else {						//server side
            MovingObjectPosition movingobjectposition = getMovingObjectPositionFromPlayer(world, player, true);

            if (movingobjectposition == null) {
                return itemstack;
            }
            else {
                if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    int i = movingobjectposition.blockX;
                    int j = movingobjectposition.blockY;
                    int k = movingobjectposition.blockZ;

                    if (!world.canMineBlock(player, i, j, k)) {
                        return itemstack;
                    }

                    if (!player.canPlayerEdit(i, j, k, movingobjectposition.sideHit, itemstack)) {
                        return itemstack;
                    }

                    if (world.getBlock(i, j, k) instanceof BlockLiquid) {
                    	BasicEntityShip entity = (BasicEntityShip) spawnEntity(world, itemstack.getItemDamage(), i, j, k);

                        if (entity != null) {
                        	//calc bonus point, set custom name and owner name
                        	initEntityAttribute(itemstack, player, entity);    	
                        	
                        	//for egg with nameTag
                            if (entity instanceof EntityLivingBase && itemstack.hasDisplayName()) {
                                entity.setCustomNameTag(itemstack.getDisplayName());
                            }
                            //if creative mode = item not consume
                            if (!player.capabilities.isCreativeMode) {
                                --itemstack.stackSize;
                            }
                        }
                    }
                }

                return itemstack;
            }//end else
        }
    }
     
    //display egg information
    @Override
    public void addInformation(ItemStack itemstack, EntityPlayer player, List list, boolean par4) {
    	int[] material = new int[4];
    	
    	if (itemstack.stackTagCompound != null) { 	//正常製造egg, 會有四個材料tag		
    		material[0] = itemstack.stackTagCompound.getInteger("Grudge");
    		material[1] = itemstack.stackTagCompound.getInteger("Abyssium");
    		material[2] = itemstack.stackTagCompound.getInteger("Ammo");
    		material[3] = itemstack.stackTagCompound.getInteger("Polymetal");          
        }
    	
    	list.add(EnumChatFormatting.WHITE + "" + material[0] + " Grudge");
        list.add(EnumChatFormatting.RED + "" + material[1] + " Abyssium");
        list.add(EnumChatFormatting.GREEN + "" + material[2] + " Ammo");
        list.add(EnumChatFormatting.AQUA + "" + material[3] + " Polymetal");
    }
    
   
}
