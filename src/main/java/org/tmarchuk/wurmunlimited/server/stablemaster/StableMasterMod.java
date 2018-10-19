package org.tmarchuk.wurmunlimited.server.stablemaster;

/**
 * Created by Tyson Marchuk on 2016-04-27.
 */

// From Wurm Common
import com.wurmonline.shared.constants.ItemMaterials;

// From Wurm Unlimited Server
import com.wurmonline.server.behaviours.BehaviourList;
import com.wurmonline.server.behaviours.MethodsCreatures;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureHelper;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.intra.PlayerTransfer;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemMetaData;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.Items;
import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.NoSuchItemException;
import static com.wurmonline.server.items.ItemTypes.*;

// From Ago's modloader
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.ItemTemplatesCreatedListener;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.IdFactory;
import org.gotti.wurmunlimited.modsupport.IdType;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;
import org.gotti.wurmunlimited.modsupport.creatures.ModCreatures;

// Javassist
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

// Base Java
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StableMasterMod implements WurmServerMod, Configurable, Initable, PreInitable, ServerStartedListener, ItemTemplatesCreatedListener
{
	private static final Logger logger = Logger.getLogger(StableMasterMod.class.getName());
	
	// Constants
	private static final String ANIMAL_TOKEN_IDENTIFIER = "animalToken";
	private static final int ANIMAL_TOKEN_SIZE = 3;	// Normal as compared to 1 = tiny, 2 = small, 4 = large, 5 = huge
	private static final short ANIMAL_TOKEN_IMAGE_NUMBER = 321; // Piece of paper.
	private static final float ANIMAL_TOKEN_DIFFICULTY = 1.0f;
	private static final int ANIMAL_TOKEN_VALUE = 0;
	private static final boolean ANIMAL_TOKEN_IS_PURCHASED = false;

	// Configuration value string constants
	private static final String CONFIG_SPECIFY_STABLE_MASTER_ID = "specifyStableMasterId";
	private static final String CONFIG_STABLE_MASTER_ID = "stableMasterId";
	private static final String CONFIG_SPECIFY_ANIMAL_TOKEN_ID = "specifyAnimalTokenId";
	private static final String CONFIG_ANIMAL_TOKEN_ID = "animalTokenId";
	private static final String CONFIG_ANIMAL_TOKEN_CM_X = "animalTokenCentimetersX";
	private static final String CONFIG_ANIMAL_TOKEN_CM_Y = "animalTokenCentimetersY";
	private static final String CONFIG_ANIMAL_TOKEN_CM_Z = "animalTokenCentimetersZ";
	private static final String CONFIG_ANIMAL_TOKEN_MIN_WEIGHT_GRAMS = "animalTokenMinimumWeightGrams";
	private static final String CONFIG_ANIMAL_TOKEN_MAX_WEIGHT_GRAMS = "animalTokenMaximumWeightGrams";
	private static final String CONFIG_EXCHANGE_ANIMAL_COST_IRONS = "exchangeAnimalCostIrons";
	private static final String CONFIG_ENABLE_NO_NPC_EXCHANGE = "enableNoNpcExchange";
	private static final String CONFIG_ENABLE_SMALL_BOATS_LOAD = "enableSmallBoatsLoad";
	private static final String CONFIG_ENABLE_SERVER_TRANSFER = "enableServerTransfer";
	private static final String CONFIG_ENABLE_SERVER_TRANSFER_LOGGING = "enableServerTransferLogging";

	// Configuration values
	private boolean specifyStableMasterId = false;
	private int stableMasterId = 20001;
	private boolean specifyAnimalTokenId = false;
	private int animalTokenId = 20002;
	private int animalTokenCentimetersX = 20;
	private int animalTokenCentimetersY = 50;
	private int animalTokenCentimetersZ = 200;
	private int animalTokenMinimumWeightGrams = 50000;
	private int animalTokenMaximumWeightGrams = 75000;
	private int exchangeAnimalCostIrons = 1234;
	private boolean enableNoNpcExchange = false;
	private boolean enableSmallBoatsLoad = false;
	private boolean enableServerTransfer = true;
	private boolean enableServerTransferLogging = true;
	
	// Internal
	private StableMaster stableMasterBuilder = null;
	private Set<Long> playersTransferringNotSwitchingType = new HashSet<Long>();
	
	public static void logException(String msg, Throwable e)
	{
		if (logger != null)
			logger.log(Level.SEVERE, msg, e);
	}

	@Override
	public void configure(Properties properties)
	{
		try
		{
			// Whether or not to use a hard coded stable master creature template ID.
			this.specifyStableMasterId = Boolean.parseBoolean(properties.getProperty(CONFIG_SPECIFY_STABLE_MASTER_ID, 
				String.valueOf(this.specifyStableMasterId)));
			logger.log(Level.INFO, CONFIG_SPECIFY_STABLE_MASTER_ID + ": " + this.specifyStableMasterId);
			
			// Hard coded stable master ID to use if enabled.
			this.stableMasterId = Integer.parseInt(properties.getProperty(CONFIG_STABLE_MASTER_ID, String.valueOf(this.stableMasterId)));
			logger.log(Level.INFO, CONFIG_STABLE_MASTER_ID + ": " + this.stableMasterId);
			
			// Whether or not to use a hard coded animal token item template ID.
			this.specifyAnimalTokenId = Boolean.parseBoolean(properties.getProperty(CONFIG_SPECIFY_ANIMAL_TOKEN_ID, 
					String.valueOf(this.specifyAnimalTokenId)));
			logger.log(Level.INFO, CONFIG_SPECIFY_ANIMAL_TOKEN_ID + ": " + this.specifyAnimalTokenId);
				
			// Hard coded animal token ID to use if enabled.
			this.animalTokenId = Integer.parseInt(properties.getProperty(CONFIG_ANIMAL_TOKEN_ID, String.valueOf(this.animalTokenId)));
			logger.log(Level.INFO, CONFIG_ANIMAL_TOKEN_ID + ": " + this.animalTokenId);
			
			// Animal token centimeters in the X dimension. Used to determine volume of token for loading.
			this.animalTokenCentimetersX = Integer.parseInt(properties.getProperty(CONFIG_ANIMAL_TOKEN_CM_X, 
					String.valueOf(this.animalTokenCentimetersX)));
			logger.log(Level.INFO, CONFIG_ANIMAL_TOKEN_CM_X + ": " + this.animalTokenCentimetersX);
			
			// Animal token centimeters in the Y dimension. Used to determine volume of token for loading.
			this.animalTokenCentimetersY = Integer.parseInt(properties.getProperty(CONFIG_ANIMAL_TOKEN_CM_Y, 
					String.valueOf(this.animalTokenCentimetersY)));
			logger.log(Level.INFO, CONFIG_ANIMAL_TOKEN_CM_Y + ": " + this.animalTokenCentimetersY);
			
			// Animal token centimeters in the Z dimension. Used to determine volume of token for loading.
			this.animalTokenCentimetersZ = Integer.parseInt(properties.getProperty(CONFIG_ANIMAL_TOKEN_CM_Z, 
					String.valueOf(this.animalTokenCentimetersZ)));
			logger.log(Level.INFO, CONFIG_ANIMAL_TOKEN_CM_Z + ": " + this.animalTokenCentimetersZ);
			
			// Animal token minimum weight in grams. Applied before maximum so if larger than maximum it will be ignored.
			this.animalTokenMinimumWeightGrams = Integer.parseInt(properties.getProperty(CONFIG_ANIMAL_TOKEN_MIN_WEIGHT_GRAMS, 
					String.valueOf(this.animalTokenMinimumWeightGrams)));
			logger.log(Level.INFO, CONFIG_ANIMAL_TOKEN_MIN_WEIGHT_GRAMS + ": " + this.animalTokenMinimumWeightGrams);
			
			// Animal token maximum weight in grams. Applied after minimum so, if smaller than minimum, 
			// minimum will be ignored.
			this.animalTokenMaximumWeightGrams = Integer.parseInt(properties.getProperty(CONFIG_ANIMAL_TOKEN_MAX_WEIGHT_GRAMS, 
					String.valueOf(this.animalTokenMaximumWeightGrams)));
			logger.log(Level.INFO, CONFIG_ANIMAL_TOKEN_MAX_WEIGHT_GRAMS + ": " + this.animalTokenMaximumWeightGrams);
	
			// Exchange animal cost in irons if using NPC.
			this.exchangeAnimalCostIrons = Integer.parseInt(properties.getProperty(CONFIG_EXCHANGE_ANIMAL_COST_IRONS, 
					String.valueOf(this.exchangeAnimalCostIrons)));
			logger.log(Level.INFO, CONFIG_EXCHANGE_ANIMAL_COST_IRONS + ": " + this.exchangeAnimalCostIrons);
			
			// Whether or not to allow the exchange action to work directly on a horse for no cost.
			this.enableNoNpcExchange = Boolean.parseBoolean(properties.getProperty(CONFIG_ENABLE_NO_NPC_EXCHANGE, 
				String.valueOf(this.enableNoNpcExchange)));
			logger.log(Level.INFO, CONFIG_ENABLE_NO_NPC_EXCHANGE + ": " + this.enableNoNpcExchange);
			
			// Whether or not to allow loading of animal tokens onto smaller boats (rowboat/sailboat).
			this.enableSmallBoatsLoad = Boolean.parseBoolean(properties.getProperty(CONFIG_ENABLE_SMALL_BOATS_LOAD, 
				String.valueOf(this.enableSmallBoatsLoad)));
			logger.log(Level.INFO, CONFIG_ENABLE_SMALL_BOATS_LOAD + ": " + this.enableSmallBoatsLoad);
			
			// Whether or not to enable transferring between servers.
			this.enableServerTransfer = Boolean.parseBoolean(properties.getProperty(CONFIG_ENABLE_SERVER_TRANSFER, 
				String.valueOf(this.enableServerTransfer)));
			logger.log(Level.INFO, CONFIG_ENABLE_SERVER_TRANSFER + ": " + this.enableServerTransfer);
			
			// Whether or not to enable more detailed logging for transferring between servers.
			this.enableServerTransferLogging = Boolean.parseBoolean(properties.getProperty(CONFIG_ENABLE_SERVER_TRANSFER_LOGGING, 
				String.valueOf(this.enableServerTransferLogging)));
			logger.log(Level.INFO, CONFIG_ENABLE_SERVER_TRANSFER_LOGGING + ": " + this.enableServerTransferLogging);
		} catch (NumberFormatException e)
		{
			logger.log(Level.WARNING, "Failed to parse one of the configuration values. " + e.getMessage(), e);
		}
	}
	
	@Override
	public void preInit() 
	{
		ModActions.init();
	}

	@Override
	public void init() 
	{
		logger.log(Level.INFO, "Registering stable master template");
		ModCreatures.init();
		stableMasterBuilder = new StableMaster(specifyStableMasterId, stableMasterId);
		ModCreatures.addCreature(stableMasterBuilder);
		
		if (this.enableServerTransfer)
		{
			// Add hooks for server transfer.
			try
			{
				// void com.wurmonline.server.intra.PlayerTransfer.sendItem(final Item item, final DataOutputStream dos, final boolean dragged) throws UnsupportedEncodingException, IOException
				ClassPool classPool = HookManager.getInstance().getClassPool();
				String descriptor;
				descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
						classPool.get("com.wurmonline.server.items.Item"), 
						classPool.get("java.io.DataOutputStream"),
						CtClass.booleanType
				});
	
				HookManager.getInstance().registerHook("com.wurmonline.server.intra.PlayerTransfer", "sendItem", 
					descriptor, new InvocationHandlerFactory()
	                    {
	                        @Override
	                        public InvocationHandler createInvocationHandler()
	                        {
	                            return new InvocationHandler()
	                            {
	                                @Override
	                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	                                {
	                                	try
	                                	{
		                                	// Get arguments
		                                	Item toSend = (Item) args[0];
		                                	DataOutputStream outputStream = (DataOutputStream) args[1];
		                                	
		                                	// Call base version.
		                                	method.invoke(proxy, args);
		                                	
		                                	// Tack on a boolean specify whether or not this is an animal token 
		                                	// and if true add all the associated animal data.
		                                	if (toSend.getTemplateId() == animalTokenId)
		                                	{
												long animalId = toSend.getData();
												try {
													Creature animal = Creatures.getInstance().getCreature(animalId);
													outputStream.writeBoolean(true);
													CreatureHelper.toStream(animal, outputStream, enableServerTransferLogging);
												} catch (Exception e) {
													logException(String.format("Error getting creature %d to send with token", animalId), e);
													outputStream.writeBoolean(false);
												}
		                                	}
		                                	else
		                                	{
		                                		outputStream.writeBoolean(false);
		                                	}
	                                	} catch (IOException e)
	                                	{
	                            			logException("Failed to encode animal token.", e);
	                                        throw new RuntimeException(e);
	                                	}
	                                	return null;
	                                }
	                            };
	                        }
	                    });
				// END - void com.wurmonline.server.intra.PlayerTransfer.sendItem(final Item item, final DataOutputStream dos, final boolean dragged) throws UnsupportedEncodingException, IOException
				
				// void com.wurmonline.server.intra.IntraServerConnection.createItem(final DataInputStream dis, final float posx, final float posy, final float posz, final Set<ItemMetaData> metadataset, final boolean frozen) throws IOException
				descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
						classPool.get("java.io.DataInputStream"),
						CtClass.floatType, CtClass.floatType, CtClass.floatType,
						classPool.get("java.util.Set"), 
						CtClass.booleanType
				});
	
				HookManager.getInstance().registerHook("com.wurmonline.server.intra.IntraServerConnection", "createItem", 
					descriptor, new InvocationHandlerFactory()
	                    {
	                        @Override
	                        public InvocationHandler createInvocationHandler()
	                        {
	                            return new InvocationHandler()
	                            {
	                                @Override
	                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	                                {
	                                	try
	                                	{
		                                	// Get some arguments
	                                		DataInputStream inputStream = (DataInputStream) args[0];
	                                		float posx = (float) args[1];
	                                		float posy = (float) args[2];
	                                		float posz = (float) args[3];
	                                		@SuppressWarnings("unchecked")
											Set<ItemMetaData> createdItems = ((Set<ItemMetaData>) args[4]);
		                                	boolean frozen = (boolean) args[5];
		                                	
		                                	// Call base version.
		                                	method.invoke(proxy, args);
		                                	
		                                	// Check the boolean we tacked on specifying whether or not this is an 
		                                	// animal token and if true unpack the associated animal data.
		                                	boolean isAnimalToken = inputStream.readBoolean();
		                                	if (isAnimalToken)
		                                	{
		                                		CreatureHelper.fromStream(inputStream, posx, posy, posz, createdItems, frozen, enableServerTransferLogging);
		                                	}
	                                	} catch (IOException e)
	                                	{
	                            			logException("Failed to decode animal token.", e);
	                                        throw new RuntimeException(e);
	                                	}
	                                	return null;
	                                }
	                            };
	                        }
	                    });
				// END - void com.wurmonline.server.intra.IntraServerConnection.createItem(final DataInputStream dis, final float posx, final float posy, final float posz, final Set<ItemMetaData> metadataset, final boolean frozen) throws IOException
	
				// String com.wurmonline.server.LoginServerWebConnection.sendVehicle(byte[], byte[], long, long, 
				//	int, int, int, int, float)
				descriptor = Descriptor.ofMethod(classPool.get("java.lang.String"), new CtClass[] {
						classPool.get("byte[]"), classPool.get("byte[]"), CtClass.longType, CtClass.longType, 
						CtClass.intType, CtClass.intType, CtClass.intType, CtClass.intType, CtClass.floatType });
				
				HookManager.getInstance().registerHook("com.wurmonline.server.LoginServerWebConnection", "sendVehicle", 
					descriptor, new InvocationHandlerFactory()
	                    {
	                        @Override
	                        public InvocationHandler createInvocationHandler()
	                        {
	                            return new InvocationHandler()
	                            {
	                                @Override
	                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	                                {
	                                	String result = "Failed - Unknown";
	                                	try
	                                	{
		                                	// Get the vehicle.
		                                	long vehicleId = (long) args[3];
		                                	Item vehicle = Items.getItem(vehicleId);
		                                	
		                                	// Get all items on the vehicle and look for animal tokens
		                                	// For each animal token get the Creature (animal) and add to a list.
		                                	Item[] allItems = vehicle.getAllItems(true);
		                                	Set<Creature> allAnimals = new HashSet<Creature>();
		                                	Creatures allCreatures = Creatures.getInstance();
		                                	for (Item curItem : allItems)
		                                	{
		                                		if (curItem.getTemplateId() == animalTokenId)
		                                		{
		                                			// Get animal associated with this token.
		                                			Creature animal = allCreatures.getCreature(curItem.getData());
		                                			allAnimals.add(animal);
		                                			if (enableServerTransferLogging)
		                                			{
		                                				logger.log(Level.INFO, "Found an animal token(" + curItem.getName() + 
		                                						") referring to an animal(" + animal.getName() + 
		                                						") on a vehicle being transferred to another server.");
		                                			}
		                                		}
		                                	}
		                                	
		                                	// Call base version.
		                                	result = (String) method.invoke(proxy, args);
		                                	
		                                	// If the transfer succeeded then destroy all the animals associated
		                                	// with the animal tokens that were just transfered.
		                                	if (result.equals(""))
		                                	{
		                                		for (Creature curAnimal : allAnimals)
		                                		{
		                                			if (enableServerTransferLogging)
		                                			{
		                                				logger.log(Level.INFO, "Deleting animal(" + curAnimal.getName() + 
		                                						") associated with animal token that transferred on a vehicle.");
		                                			}
		                                			CreatureHelper.cleanCreatureItemsSafe(curAnimal);
		                                			MethodsCreatures.destroyCreature(curAnimal);
		                                		}
		                                	}
		                                	
	                                	} catch (NoSuchItemException e)
	                                	{
	                            			logException("\tFailed to get the vehicle item.", e);
	                                        throw new RuntimeException(e);
	                                	} catch (NoSuchCreatureException e)
	                                	{
	                                		logException("\tFailed to get an animal.", e);
	                                	}
	                                	return result;
	                                }
	                            };
	                        }
	                    });
				// END - String com.wurmonline.server.LoginServerWebConnection.sendVehicle(byte[], byte[], long, long, int, int, int, int, float)
	
				// void com.wurmonline.server.creatures.Communicator.sendReconnect(final String ip, 
				//		final int port, final String session)
				descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
						classPool.get("java.lang.String"), CtClass.intType, classPool.get("java.lang.String") });
				
				HookManager.getInstance().registerHook("com.wurmonline.server.creatures.Communicator", "sendReconnect", 
					descriptor, new InvocationHandlerFactory()
	                    {
	                        @Override
	                        public InvocationHandler createInvocationHandler()
	                        {
	                            return new InvocationHandler()
	                            {
	                                @Override
	                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	                                {
	                                	// Get the Player.
	                                	Communicator comm = (Communicator) proxy;
	                                	Player thePlayer = (Player) comm.player;
	                                	
                            			if (enableServerTransferLogging)
                            			{
                            				logger.log(Level.INFO, "It appears player(" + thePlayer.getName() + 
                            						") successfully transferred to another server.");
                            			}

                            			// Check if the player is transferring and not switching to/from epic.
	                                	if (playersTransferringNotSwitchingType.contains(thePlayer.getWurmId()))
	                                	{
	                                    	// Get all the player items.
	                                    	Item[] playerItems = thePlayer.getAllItems();
	                                    	
	                                    	// Go through all the items looking for tokens
	                                    	Creatures allCreatures = Creatures.getInstance();
	                                    	for (Item curItem : playerItems)
	                                    	{
	                                        	// If it's an animal token get the associated animal
	                                    		if (curItem.getTemplateId() == animalTokenId)
	                                    		{
	           	                                	Creature animal = null;
	           	                                	try
	           	                                	{
	           	                                		// Get animal associated with this token.
	                                        			animal = allCreatures.getCreature(curItem.getData());
														if (enableServerTransferLogging)
														{
															logger.log(Level.INFO, "Found an animal token(" + curItem.getName() +
																	") referring to an animal(" + animal.getName() +
																	") in a player's inventory being transferred to another server of the same type. Animal is being deleted.");
														}
														CreatureHelper.cleanCreatureItemsSafe(animal);
														MethodsCreatures.destroyCreature(animal);
													} catch (NoSuchCreatureException e)
	           	                                	{
	           	                                		logger.log(Level.WARNING, "Failed to get animal associated with animal token. " + e.getMessage(), e);
	           	                                	}
	                                    		}
	                                    	}
	                                	}
	                                	
	                                	// Call base version.
	                                	return method.invoke(proxy, args);
	                                	
	                                }
	                            };
	                        }
	                    });
				// END - void com.wurmonline.server.creatures.Communicator.sendReconnect(final String ip, final int port, final String session)
	
				// boolean com.wurmonline.server.intra.PlayerTransfer.poll()
				descriptor = Descriptor.ofMethod(CtClass.booleanType, new CtClass[] {});
				
				HookManager.getInstance().registerHook("com.wurmonline.server.intra.PlayerTransfer", "poll", 
						descriptor, new InvocationHandlerFactory()
		                    {
		                        @Override
		                        public InvocationHandler createInvocationHandler()
		                        {
		                            return new InvocationHandler()
		                            {
		                                @Override
		                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
		                                {
		                                	// Call base version.
		                                	boolean doneTransfer = (boolean) method.invoke(proxy, args);
		                                	
		                                	try
		                                	{
			                                	// Get the player and whether or not they're transferring to/from epic.
			                                	Player thePlayer = ReflectionUtil.getPrivateField(proxy, 
			                                			ReflectionUtil.getField(PlayerTransfer.class, "player"));
			                                	boolean toOrFromEpic = ReflectionUtil.getPrivateField(proxy, 
			                                			ReflectionUtil.getField(PlayerTransfer.class, "toOrFromEpic"));
			                                	
			                                	if (doneTransfer && !toOrFromEpic)
			                                	{
				                        			// If done and we previously added them then remove them from 
			                                		// the list because they have presumably either transferred or 
			                                		// failed.
			                                		playersTransferringNotSwitchingType.remove(thePlayer.getWurmId());
		                                			if (enableServerTransferLogging)
		                                			{
		                                				logger.log(Level.INFO, "Player(" + thePlayer.getName() + 
		                                						") is either done being transferred to another server of the same type or is transferring between servers of different types.");
		                                			}
			                                	}
			                                	else if (!toOrFromEpic && !playersTransferringNotSwitchingType.contains(
			                                			thePlayer.getWurmId()))
			                                	{
				                        			// If player is not switching to or from epic and they're not 
			                                		// in the list and this isn't done add them to the list of 
			                                		// players we track to wipe their animal tokens if they end up 
				                        			// transferring (sendReconnect)
			                                		playersTransferringNotSwitchingType.add(thePlayer.getWurmId());
		                                			if (enableServerTransferLogging)
		                                			{
		                                				logger.log(Level.INFO, "Found a player(" + thePlayer.getName() + 
		                                						") being transferred to another server of the same type.");
		                                			}
			                                	}
		                                	} catch (NoSuchFieldException e)
		                                	{
		                                		logger.log(Level.WARNING, "Unable to get private fields for PlayerTransfer class :" + e.getMessage(), e);
		                                	}
		                                	
		                                	return doneTransfer;
		                                }
		                            };
		                        }
		                    });			
				// END - boolean com.wurmonline.server.intra.PlayerTransfer.poll()
				
			} catch (NotFoundException e)
			{
	            logException("Failed to create hooks for " + StableMasterMod.class.getName(), e);
	            throw new HookException(e);
			}
		}
	}

	@Override
	public void onItemTemplatesCreated() 
	{
		// Create Horse Redemption Token Item Template.
		if (!this.specifyAnimalTokenId)
		{
			this.animalTokenId = IdFactory.getIdFor(ANIMAL_TOKEN_IDENTIFIER, IdType.ITEMTEMPLATE);
		}
		logger.log(Level.INFO, "Creating Animal Token item template with ID: " + 
				this.animalTokenId + ".");

		try
		{
			short [] animalTokenItemTypes = null;
			if (this.enableServerTransfer)
			{
				animalTokenItemTypes = new short[] 
					{ ITEM_TYPE_LEATHER, ITEM_TYPE_MEAT, ITEM_TYPE_INDESTRUCTIBLE,
						ITEM_TYPE_NODROP, ITEM_TYPE_HASDATA, ITEM_TYPE_NORENAME,
						ITEM_TYPE_FLOATING, ITEM_TYPE_NAMED,
						ITEM_TYPE_NOBANK, ITEM_TYPE_MISSION, ITEM_TYPE_NODISCARD, 
						ITEM_TYPE_NEVER_SHOW_CREATION_WINDOW_OPTION, ITEM_TYPE_NO_IMPROVE
					};
			}
			else
			{
				animalTokenItemTypes = new short[] 
						{ ITEM_TYPE_LEATHER, ITEM_TYPE_MEAT, ITEM_TYPE_INDESTRUCTIBLE,
							ITEM_TYPE_NODROP, ITEM_TYPE_HASDATA, ITEM_TYPE_NORENAME,
							ITEM_TYPE_FLOATING, ITEM_TYPE_NAMED, ITEM_TYPE_SERVERBOUND,
							ITEM_TYPE_NOBANK, ITEM_TYPE_MISSION, ITEM_TYPE_NODISCARD, 
							ITEM_TYPE_NEVER_SHOW_CREATION_WINDOW_OPTION, ITEM_TYPE_NO_IMPROVE
						};
			}
			ItemTemplateFactory.getInstance().createItemTemplate(
					animalTokenId, ANIMAL_TOKEN_SIZE, 
					"animal token", "animal tokens", 
					"excellent", "good", "ok", "poor", 
					"A token to reclaim your animal from the stable master.", 
					animalTokenItemTypes, ANIMAL_TOKEN_IMAGE_NUMBER, 
					BehaviourList.itemBehaviour, 0, Long.MAX_VALUE, 
					animalTokenCentimetersX, animalTokenCentimetersY, 
					animalTokenCentimetersZ, (int) MiscConstants.NOID, 
					MiscConstants.EMPTY_BYTE_PRIMITIVE_ARRAY, 
					"model.writ.", ANIMAL_TOKEN_DIFFICULTY, 
					animalTokenMinimumWeightGrams, ItemMaterials.MATERIAL_PAPER, 
					ANIMAL_TOKEN_VALUE, ANIMAL_TOKEN_IS_PURCHASED, 0);
       
		} catch (IOException ioEx)
		{
			logException("Failed to create Animal Token item template.", ioEx);
            throw new RuntimeException(ioEx);
		}
	}

	@Override
	public void onServerStarted()
	{
		// Get the stable master ID that was ultimately used.
		stableMasterId = stableMasterBuilder.getTemplateId();

		logger.log(Level.INFO, "Registering exchange/redeem/load actions.");
		logger.log(Level.INFO, "animalTokenId = " + animalTokenId);
		logger.log(Level.INFO, "stableMasterId = " + stableMasterId);
		ModActions.registerAction(new ExchangeAction(animalTokenId, stableMasterId, animalTokenMinimumWeightGrams,
				animalTokenMaximumWeightGrams, exchangeAnimalCostIrons, enableNoNpcExchange));
		ModActions.registerAction(new RedeemAction(animalTokenId));
		ModActions.registerAction(new LoadTokenAction(animalTokenId, enableSmallBoatsLoad));
		ModActions.registerAction(new UnloadTokenAction(animalTokenId));
	}

}
