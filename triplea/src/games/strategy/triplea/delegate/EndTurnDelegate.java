/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * EndTurnDelegate.java
 *
 * Created on November 2, 2001, 12:30 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * At the end of the turn collect income.
 */
@AutoSave(afterStepEnd=true)
public class EndTurnDelegate extends AbstractEndTurnDelegate
{

    protected void checkForWinner(IDelegateBridge bridge)
    {
        //only notify once
        if(m_gameOver)
            return;
        String victoryMessage = null;

        // TODO: what the heck is this used for? it doesn't even include italians or chinese, or anyone else
        PlayerID russians = m_data.getPlayerList().getPlayerID(Constants.RUSSIANS);
        PlayerID germans = m_data.getPlayerList().getPlayerID(Constants.GERMANS);
        PlayerID british = m_data.getPlayerList().getPlayerID(Constants.BRITISH);
        PlayerID japanese = m_data.getPlayerList().getPlayerID(Constants.JAPANESE);
        PlayerID americans = m_data.getPlayerList().getPlayerID(Constants.AMERICANS);

        //TODO kev see if we can abstract this from the specific players/games
        if(m_data.getProperties().get(Constants.PACIFIC_THEATER, false))
        {
            PlayerAttachment pa = PlayerAttachment.get(japanese);


            if(pa != null && Integer.parseInt(pa.getVps()) >= 22)
            {
                m_gameOver = true;
                victoryMessage = "Axis achieve VP victory";
                bridge.getHistoryWriter().startEvent(victoryMessage);
                signalGameOver(victoryMessage,bridge);
                return;
            }
        }

        // do national objectives
		if (isNationalObjectives())
		{
			determineNationalObjectives(m_data, bridge);
		}

        // create units if any owned units have the ability
        createUnits(m_data, bridge);

        // create resources if any owned units have the ability
        createResources(m_data, bridge);

        if(isWW2V2())
            return;


        if(germans == null || russians == null || british == null || japanese == null || americans == null)
            return;

        // Quick check to see who still owns their own capital
        boolean russia = TerritoryAttachment.getCapital(russians, m_data).getOwner().equals(russians);
        boolean germany = TerritoryAttachment.getCapital(germans, m_data).getOwner().equals(germans);
        boolean britain = TerritoryAttachment.getCapital(british, m_data).getOwner().equals(british);
        boolean japan = TerritoryAttachment.getCapital(japanese, m_data).getOwner().equals(japanese);
        boolean america = TerritoryAttachment.getCapital(americans, m_data).getOwner().equals(americans);


        int count = 0;
        if (!russia) count++;
        if (!britain) count++;
        if (!america) count++;
        victoryMessage = " achieve a military victory";

        if ( germany && japan && count >=2)
        {
            m_gameOver = true;
            bridge.getHistoryWriter().startEvent("Axis" + victoryMessage);
            signalGameOver(victoryMessage,bridge);
            //TODO We might want to find a more elegant way to end the game ala the TIC-TAC-TOE example
            //Added this to end the game on victory conditions
            //bridge.stopGameSequence();
        }

        if ( russia && !germany && britain && !japan && america)
        {
            m_gameOver = true;
            bridge.getHistoryWriter().startEvent("Allies" + victoryMessage);
            signalGameOver(victoryMessage,bridge);
            //Added this to end the game on victory conditions
            //bridge.stopGameSequence();
        }
    }

    /**
     * Notify all players that the game is over.
     *
     * @param status the "game over" text to be displayed to each user.
     */
    private void signalGameOver(String status, IDelegateBridge a_bridge)
    {
        // If the game is over, we need to be able to alert all UIs to that fact.
        //    The display object can send a message to all UIs.
        if (!m_gameOver)
        {
            m_gameOver = true;
            //we can't talk to the user directly
            //we need to go through an ITripleAPlayer, or an IDisplay

//            // Make sure the user really wants to leave the game.
//            int rVal = JOptionPane.showConfirmDialog(null, status +"\nDo you want to continue?", "Continue" , JOptionPane.YES_NO_OPTION);
//            if(rVal != JOptionPane.OK_OPTION)
//                a_bridge.stopGameSequence();
        }
    }

    /**
     *
     */
    private void createUnits(GameData data, IDelegateBridge bridge)
    {
    	PlayerID player = data.getSequence().getStep().getPlayerID();
    	Match<Unit> myCreatorsMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCreatesUnits);
    	CompositeChange change = new CompositeChange();
    	for (Territory t : data.getMap().getTerritories())
    	{
    		Collection<Unit> myCreators = Match.getMatches(t.getUnits().getUnits(), myCreatorsMatch);
    		if (myCreators != null && !myCreators.isEmpty())
    		{
        		Collection<Unit> toAdd = new ArrayList<Unit>();
        		Collection<Unit> toAddSea = new ArrayList<Unit>();
        		Collection<Unit> toAddLand = new ArrayList<Unit>();
    			for (Unit u : myCreators)
    			{
    				UnitAttachment ua = UnitAttachment.get(u.getType());
    	        	IntegerMap<UnitType> createsUnitsMap = ua.getCreatesUnitsList();
    	        	Collection<UnitType> willBeCreated = createsUnitsMap.keySet();
    				for (UnitType ut : willBeCreated)
    				{
    					if (UnitAttachment.get(ut).isSea() && Matches.TerritoryIsLand.match(t))
    						toAddSea.addAll(ut.create(createsUnitsMap.getInt(ut), player));
    					else if (!UnitAttachment.get(ut).isSea() && !UnitAttachment.get(ut).isAir() && Matches.TerritoryIsWater.match(t))
    						toAddLand.addAll(ut.create(createsUnitsMap.getInt(ut), player));
    					else
    						toAdd.addAll(ut.create(createsUnitsMap.getInt(ut), player));
    				}
    			}
    			if (toAdd != null && !toAdd.isEmpty())
    			{
    		        String transcriptText = player.getName() + " creates " + MyFormatter.unitsToTextNoOwner(toAdd) + " in " + t.getName();
    		        bridge.getHistoryWriter().startEvent(transcriptText);
    		        bridge.getHistoryWriter().setRenderingData(toAdd);
    		        Change place = ChangeFactory.addUnits(t, toAdd);
    		        change.add(place);
    			}
    			if (toAddSea != null && !toAddSea.isEmpty())
    			{
    				Match<Territory> myTerrs = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater);
    				Collection<Territory> waterNeighbors = data.getMap().getNeighbors(t, myTerrs);
    				if (waterNeighbors != null && !waterNeighbors.isEmpty())
    				{
    					Territory tw = waterNeighbors.iterator().next();
        		        String transcriptText = player.getName() + " creates " + MyFormatter.unitsToTextNoOwner(toAddSea) + " in " + tw.getName();
        		        bridge.getHistoryWriter().startEvent(transcriptText);
        		        bridge.getHistoryWriter().setRenderingData(toAddSea);
        		        Change place = ChangeFactory.addUnits(tw, toAddSea);
        		        change.add(place);
    				}
    			}
    			if (toAddLand != null && !toAddLand.isEmpty())
    			{
    				Match<Territory> myTerrs = new CompositeMatchAnd<Territory>(Matches.isTerritoryOwnedBy(player), Matches.TerritoryIsLand);
    				Collection<Territory> landNeighbors = data.getMap().getNeighbors(t, myTerrs);
    				if (landNeighbors != null && !landNeighbors.isEmpty())
    				{
    					Territory tl = landNeighbors.iterator().next();
        		        String transcriptText = player.getName() + " creates " + MyFormatter.unitsToTextNoOwner(toAddLand) + " in " + tl.getName();
        		        bridge.getHistoryWriter().startEvent(transcriptText);
        		        bridge.getHistoryWriter().setRenderingData(toAddLand);
        		        Change place = ChangeFactory.addUnits(tl, toAddLand);
        		        change.add(place);
    				}
    			}
    		}
    	}
    	if (change != null && !change.isEmpty())
    		bridge.addChange(change);
    }
    
    /**
     * 
     * @param data
     * @param bridge
     */
    private void createResources(GameData data, IDelegateBridge bridge)
    {
    	PlayerID player = data.getSequence().getStep().getPlayerID();
    	Match<Unit> myCreatorsMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCreatesResources);
    	CompositeChange change = new CompositeChange();

    	for (Territory t : data.getMap().getTerritories())
    	{
    		Collection<Unit> myCreators = Match.getMatches(t.getUnits().getUnits(), myCreatorsMatch);
    		if (myCreators != null && !myCreators.isEmpty())
    		{
    			for (Unit u : myCreators)
    			{
    				UnitAttachment ua = UnitAttachment.get(u.getType());
    	        	IntegerMap<Resource> createsUnitsMap = ua.getCreatesResourcesList();
    	        	Collection<Resource> willBeCreated = createsUnitsMap.keySet();
    				for (Resource r : willBeCreated)
    				{
    					int toAdd = createsUnitsMap.getInt(r);
    					if (r.getName().equals(Constants.PUS))
    						toAdd *= Properties.getPU_Multiplier(data);
    					int total = player.getResources().getQuantity(r) + toAdd;
    					if(total < 0) {
    						toAdd -= total;
    						total = 0;
    					}
        		        String transcriptText = u.getUnitType().getName() + " in " + t.getName() + " creates " + toAdd + " " + r.getName() + "; " + player.getName() + " end with " + total + " " + r.getName();
        		        bridge.getHistoryWriter().startEvent(transcriptText);
        		        Change resources = ChangeFactory.changeResourcesChange(player, r, toAdd);
    					change.add(resources);
    				}
    			}
    		}
    	}
    	if (change != null && !change.isEmpty())
    		bridge.addChange(change);
    }
    
/**
 * Determine if National Objectives have been met
 * @param data
 */
    private void determineNationalObjectives(GameData data, IDelegateBridge bridge)
    {
    	PlayerID player = data.getSequence().getStep().getPlayerID();

    	//See if the player has National Objectives
    	Set<RulesAttachment> natObjs = new HashSet<RulesAttachment>();
        Map<String, IAttachment> map = player.getAttachments();
        Iterator<String> objsIter = map.keySet().iterator();
        while(objsIter.hasNext() )
        {
            IAttachment attachment = map.get(objsIter.next());
            String name = attachment.getName();
            if (name.startsWith(Constants.RULES_OBJECTIVE_PREFIX))
            {
            	natObjs.add((RulesAttachment)attachment);
            }
        }

        //Check whether any National Objectives are met
    	Iterator<RulesAttachment> rulesIter = natObjs.iterator();
    	while(rulesIter.hasNext())
    	{
    		RulesAttachment rule = rulesIter.next();
    		boolean objectiveMet = true;
    		Integer uses = rule.getUses();
    		if( uses == 0)
    			continue;
    		objectiveMet = rule.isSatisfied(data);
    		//
    		//Check for allied unit exclusions
    		//
    		/*if(rule.getAlliedExclusionTerritories() != null)
    		{
    			//Get the listed territories
    			String[] terrs = rule.getAlliedExclusionTerritories();
    			String value = new String();

	    		//If there's only 1, it might be a 'group' (original, controlled, all)
    			if(terrs.length == 1)
    			{
	    			for(String name : terrs)
	    			{
	    				if(name.equals("original"))
	    				{	//get all originally owned territories
	    					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
	        				Collection<Territory> originalTerrs = origOwnerTracker.getOriginallyOwned(data, player);
	        				rule.setTerritoryCount(String.valueOf(originalTerrs.size()));
	        				//Colon delimit the collection as it would exist in the XML
	        				for (Territory item : originalTerrs)
	      					  	value = value + ":" + item;
	        				//Remove the leading colon
	        				value = value.replaceFirst(":", "");
	    				}
	    				else if (name.equals("controlled"))
	        			{
	        				Collection<Territory> ownedTerrs = data.getMap().getTerritoriesOwnedBy(player);
	        				rule.setTerritoryCount(String.valueOf(ownedTerrs.size()));
	        				//Colon delimit the collection as it would exist in the XML
	        				  for (Territory item : ownedTerrs)
	        					  value = value + ":" + item;
	        				  //Remove the leading colon
	        				  value = value.replaceFirst(":", "");
	        			}
	    				else if (name.equals("all"))
	        			{
	        				Collection<Territory> allTerrs = data.getMap().getTerritoriesOwnedBy(player);
	        				OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
	        				allTerrs.addAll(origOwnerTracker.getOriginallyOwned(data, player));
	        				rule.setTerritoryCount(String.valueOf(allTerrs.size()));
	        				//Colon delimit the collection as it would exist in the XML
	        				for (Territory item : allTerrs)
	      					  value = value + ":" + item;
	        				//Remove the leading colon
	        				value = value.replaceFirst(":", "");
	        			}
	    				else
	    				{	//The list just contained 1 territory
	    					value = name;
	    				}
	    			}
    			}
    			else
    			{
    				//Get the list of territories
    				Collection<Territory> listedTerrs = rule.getListedTerritories(terrs);
    				//Colon delimit the collection as it exists in the XML
    				for (Territory item : listedTerrs)
  					  value = value + ":" + item;
    				//Remove the leading colon
    				value = value.replaceFirst(":", "");
    			}

    			//create the String list from the XML/gathered territories
    			terrs = value.split(":");
    			objectiveMet = checkUnitExclusions(objectiveMet, rule.getListedTerritories(terrs), "allied", rule.getTerritoryCount(), player);
    		}

    		//
    		//Check for enemy unit exclusions (ANY UNITS)
    		//TODO Transports and Subs don't count-- perhaps list types to exclude
    		//
    		if(rule.getEnemyExclusionTerritories() != null && objectiveMet == true)
    		{
    			//Get the listed territories
    			String[] terrs = rule.getEnemyExclusionTerritories();
    			String value = new String();

	    		//If there's only 1, it might be a 'group'  (original)
    			if(terrs.length == 1)
    			{
	    			for(String name : terrs)
	    			{
	    				if(name.equals("original"))
	    				{	//get all originally owned territories
	    					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
	        				Collection<Territory> enemyTerrs = origOwnerTracker.getOriginallyOwned(data, player);
	        				rule.setTerritoryCount(String.valueOf(enemyTerrs.size()));
	        				//Colon delimit the collection as it would exist in the XML
	        				for (Territory item : enemyTerrs)
	      					  	value = value + ":" + item;
	        				//Remove the leading colon
	        				value = value.replaceFirst(":", "");
	    				}
	    				else
	    				{	//The list just contained 1 territory
	    					value = name;
	    				}
	    			}
    			}
    			else
    			{
    				//Get the list of territories
    				Collection<Territory> listedTerrs = rule.getListedTerritories(terrs);
    				//Colon delimit the collection as it exists in the XML
    				for (Territory item : listedTerrs)
  					  value = value + ":" + item;
    				//Remove the leading colon
    				value = value.replaceFirst(":", "");
    			}

    			//create the String list from the XML/gathered territories
    			terrs = value.split(":");
    			objectiveMet = checkUnitExclusions(objectiveMet, rule.getListedTerritories(terrs), "enemy", rule.getTerritoryCount(), player);
    		}

    		//Check for enemy unit exclusions (SURFACE UNITS with ATTACK POWER)
    		if(rule.getEnemySurfaceExclusionTerritories() != null && objectiveMet == true)
    		{
    			//Get the listed territories
    			String[] terrs = rule.getEnemySurfaceExclusionTerritories();
    			String value = new String();

	    		//If there's only 1, it might be a 'group'  (original)
    			if(terrs.length == 1)
    			{
	    			for(String name : terrs)
	    			{
	    				if(name.equals("original"))
	    				{	//get all originally owned territories
	    					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
	        				Collection<Territory> enemyTerrs = origOwnerTracker.getOriginallyOwned(data, player);
	        				rule.setTerritoryCount(String.valueOf(enemyTerrs.size()));
	        				//Colon delimit the collection as it would exist in the XML
	        				for (Territory item : enemyTerrs)
	      					  	value = value + ":" + item;
	        				//Remove the leading colon
	        				value = value.replaceFirst(":", "");
	    				}
	    				else
	    				{	//The list just contained 1 territory
	    					value = name;
	    				}
	    			}
    			}
    			else
    			{
    				//Get the list of territories
    				Collection<Territory> listedTerrs = rule.getListedTerritories(terrs);
    				//Colon delimit the collection as it exists in the XML
    				for (Territory item : listedTerrs)
  					  value = value + ":" + item;
    				//Remove the leading colon
    				value = value.replaceFirst(":", "");
    			}

    			//create the String list from the XML/gathered territories
    			terrs = value.split(":");
    			objectiveMet = checkUnitExclusions(objectiveMet, rule.getListedTerritories(terrs), "enemy_surface", rule.getTerritoryCount(), player);
    		}

    		//
    		//Check for Territory Ownership rules
    		//
    		if(rule.getAlliedOwnershipTerritories() != null && objectiveMet == true)
    		{
    			//Get the listed territories
    			String[] terrs = rule.getAlliedOwnershipTerritories();
    			String value = new String();

	    		//If there's only 1, it might be a 'group' (original)
    			if(terrs.length == 1)
    			{
	    			for(String name : terrs)
	    			{
	    				if(name.equals("original"))
	    				{
	    					Collection<PlayerID> players = data.getPlayerList().getPlayers();
	    					Iterator<PlayerID> playersIter = players.iterator();
	    					while(playersIter.hasNext())
	    					{
	    						PlayerID currPlayer = playersIter.next();
	    						if (data.getRelationshipTracker().isAllied(currPlayer, player))
	    						{
	    							//get all originally owned territories
	    	    					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
	    	        				Collection<Territory> originalAlliedTerrs = origOwnerTracker.getOriginallyOwned(data, currPlayer);
	    	        				rule.setTerritoryCount(String.valueOf(originalAlliedTerrs.size()));
	    	        				//Colon delimit the collection as it would exist in the XML
	    	        				for (Territory item : originalAlliedTerrs)
	    	      					  	value = value + ":" + item;
	    	        				//Remove the leading colon
	    	        				value = value.replaceFirst(":", "");
	    						}
	    					}
	    				}
	    				else if(name.equals("enemy"))
	    				{	//TODO Perhaps add a count to signify how many territories must be controlled- currently, it's ALL
	    					Collection<PlayerID> players = data.getPlayerList().getPlayers();
	    					Iterator<PlayerID> playersIter = players.iterator();
	    					while(playersIter.hasNext())
	    					{
	    						PlayerID currPlayer = playersIter.next();
	    						if (!data.getRelationshipTracker().isAllied(currPlayer, player))
	    						{
	    							//get all originally owned territories
	    	    					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
	    	        				Collection<Territory> originalEnemyTerrs = origOwnerTracker.getOriginallyOwned(data, currPlayer);
	    	        				rule.setTerritoryCount(String.valueOf(originalEnemyTerrs.size()));
	    	        				//Colon delimit the collection as it would exist in the XML
	    	        				for (Territory item : originalEnemyTerrs)
	    	      					  	value = value + ":" + item;
	    	        				//Remove the leading colon
	    	        				value = value.replaceFirst(":", "");
	    						}
	    					}
	    				}
	    				else
	    				{	//The list just contained 1 territory
	    					value = name;
	    				}
	    			}
    			}
    			else
    			{
    				//Get the list of territories
    				Collection<Territory> listedTerrs = rule.getListedTerritories(terrs);
    				//Colon delimit the collection as it exists in the XML
    				for (Territory item : listedTerrs)
  					  value = value + ":" + item;
    				//Remove the leading colon
    				value = value.replaceFirst(":", "");
    			}

    			//create the String list from the XML/gathered territories
    			terrs = value.split(":");

    			objectiveMet = checkAlliedOwnership(objectiveMet, rule.getListedTerritories(terrs), rule.getTerritoryCount(), player);
    		}

			//Direct Ownership mod by astabada

    		//
    		//Check for Direct Territory Ownership rules
    		//
			if(rule.getDirectOwnershipTerritories() != null && objectiveMet == true)
    		{
    			//Get the listed territories
    			String[] terrs = rule.getDirectOwnershipTerritories();
    			String value = new String();

	    		//If there's only 1, it might be a 'group' (original)
    			if(terrs.length == 1)
    			{
	    			for(String name : terrs)
	    			{
	    				if(name.equals("original"))
	    				{
	    					Collection<PlayerID> players = data.getPlayerList().getPlayers();
	    					Iterator<PlayerID> playersIter = players.iterator();
	    					while(playersIter.hasNext())
	    					{
	    						PlayerID currPlayer = playersIter.next();
	    						if (data.getRelationshipTracker().isAllied(currPlayer, player)) // check this again (should we be looking for allied control if this is direct ownership?)
	    						{
	    							//get all originally owned territories
	    	    					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
	    	        				Collection<Territory> originalAlliedTerrs = origOwnerTracker.getOriginallyOwned(data, currPlayer);
	    	        				rule.setTerritoryCount(String.valueOf(originalAlliedTerrs.size()));
	    	        				//Colon delimit the collection as it would exist in the XML
	    	        				for (Territory item : originalAlliedTerrs)
	    	      					  	value = value + ":" + item;
	    	        				//Remove the leading colon
	    	        				value = value.replaceFirst(":", "");
	    						}
	    					}
	    				}
	    				else if(name.equals("enemy"))
	    				{	//TODO Perhaps add a count to signify how many territories must be controlled- currently, it's ALL
	    					Collection<PlayerID> players = data.getPlayerList().getPlayers();
	    					Iterator<PlayerID> playersIter = players.iterator();
	    					while(playersIter.hasNext())
	    					{
	    						PlayerID currPlayer = playersIter.next();
	    						if (!data.getRelationshipTracker().isAllied(currPlayer, player))
	    						{
	    							//get all originally owned territories
	    	    					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
	    	        				Collection<Territory> originalEnemyTerrs = origOwnerTracker.getOriginallyOwned(data, currPlayer);
	    	        				rule.setTerritoryCount(String.valueOf(originalEnemyTerrs.size()));
	    	        				//Colon delimit the collection as it would exist in the XML
	    	        				for (Territory item : originalEnemyTerrs)
	    	      					  	value = value + ":" + item;
	    	        				//Remove the leading colon
	    	        				value = value.replaceFirst(":", "");
	    						}
	    					}
	    				}
	    				else
	    				{	//The list just contained 1 territory
	    					value = name;
	    				}
	    			}
    			}
    			else
    			{
    				//Get the list of territories
    				Collection<Territory> listedTerrs = rule.getListedTerritories(terrs);
    				//Colon delimit the collection as it exists in the XML
    				for (Territory item : listedTerrs)
  					  value = value + ":" + item;
    				//Remove the leading colon
    				value = value.replaceFirst(":", "");
    			}

    			//create the String list from the XML/gathered territories
    			terrs = value.split(":");

    			objectiveMet = checkDirectOwnership(objectiveMet, rule.getListedTerritories(terrs), rule.getTerritoryCount(), player);
    		}

    		if( rule.getAtWarPlayers()!=null && objectiveMet == true) {
    			objectiveMet = checkAtWar(player, rule.getAtWarPlayers(), rule.getAtWarCount(), data);
    		}*/
    		//
    		//If all are satisfied add the PUs for this objective
    		//
    		if (objectiveMet)
    		{
    			int toAdd = rule.getObjectiveValue();
    			toAdd *= Properties.getPU_Multiplier(m_data);
    			int total = player.getResources().getQuantity(Constants.PUS) + toAdd;
    		    if(total < 0) {
    		    	toAdd -= total;
    		    	total = 0;
    		    }
    		    Change change = ChangeFactory.changeResourcesChange(player, data.getResourceList().getResource(Constants.PUS), toAdd);
    		    //player.getResources().addResource(data.getResourceList().getResource(Constants.PUS), rule.getObjectiveValue());
                bridge.addChange(change);
        	    if( uses > 0) {
        	    	uses--;
        	    	Change use = ChangeFactory.attachmentPropertyChange(rule, new Integer(uses).toString(), "uses");
                	bridge.addChange(use);
        	    }
    			String PUMessage = player.getName() + " met a national objective for an additional " + rule.getObjectiveValue() + MyFormatter.pluralize(" PU", rule.getObjectiveValue()) +
    			"; end with " + total + MyFormatter.pluralize(" PU", total);
    			bridge.getHistoryWriter().startEvent(PUMessage);
    		}
    	} //end while
    	if(games.strategy.triplea.Properties.getTriggers(data))
    		TriggerAttachment.triggerResourceChange(player, bridge, data, null, null);
    } //end determineNationalObjectives

	private boolean isNationalObjectives()
    {
    	return games.strategy.triplea.Properties.getNationalObjectives(m_data);
    }

    private boolean isWW2V2()
    {
    	return games.strategy.triplea.Properties.getWW2V2(m_data);
    }

    /*
    //TODO: CHECK whether these function are really necessary
    /**
     * Checks for allied ownership of the collection of territories.  Once the needed number threshold is reached, the satisfied flag is set
     * to true and returned
     *
    private boolean checkAlliedOwnership(boolean satisfied, Collection<Territory> listedTerrs, int numberNeeded, PlayerID player)
    {
        int numberMet = 0;
        satisfied = false;

        Iterator<Territory> listedTerrIter = listedTerrs.iterator();

        while(listedTerrIter.hasNext())
        {
            Territory listedTerr = listedTerrIter.next();
            //if the territory owner is an ally
            if (m_data.getRelationshipTracker().isAllied(listedTerr.getOwner(), player))
            {
                numberMet += 1;
                if(numberMet >= numberNeeded)
                {
                    satisfied = true;
                    break;
                }
            }
        }
        return satisfied;
    }

    /** astabada
     * Checks for direct ownership of the collection of territories.  Once the needed number threshold is reached, the satisfied flag is set
     * to true and returned
     *
    private boolean checkDirectOwnership(boolean satisfied, Collection<Territory> listedTerrs, int numberNeeded, PlayerID player)
    {
        int numberMet = 0;
        satisfied = false;

        Iterator<Territory> listedTerrIter = listedTerrs.iterator();

        while(listedTerrIter.hasNext())
        {
            Territory listedTerr = listedTerrIter.next();
            //if the territory owner is an ally
            if (listedTerr.getOwner() == player)
            {
                numberMet += 1;
                if(numberMet >= numberNeeded)
                {
                    satisfied = true;
                    break;
                }
            }
        }
        return satisfied;
    }
    /**
     * Checks for the collection of territories to see if they have units owned by the exclType alliance.
     * It doesn't yet threshold the data
     *
    private boolean checkUnitExclusions(boolean satisfied, Collection<Territory> Territories, String exclType, int numberNeeded, PlayerID player)
    {
        int numberMet = 0;
        satisfied = false;

        Iterator<Territory> ownedTerrIter = Territories.iterator();
        //Go through the owned territories and see if there are any units owned by allied/enemy based on exclType
        while (ownedTerrIter.hasNext())
        {
            //get all the units in the territory
            Territory terr = ownedTerrIter.next();
            Collection<Unit> allUnits =  terr.getUnits().getUnits();

            if (exclType == "allied")
            {   //any allied units in the territory
                allUnits.removeAll(Match.getMatches(allUnits, Matches.unitIsOwnedBy(player)));
                Collection<Unit> playerUnits = Match.getMatches(allUnits, Matches.alliedUnit(player, m_data));
                if (playerUnits.size() < 1)
                {
                    numberMet += 1;
                    if(numberMet >= numberNeeded)
                    {
                        satisfied = true;
                        break;
                    }
                }
            }
            else if (exclType == "enemy")
            {   //any enemy units in the territory
                Collection<Unit> enemyUnits = Match.getMatches(allUnits, Matches.enemyUnit(player, m_data));
                if (enemyUnits.size() < 1)
                {
                    numberMet += 1;
                    if(numberMet >= numberNeeded)
                    {
                        satisfied = true;
                        break;
                    }
                }
            }
            else //if (exclType == "enemy_surface")
            {//any enemy units (not trn/sub) in the territory
                Collection<Unit> enemyUnits = Match.getMatches(allUnits, new CompositeMatchAnd(Matches.enemyUnit(player, m_data), Matches.UnitIsNotSub, Matches.UnitIsNotTransport));
                if (enemyUnits.size() < 1)
                {
                    numberMet += 1;
                    if(numberMet >= numberNeeded)
                    {
                        satisfied = true;
                        break;
                    }
                }
            }
        }
        return satisfied;
    }

    private boolean checkAtWar(PlayerID player, Set<PlayerID> enemies, int count, GameData data) {
        int found =0;
        for( PlayerID e:enemies)
            if(data.getRelationshipTracker().isAtWar(player,e))
                found++;
        if( count == 0)
            return count == found;
        else
            return  found >= count;
    }
     */
}
