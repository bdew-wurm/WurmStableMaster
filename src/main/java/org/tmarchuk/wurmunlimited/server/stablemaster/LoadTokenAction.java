package org.tmarchuk.wurmunlimited.server.stablemaster;

/**
 * Created by Tyson Marchuk on 2016-05-02.
 */

//From Wurm Unlimited Server
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.Items;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;

// Base Java
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// From Ago's modloader
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

public class LoadTokenAction implements ModAction, BehaviourProvider, ActionPerformer
{
	private static Logger logger = Logger.getLogger(LoadTokenAction.class.getName());

	// Configuration
	private final int animalTokenId;
	private final boolean enableSmallBoatsLoad;
	private static final String actionString = "Load animal token";
	private static final String actionVerb = "loading";
	private static final int[] actionTypes = new int [] 
			{
					36,		// ACTION_TYPE_ALWAYS_USE_ACTIVE_ITEM
					48		// ACTION_TYPE_ENEMY_ALWAYS
			};
	
	// Action data
	private final short actionId;
	private final ActionEntry actionEntry;
	

	public LoadTokenAction(int animalTokenId, boolean enableSmallBoatsLoad) 
	{
		this.animalTokenId = animalTokenId;
		this.enableSmallBoatsLoad = enableSmallBoatsLoad;
		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(actionId, actionString, actionVerb, actionTypes);
		ModActions.registerAction(actionEntry);
	}

	@Override
	public BehaviourProvider getBehaviourProvider() 
	{
		return this;
	}

	@Override
	public ActionPerformer getActionPerformer() 
	{
		return this;
	}

	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target) 
	{
		if ((performer instanceof Player) && 
				(subject.getTemplateId() == animalTokenId) &&
				((target.isBoat()))) 
		{
			return Arrays.asList(actionEntry);
		} 
		else 
		{
			return null;
		}
	}

	@Override
	public short getActionId()
	{
		return actionId;
	}

	@Override
	public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) 
	{
		// Make sure there is space on the boat.
		if (!target.hasSpaceFor(source.getVolume()))
		{
			performer.getCommunicator().sendNormalServerMessage("There is no enough space to load the animal token on the boat.");
			return true;
		}
		
		// Make sure the boat doesn't already have max inventory items.
		if (!target.mayCreatureInsertItem())
		{
			performer.getCommunicator().sendNormalServerMessage("There is no room to load the animal token on the boat.");
			return true;
		}
		
		// Make sure source item is a token 
		if (source.getTemplateId() != animalTokenId)
		{
			performer.getCommunicator().sendNormalServerMessage("You must activate your animal token to load it on the boat.");
			return true;
		}
		
		// Make sure target item is a boat.
		if (!target.isBoat())
		{
			performer.getCommunicator().sendNormalServerMessage("Animal tokens can only be loaded on boats.");
			return true;
		}
		
		// Check if the boat type is allowed.
		if (!this.enableSmallBoatsLoad && ((target.getTemplateId() == ItemList.boatRowing) || 
										   (target.getTemplateId() == ItemList.boatSailing)))
		{
			performer.getCommunicator().sendNormalServerMessage("Animal tokens can only be loaded on boats larger than rowboats and sailboats.");
			return true;
		}
		
		try
		{
			// Make sure performer has permission to put items in the inventory of the boat.
			if (target.isLocked() && !performer.hasKeyForLock(Items.getItem(target.getLockId())) && 
					!target.isOwner(performer) && !target.mayAccessHold(performer))
			{
				performer.getCommunicator().sendNormalServerMessage("You are not allowed to place items on the boat.");
				return true;
			}

			// Move the item from the performer's inventory to the boat's inventory.

			// Load it. Unfortunately we can't just use 'moveToItem' because the item is 'nodrop' and will
			// fail. Also, even if we set it to drop temporarily the code checks the template and will fail 
			// there. Instead this is the path that moveToItem takes when using a GM character (which works).
			source.getParent().dropItem(source.getWurmId(), false);
			performer.addItemDropped(source);
			source.setLastOwnerId(performer.getWurmId());
			boolean result = target.insertItem(source);

			// Check if the load worked.
			if (result)
			{
				performer.getCommunicator().sendNormalServerMessage("You load your animal token onto the boat.");
			}
			else
			{
				performer.getCommunicator().sendNormalServerMessage("You fail to load your animal token onto the boat.");
			}
			return true;
		} catch (NoSuchItemException e)
		{
			logger.log(Level.WARNING, e.getMessage(), e);
			return true;
		}
	}
}
