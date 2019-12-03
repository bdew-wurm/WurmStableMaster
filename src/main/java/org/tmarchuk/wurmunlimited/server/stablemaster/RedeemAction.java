package org.tmarchuk.wurmunlimited.server.stablemaster;

/**
 * Created by Tyson Marchuk on 2016-04-27.
 */

// From Wurm Unlimited Server
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.*;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;

// From Ago's modloader
import com.wurmonline.server.villages.Village;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

// Base Java
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RedeemAction implements ModAction, BehaviourProvider, ActionPerformer
{
	private static Logger logger = Logger.getLogger(RedeemAction.class.getName());

	// Constants

	// Configuration
	private final int animalTokenId;

	// Action data
	private final short actionId;
	private final ActionEntry actionEntry;

	public RedeemAction(int animalTokenId)
	{
		this.animalTokenId = animalTokenId;
		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(actionId, "Redeem animal token", "redeeming", new int[] { 0 /* ACTION_TYPE_QUICK */, 48 /* ACTION_TYPE_ENEMY_ALWAYS */});
		ModActions.registerAction(actionEntry);
	}

	@Override
	public BehaviourProvider getBehaviourProvider() {
		return this;
	}

	@Override
	public ActionPerformer getActionPerformer() {
		return this;
	}

	private boolean canRedeem(Creature performer, Item target) {
		try {
			if (!performer.isPlayer() || target.getTemplateId() != animalTokenId || target.isTraded()) return false;
			Item topParent = target.getTopParentOrNull();
			return performer.getInventory() == topParent;
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error in redeem check", e);
			return false;
		}
	}

	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item target)
	{
		if (canRedeem(performer, target))
		{
			return Collections.singletonList(actionEntry);
		}
		else
		{
			return null;
		}
	}

	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target)
	{
		return getBehavioursFor(performer, target);
	}

	@Override
	public short getActionId() {
		return actionId;
	}

	@Override
	public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter)
	{
		return action(action, performer, target, num, counter);
	}

	@Override
	public boolean action(Action action, Creature performer, Item target, short num, float counter)
	{
		if (!canRedeem(performer, target)) return true;

		try {
			Creature theAnimal = Creatures.getInstance().getCreature(target.getData());

			// Set the location to the current player location.
			CreaturePos performerPos = performer.getStatus().getPosition();
			CreatureStatus animalStatus = theAnimal.getStatus();
			animalStatus.setPositionXYZ(performerPos.getPosX(), performerPos.getPosY(), performerPos.getPosZ());
			animalStatus.setLayer(performer.getLayer());
			animalStatus.getPosition().setZoneId(performerPos.getZoneId());
			Creatures.getInstance().setLastLed(theAnimal.getWurmId(), performer.getWurmId());
			theAnimal.setBridgeId(performer.getBridgeId());

			// Set the kingdom of the animal to the player's kingdom.
			theAnimal.setKingdomId(performer.getKingdomId());

            Brand brand = Creatures.getInstance().getBrand(theAnimal.getWurmId());
            if (brand != null && (performer.getCitizenVillage() == null || brand.getBrandId() != performer.getCitizenVillage().getId())) {
                logger.log(Level.INFO, String.format("Removing brand on %s (%d) as it was redeemed by %s who's in a different village", theAnimal.getName(), theAnimal.getWurmId(), performer.getName()));
                brand.deleteBrand();
            }

            // Restore animal to world.
			CreatureHelper.showCreature(theAnimal);

			// Delete redemption token from player's inventory.
			Items.destroyItem(target.getWurmId());

			// Inform the player.
			performer.getCommunicator().sendNormalServerMessage("You redeem your animal token for an animal!");

			return true;
		} catch (NoSuchCreatureException e) {
			logger.log(Level.WARNING, String.format("Creature ID=%d is gone, destroying token (%d) and notifying player (%s)", target.getData(), target.getWurmId(), performer.getName()));
			performer.getCommunicator().sendAlertServerMessage("The animal associated with this token is either dead or missing and can't be restored.");
			Items.destroyItem(target.getWurmId());
			return true;
		} catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			return true;

		}
	}
}
