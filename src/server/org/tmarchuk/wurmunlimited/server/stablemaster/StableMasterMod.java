package org.tmarchuk.wurmunlimited.server.stablemaster;

/**
 * Created by Tyson Marchuk on 2016-04-27.
 */

// From Wurm Unlimited Server
import com.wurmonline.server.behaviours.BehaviourList;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.shared.constants.ItemMaterials;
import com.wurmonline.server.MiscConstants;

// From Ago's modloader
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.ItemTemplatesCreatedListener;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;
import org.gotti.wurmunlimited.modsupport.IdFactory;
import org.gotti.wurmunlimited.modsupport.IdType;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

// Base Java
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StableMasterMod implements WurmMod, Initable, PreInitable, ServerStartedListener, ItemTemplatesCreatedListener, ItemTypes, MiscConstants
{
	private static final Logger logger = Logger.getLogger(StableMasterMod.class.getName());
	
	// Constants
	private static final String HORSE_REDEMPTION_TOKEN_IDENTIFIER = "horseRedemptionToken";
	private static final int HORSE_REDEMPTION_TOKEN_SIZE = 3;	// Normal as compared to 1 = tiny, 2 = small, 4 = large, 5 = huge
	private static final short HORSE_REDEMPTION_TOKEN_IMAGE_NUMBER = 321; // Piece of paper.
	private static final float HORSE_REDEMPTION_TOKEN_DIFFICULTY = 1.0f;
	private static final int HORSE_REDEMPTION_TOKEN_VALUE = 0;
	private static final boolean HORSE_REDEMPTION_TOKEN_IS_PURCHASED = false;
	private static final int HORSE_REDEMPTION_TOKEN_ARMOR_TYPE = -1;
	
	// Configuration values
	private boolean specifyStableMasterId = false;
	private int stableMasterId = 20001;
	private boolean specifyHorseRedemptionTokenId = false;
	private int horseRedemptionTokenId = 20002;
	private int horseRedemptionTokenCentimetersX = 20;
	private int horseRedemptionTokenCentimetersY = 50;
	private int horseRedemptionTokenCentimetersZ = 200;
	private int horseRedemptionTokenWeightGrams = 50000;

	public static void logException(String msg, Throwable e)
	{
		if (logger != null)
			logger.log(Level.SEVERE, msg, e);
	}
	@Override
	public void onItemTemplatesCreated() 
	{
		// Create Horse Redemption Token Item Template.
		if (!this.specifyHorseRedemptionTokenId)
		{
			this.horseRedemptionTokenId = IdFactory.getIdFor(HORSE_REDEMPTION_TOKEN_IDENTIFIER, IdType.ITEMTEMPLATE);
		}

		try
		{
			short [] horseRedemptionTokenItemTypes = new short[] 
				{ ITEM_TYPE_LEATHER, ITEM_TYPE_MEAT, ITEM_TYPE_NOTAKE, ITEM_TYPE_INDESTRUCTIBLE,
					ITEM_TYPE_NODROP, ITEM_TYPE_TEMPORARY, ITEM_TYPE_HASDATA, ITEM_TYPE_NORENAME,
					ITEM_TYPE_FLOATING, ITEM_TYPE_NOTRADE, ITEM_TYPE_SERVERBOUND, ITEM_TYPE_NAMED,
					ITEM_TYPE_NOBANK, ITEM_TYPE_MISSION, ITEM_TYPE_NODISCARD, ITEM_TYPE_TRANSPORTABLE,
					ITEM_TYPE_NEVER_SHOW_CREATION_WINDOW_OPTION, ITEM_TYPE_NO_IMPROVE
				};
			ItemTemplateFactory.getInstance().createItemTemplate(
					horseRedemptionTokenId, HORSE_REDEMPTION_TOKEN_SIZE, 
					"horse redemption token", "horse redemption tokens", 
					"excellent", "good", "ok", "poor", 
					"A token to reclaim your horse from the stable master.", 
					horseRedemptionTokenItemTypes, HORSE_REDEMPTION_TOKEN_IMAGE_NUMBER, 
					BehaviourList.itemBehaviour, 0, Long.MAX_VALUE, 
					horseRedemptionTokenCentimetersX, horseRedemptionTokenCentimetersY, 
					horseRedemptionTokenCentimetersZ, (int) NOID, EMPTY_BYTE_PRIMITIVE_ARRAY, 
					"model.writ.", HORSE_REDEMPTION_TOKEN_DIFFICULTY, 
					horseRedemptionTokenWeightGrams, ItemMaterials.MATERIAL_PAPER, 
					HORSE_REDEMPTION_TOKEN_VALUE, HORSE_REDEMPTION_TOKEN_IS_PURCHASED,
					HORSE_REDEMPTION_TOKEN_ARMOR_TYPE);
       
		} catch (IOException ioEx)
		{
			logException("Failed to create Horse Redemption Token item template.", ioEx);
            throw new RuntimeException(ioEx);
		}
	}

	@Override
	public void onServerStarted()
	{
		logger.log(Level.INFO, "Registering exchange/redeem actions.");
		ModActions.registerAction(new ExchangeAction());
		ModActions.registerAction(new RedeemAction());
	}

	@Override
	public void init() 
	{
	}
	
	@Override
	public void preInit() 
	{
		ModActions.init();
	}
}
