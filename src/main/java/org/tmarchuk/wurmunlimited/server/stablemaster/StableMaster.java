package org.tmarchuk.wurmunlimited.server.stablemaster;

/**
 * Created by Tyson Marchuk on 2016-04-30.
 */

// From Wurm Commmon
import com.wurmonline.shared.constants.SoundNames;

//From Wurm Unlimited Server
import com.wurmonline.server.bodys.BodyTemplate;
import static com.wurmonline.server.creatures.CreatureTypes.*;
import com.wurmonline.server.skills.SkillList;

//From Ago's modloader
import org.gotti.wurmunlimited.modsupport.CreatureTemplateBuilder;
import org.gotti.wurmunlimited.modsupport.IdFactory;
import org.gotti.wurmunlimited.modsupport.IdType;
import org.gotti.wurmunlimited.modsupport.creatures.ModCreature;


public class StableMaster implements ModCreature
{
	// Constants
	private static String STABLE_MASTER_TEMPLATE_BUILDER_ID = "stableMaster";
	
	// Template ID to use for the stable master. Might be set or auto-generated.
	private boolean specifiedStableMasterTemplateId;
	private int stableMasterTemplateId;

	// Simple parameters for the template
	private final String tName = "Stable master";
	private final String tDescription = "An expert with animals. Able to help load animals on ships.";
	private final String tModelName = "model.creature.humanoid.human.salesman";
	private final short tVisionTiles = 2;	// How  many tiles away it can see.
	private final byte tSex = 0;			// I don't understand. This appears to be male but female can be spawned...
	private final short tHeightCentimeters = 180;
	private final short tLengthCentimeters = 20;
	private final short tWidthCentimeters = 35;
	private final float tNaturalArmour = 1.0f;
	private final float tHandDamage = 1.0f;
	private final float tKickDamage = 2.0f;
	private final float tBiteDamage = 0.0f;
	private final float tHeadDamage = 0.0f;
	private final float tBreathDamage = 0.0f;
	private final float tSpeed = 0.8f;
	private final int tMoveRate = 0;
	private final int[] tItemsButchered = new int[0]; 	// Butchering gets inventory?
	private final int tMaxHuntDistanceTiles = 3;
	private final int tAggressivity = 0;
	
	public StableMaster(boolean specifyId, int id)
	{
		specifiedStableMasterTemplateId = specifyId;
		stableMasterTemplateId = id;
	}
	
	public int getTemplateId()
	{
		return this.stableMasterTemplateId;
	}

	@Override
	public CreatureTemplateBuilder createCreateTemplateBuilder()
	{
		// Update the stable master template ID based on the factory if we weren't told to use a specific one.
		if (!specifiedStableMasterTemplateId)
		{
			stableMasterTemplateId = IdFactory.getIdFor(STABLE_MASTER_TEMPLATE_BUILDER_ID, IdType.CREATURETEMPLATE);
		}

		// This NPC is largely based on the Salesman NPC for now.
		final int[] tTypes = {C_TYPE_SENTINEL, C_TYPE_INVULNERABLE, C_TYPE_SWIMMING, C_TYPE_HUMAN };
		CreatureTemplateBuilder builder = new CreatureTemplateBuilder(stableMasterTemplateId);
		builder.name(tName);
		builder.description(tDescription);
		builder.modelName(tModelName);
		builder.types(tTypes);
		builder.bodyType(BodyTemplate.TYPE_HUMAN);
		builder.vision(tVisionTiles);
		builder.sex(tSex);
		builder.dimension(tHeightCentimeters, tLengthCentimeters, tWidthCentimeters);
		builder.deathSounds(SoundNames.DEATH_MALE_SND, SoundNames.DEATH_FEMALE_SND);
		builder.hitSounds(SoundNames.HIT_MALE_SND, SoundNames.HIT_FEMALE_SND);
		builder.naturalArmour(tNaturalArmour);
		builder.damages(tHandDamage, tKickDamage, tBiteDamage, tHeadDamage, tBreathDamage);
		builder.speed(tSpeed);
		builder.moveRate(tMoveRate);
		builder.itemsButchered(tItemsButchered);
		builder.maxHuntDist(tMaxHuntDistanceTiles);
		builder.aggressive(tAggressivity);

		// Skills (Copied from Salesman for now)
		builder.skill(SkillList.BODY_STRENGTH, 15.0f);
		builder.skill(SkillList.BODY_CONTROL, 15.0f);
		builder.skill(SkillList.BODY_STAMINA, 10.0f);
		builder.skill(SkillList.MIND_LOGICAL, 30.0f);
		builder.skill(SkillList.MIND_SPEED, 30.0f);
		builder.skill(SkillList.SOUL_STRENGTH, 99.0f);
		builder.skill(SkillList.SOUL_DEPTH, 4.0f);
		builder.skill(SkillList.WEAPONLESS_FIGHTING, 40.0f);
		
		builder.baseCombatRating(70.0f);

		// TODO: Figure out if we need this.
		//Field hasHands = ReflectionUtil.getField(CreatureTemplate.class, "hasHands");
		//ReflectionUtil.setPrivateField(temp, hasHands, true);

		return builder;
	}
}
