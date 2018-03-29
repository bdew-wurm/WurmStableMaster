package org.tmarchuk.wurmunlimited.server.stablemaster;

/**
 * Created by Tyson Marchuk on 2016-05-02.
 */

//From Wurm Unlimited Server
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.Items;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;

//Base Java
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//From Ago's modloader
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

public class UnloadTokenAction implements ModAction, BehaviourProvider, ActionPerformer
{
	private static Logger logger = Logger.getLogger(UnloadTokenAction.class.getName());

	// Configuration
	private final int animalTokenId;
	private static final String actionString = "Unload animal token";
	private static final String actionVerb = "unloading";
	private static final int[] actionTypes = new int [] 
			{
					0, 		// ACTION_TYPE_QUICK
					37,		// ACTION_TYPE_NEVER_USE_ACTIVE_ITEM
					48		// ACTION_TYPE_ENEMY_ALWAYS
			};
	
	// Action data
	private final short actionId;
	private final ActionEntry actionEntry;
	

	public UnloadTokenAction(int animalTokenId) 
	{
		this.animalTokenId = animalTokenId;
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
	public List<ActionEntry> getBehavioursFor(Creature performer, Item target) 
	{
		try
		{
			if ((performer instanceof Player) && 
					(target.getTemplateId() == animalTokenId) && (target.getParent().isBoat())) 
			{
				return Arrays.asList(actionEntry);
			} 
			else 
			{
				return null;
			}
		} catch (NoSuchItemException e)
		{
			logger.log(Level.WARNING, e.getMessage(), e);
			return null;
		}
	}

	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target) 
	{
		return getBehavioursFor(performer, target);
	}

	@Override
	public short getActionId()
	{
		return actionId;
	}

	@Override
	public boolean action(Action action, Creature performer, Item target, short num, float counter) 
	{
		// Make sure we're not at max number items.
        if (!performer.getInventory().mayCreatureInsertItem()) 
        {
            performer.getCommunicator().sendNormalServerMessage("You do not have enough room in your inventory.");
            return true;
        }

        // Check max weight of player
        if (!performer.canCarry(target.getWeightGrams()))
        {
            performer.getCommunicator().sendNormalServerMessage("You would not be able to carry the animal token. You need to drop some things first.");
            return true;
        }

		try
		{
			// Make sure performer has permission to take items from the inventory of the boat.
			if (target.isLocked() && !performer.hasKeyForLock(Items.getItem(target.getLockId())) && 
					!target.isOwner(performer) && !target.mayAccessHold(performer))
			{
				performer.getCommunicator().sendNormalServerMessage("You are not allowed to take items from the boat.");
				return true;
			}

			// Make sure target item is a token 
			if ((target.getTemplateId() != animalTokenId) || (!target.getParent().isBoat()))
			{
				performer.getCommunicator().sendNormalServerMessage("You must select a animal token on a boat to unload it from a boat.");
				return true;
			}
			
			// Move the item from the boat's inventory to the performer's inventory.
			boolean result = target.moveToItem(performer, performer.getInventory().getWurmId(), true);
			if (result)
			{
				performer.getCommunicator().sendNormalServerMessage("You unload your animal token from the boat.");
			}
			else
			{
				performer.getCommunicator().sendNormalServerMessage("You fail to unload your animal token from the boat.");
			}
			return true;
		} catch (NoSuchItemException | NoSuchPlayerException | NoSuchCreatureException e)
		{
			logger.log(Level.WARNING, e.getMessage(), e);
			return true;
		}
	}

	@Override
	public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) 
	{
		return action(action, performer, target, num, counter);
	}
}
