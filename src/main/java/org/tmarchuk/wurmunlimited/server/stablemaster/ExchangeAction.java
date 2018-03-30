package org.tmarchuk.wurmunlimited.server.stablemaster;

/**
 * Created by Tyson Marchuk on 2016-04-27.
 */

// From Wurm Unlimited Server
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureHelper;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.ExchangeAnimalQuestion;

// From Ago's modloader
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

// Base Java
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExchangeAction implements ModAction, BehaviourProvider, ActionPerformer
{
	private static Logger logger = Logger.getLogger(ExchangeAction.class.getName());
	
	// Constants
	private static final int HORSE_TEMPLATE_ID = 64;
	private static final float ANIMAL_TOKEN_QUALITY = 100.0f;
	
	// Configuration
	private static int animalTokenId;
	private final int stableMasterId;
	private static int animalTokenMinimumWeightGrams;
	private static int animalTokenMaximumWeightGrams;
	private final int exchangeAnimalCostIrons;
	private final boolean enableNoNpcExchange;
	
	// Action data
	private final short actionId;
	private final ActionEntry actionEntry;

	public ExchangeAction(int animalTokenId, int stableMasterId, int animalTokenMinimumWeightGrams, 
			int animalTokenMaximumWeightGrams, int exchangeAnimalCostIrons, boolean enableNoNpcExchange) 
	{
		ExchangeAction.animalTokenId = animalTokenId;
		this.stableMasterId = stableMasterId;
		ExchangeAction.animalTokenMinimumWeightGrams = animalTokenMinimumWeightGrams;
		ExchangeAction.animalTokenMaximumWeightGrams = animalTokenMaximumWeightGrams;
		this.exchangeAnimalCostIrons = exchangeAnimalCostIrons;
		this.enableNoNpcExchange = enableNoNpcExchange;
		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(actionId, "Exchange animal", "exchanging", new int[] { 0 /* ACTION_TYPE_QUICK */, 48 /* ACTION_TYPE_ENEMY_ALWAYS */, 37 /* ACTION_TYPE_NEVER_USE_ACTIVE_ITEM */});
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

	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) 
	{
		return getBehavioursFor(performer, target);
	}

	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Creature target) 
	{
		// If we are configured to allow direct exchanges for no cost check if this is an animal we are leading.
		if (this.enableNoNpcExchange && (performer instanceof Player) && 
				target.leader == performer) 
		{
			return Arrays.asList(actionEntry);
		} 
		
		// Check if we're using a stable master and leading one or more animals.
		if ((performer instanceof Player) &&
			(target.getTemplate().getTemplateId() == stableMasterId) && 
			(performer.getNumberOfFollowers() > 0))
		{
			return Arrays.asList(actionEntry);
		}
		
		// Doesn't apply.
		return null;
	}

	@Override
	public short getActionId() {
		return actionId;
	}

	@Override
	public boolean action(Action action, Creature performer, Creature target, short num, float counter) 
	{
		int numLedAnimals = performer.getNumberOfFollowers();
		int numAnimalsToExchange = 0;
		Creature[] animalsToExchange = new Creature[] {};
        int totalWeight = 0;
        boolean useStableMaster = false;

		// Handle the case where we're talking to a stable master.
		if ((target.getTemplate().getTemplateId() == stableMasterId) && (numLedAnimals > 0))
		{
			useStableMaster = true;
			numAnimalsToExchange = numLedAnimals;
	        animalsToExchange = performer.getFollowers();
	        for (Creature creat : animalsToExchange)
	        {
	        	totalWeight += getAnimalTokenWeight(creat);
	        }
		}
		else if (this.enableNoNpcExchange && (target.leader == performer))
		{
			numAnimalsToExchange = 1;
			totalWeight = getAnimalTokenWeight(target);
		}
		else
		{
        	performer.getCommunicator().sendNormalServerMessage("Nothing to exchange!");
			return true;
		}
		
		// Make sure performer has space for all the tokens in terms of item number.
		final int MAX_NUM_ITEMS = 100;
        if (!performer.getInventory().mayCreatureInsertItem() && 
        		((performer.getInventory().getNumItemsNotCoins() + numAnimalsToExchange) <= MAX_NUM_ITEMS)) 
        {
            performer.getCommunicator().sendNormalServerMessage("You do not have enough room in your inventory.");
            return true;
        }
        
        // Make sure performer can carry all the tokens without being unable to move.
        if (!performer.canCarry(totalWeight))
        {
            performer.getCommunicator().sendNormalServerMessage("You would not be able to carry the animal token(s). You need to drop some things first.");
            return true;
        }

        // Handle processing with the stable master.
        if (useStableMaster)
		{
			// Ask user to pay.
			final ExchangeAnimalQuestion eQuestion = new ExchangeAnimalQuestion(performer, 
					animalsToExchange, exchangeAnimalCostIrons);
			eQuestion.sendQuestion();
			return true;
		}
        else	// Above we've checked that we're enabled to exchange animals for no cost without an NPC.
        {
        	// Do the exchange
        	exchangeAnimalForToken(performer, target);
        	return true;
        }
	}

	@Override
	public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) 
	{
		return action(action, performer, target, num, counter);
	}

	public static void exchangeAnimalForToken(final Creature performer, final Creature animal)
	{
        try 
		{
			// Create new animal token from animal.
			Item animalToken = ItemFactory.createItem(ExchangeAction.animalTokenId, 
					ANIMAL_TOKEN_QUALITY, performer.getName());
			animalToken.setDescription(getAnimalDescription(animal));
			animalToken.setName(getAnimalName(animal));
			animalToken.setData(animal.getWurmId());
			animalToken.setWeight(getAnimalTokenWeight(animal), false);
			animalToken.setLastOwnerId(performer.getWurmId());
			animalToken.setFemale(animal.getSex() == 1);

			// Add token to player's inventory.
			performer.getInventory().insertItem(animalToken, true);
			
			// Remove animal from world.
			CreatureHelper.hideCreature(animal);

			// Let the player know.
			performer.getCommunicator().sendNormalServerMessage("You exchange your animal with the stable master for an animal token." );
		} catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	private static int getAnimalTokenWeight(Creature animal)
	{
		int calculatedAnimalWeight = (int) animal.getStatus().getBody().getWeight(animal.getStatus().fat);
		int animalTokenWeight = Math.min(animalTokenMaximumWeightGrams, 
				Math.max(animalTokenMinimumWeightGrams, calculatedAnimalWeight));
		return animalTokenWeight;
	}
	
	private static String getAnimalDescription(Creature target)
	{
		String toReturn = target.getName();
		String typeName = target.getTypeName().toLowerCase();
        if(target.isHorse() || (target.getTemplate().isBlackOrWhite && target.hasTrait(17)))
        {
        	toReturn += " - " + target.getTemplate().getColourName(target.getStatus());
			if (!toReturn.contains(typeName))
				toReturn += " " + typeName;
        } else if (!toReturn.contains(typeName)) {
        	toReturn += " - " + typeName;
		}
        return toReturn;
	}

	private static String getAnimalName(Creature target)
	{
		String toReturn = "animal token";
        return toReturn;
	}
}
