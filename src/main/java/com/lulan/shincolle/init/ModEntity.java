package com.lulan.shincolle.init;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.entity.EntityAbyssMissile;
import com.lulan.shincolle.entity.EntityDestroyerI;
import com.lulan.shincolle.entity.EntityTest;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

public class ModEntity {

	public static void init() {	
		int modEntityID = 0;
		
		//register ship entity
		createEntity(EntityDestroyerI.class, "EntityDestroyerI", modEntityID++);
				
		//register projectile entity
		createProjectileEntity(EntityAbyssMissile.class, "EntityAbyssMissile", modEntityID++);
	
		//register test entity
		createEntityGlobalID(EntityTest.class, "EntityTest", 0x20FF45, 0x0040FF);
	
	}
	
	//登錄生物方法
	//參數: 該生物class, 生物名稱, 怪物蛋背景色, 怪物蛋斑點色
	public static void createEntity(Class entityClass, String entityName, int entityId){
		//登錄參數: 生物class, 生物名稱, 生物id, mod副本, 追蹤更新距離, 更新時間間隔, 是否發送同步封包(高速entity必須true才會顯示平順)
		EntityRegistry.registerModEntity(entityClass, entityName, entityId, ShinColle.instance, 64, 1, false);
	}
	
	//登錄非生物方法 (無生怪蛋)
	//參數: 該生物class, 生物名稱
	public static void createProjectileEntity(Class entityClass, String entityName, int modEntityID){
		//登錄參數: 生物class, 生物名稱, 生物id, mod副本, 追蹤更新距離, 更新時間間隔, 是否發送速度封包
		EntityRegistry.registerModEntity(entityClass, entityName, modEntityID, ShinColle.instance, 128, 1, true);
	}
	
	//使用官方共通id登錄生物
	//參數: 該生物class, 生物名稱
	public static void createEntityGlobalID(Class entityClass, String entityName, int backColor, int spotColor){
		int entityId = EntityRegistry.findGlobalUniqueEntityId();
		LogHelper.info("DEBUG : Register Entity with Global ID System "+entityId+" "+entityName);
		
		EntityRegistry.registerGlobalEntityID(entityClass, entityName, entityId);
		//登錄參數: 生物class, 生物名稱, 生物id, mod副本, 追蹤更新距離, 更新時間間隔, 是否發送速度封包
		EntityRegistry.registerModEntity(entityClass, entityName, entityId, ShinColle.instance, 64, 1, false);
		//登錄怪物生物蛋: 生物id, 生成蛋資訊(生物id,背景色,斑點色)
		EntityList.entityEggs.put(Integer.valueOf(entityId), new EntityList.EntityEggInfo(entityId, backColor, spotColor));
	}
	

}
