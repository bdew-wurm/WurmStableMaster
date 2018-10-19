package org.tmarchuk.wurmunlimited.server.stablemaster;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.behaviours.CreatureBehaviour;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ActionPropagation;

import java.lang.reflect.InvocationTargetException;

public class ExamineTokenPerformer implements ActionPerformer {
    private final int animalTokenId;

    public ExamineTokenPerformer(int animalTokenId) {
        this.animalTokenId = animalTokenId;
    }

    @Override
    public short getActionId() {
        return Actions.EXAMINE;
    }

    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        if (target.getTemplateId() == animalTokenId) {

            try {
                Creature theAnimal = Creatures.getInstance().getCreature(target.getData());
                performer.getCommunicator().sendNormalServerMessage(String.format("This is a token for %s.", theAnimal.getName()));
                ReflectionUtil.callPrivateMethod(null, ReflectionUtil.getMethod(CreatureBehaviour.class, "handle_EXAMINE", new Class[]{Creature.class, Creature.class}), performer, theAnimal);
            } catch (NoSuchCreatureException e) {
                performer.getCommunicator().sendNormalServerMessage("The animal associated with this token is either dead or missing.");
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                performer.getCommunicator().sendNormalServerMessage("Something went wrong. Try again or contact support.");
            }

            return propagate(action, ActionPropagation.FINISH_ACTION, ActionPropagation.NO_SERVER_PROPAGATION, ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
        } else {
            return propagate(action, ActionPropagation.CONTINUE_ACTION, ActionPropagation.SERVER_PROPAGATION, ActionPropagation.ACTION_PERFORMER_PROPAGATION);
        }
    }
}
