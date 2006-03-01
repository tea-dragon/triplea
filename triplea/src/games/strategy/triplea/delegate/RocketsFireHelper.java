/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.*;

import java.util.*;

/**
 * Logic to fire rockets.
 */
public class RocketsFireHelper
{

    public RocketsFireHelper()
    {

    }

    public void fireRockets(IDelegateBridge bridge, GameData data, PlayerID player)
    {

        boolean is4thEdition = data.getProperties().get(Constants.FOURTH_EDITION, false);

        Set<Territory> rocketTerritories = getTerritoriesWithRockets(data, player);
        if (rocketTerritories.isEmpty())
        {
            getRemote(bridge).reportMessage("No aa guns to fire rockets with");
            return;
        }

        if (is4thEdition)
            fire4thEdition(data, player, rocketTerritories, bridge);
        else
            fire3rdEdition(data, player, rocketTerritories, bridge);

    }

    private void fire4thEdition(GameData data, PlayerID player, Set<Territory> rocketTerritories, IDelegateBridge bridge)
    {
        Set<Territory> attackedTerritories = new HashSet<Territory>();
        Iterator<Territory> iter = rocketTerritories.iterator();
        while (iter.hasNext())
        {
            Territory territory = iter.next();
            Set<Territory> targets = getTargetsWithinRange(territory, data, player);
            targets.removeAll(attackedTerritories);
            if (targets.isEmpty())
                continue;
            Territory target = getTarget(targets, player, bridge, territory);
            if (target != null)
            {
                attackedTerritories.add(target);
                fireRocket(player, target, bridge, data, territory);
            }
        }
    }

    private void fire3rdEdition(GameData data, PlayerID player, Set<Territory> rocketTerritories, IDelegateBridge bridge)
    {
        Set<Territory> targets = new HashSet<Territory>();
        Iterator<Territory> iter = rocketTerritories.iterator();
        while (iter.hasNext())
        {
            Territory territory = iter.next();
            targets.addAll(getTargetsWithinRange(territory, data, player));
        }

        if (targets.isEmpty())
        {
            getRemote(bridge).reportMessage("No targets to attack with rockets");
            return;
        }

        Territory attacked = getTarget(targets, player, bridge, null);
        if (attacked != null)
            fireRocket(player, attacked, bridge, data, null);
    }

    private Set<Territory> getTerritoriesWithRockets(GameData data, PlayerID player)
    {

        Set<Territory> territories = new HashSet<Territory>();

        CompositeMatch<Unit> ownedAA = new CompositeMatchAnd<Unit>();
        ownedAA.add(Matches.UnitIsAA);
        ownedAA.add(Matches.unitIsOwnedBy(player));

        Iterator iter = data.getMap().iterator();
        while (iter.hasNext())
        {
            Territory current = (Territory) iter.next();
            if (current.isWater())
                continue;

            if (current.getUnits().someMatch(ownedAA))
                territories.add(current);
        }
        return territories;
    }

    private Set<Territory> getTargetsWithinRange(Territory territory, GameData data, PlayerID player)
    {

        Collection possible = data.getMap().getNeighbors(territory, 3);

        CompositeMatch<Unit> enemyFactory = new CompositeMatchAnd<Unit>();
        enemyFactory.add(Matches.UnitIsFactory);
        enemyFactory.add(Matches.enemyUnit(player, data));

        Set<Territory> hasFactory = new HashSet<Territory>();

        Iterator iter = possible.iterator();
        while (iter.hasNext())
        {
            Territory current = (Territory) iter.next();
            if (current.getUnits().someMatch(enemyFactory))
                hasFactory.add(current);
        }
        return hasFactory;
    }

    private Territory getTarget(Collection<Territory> targets, PlayerID player, IDelegateBridge bridge, Territory from)
    {
        //ask even if there is only once choice
        //that will allow the user to not attack if he doesnt want to
        
        return ((ITripleaPlayer) bridge.getRemote()).whereShouldRocketsAttack(targets, from);
    }

    private void fireRocket(PlayerID player, Territory attackedTerritory, IDelegateBridge bridge, GameData data, Territory attackFrom)
    {

        PlayerID attacked = attackedTerritory.getOwner();
        Resource ipcs = data.getResourceList().getResource(Constants.IPCS);
        //int cost = bridge.getRandom(Constants.MAX_DICE);

        int cost = bridge.getRandom(Constants.MAX_DICE, "Rocket fired by " + player.getName() + " at " + attacked.getName());

        //account for 0 base
        cost++;

        //in fourth edtion, limit rocket attack cost to
        //production value of factory.
        if (data.getProperties().get(Constants.FOURTH_EDITION, false))
        {
            int territoryProduction = TerritoryAttachment.get(attackedTerritory).getProduction();
            // If we are limiting total ipcs lost then take that into
            // account
            if (data.getProperties().get(Constants.IPC_CAP, false))
            {
                int alreadyLost = DelegateFinder.moveDelegate(data).ipcsAlreadyLost(attackedTerritory);
                territoryProduction -= alreadyLost;
                territoryProduction = Math.max(0, territoryProduction);
            }

            if (cost > territoryProduction)
            {
                cost = territoryProduction;
            }
        }

        // Trying to remove more IPCs than the victim has is A Bad Thing[tm]
        int availForRemoval = attacked.getResources().getQuantity(ipcs);
        if (cost > availForRemoval)
            cost = availForRemoval;

        // Record the ipcs lost
        DelegateFinder.moveDelegate(data).ipcsLost(attackedTerritory, cost);

        getRemote(bridge).reportMessage("Rocket attack in " + attackedTerritory.getName() + " costs:" + cost);
      
        String transcriptText = attacked.getName() + " lost " + cost + " ipcs to rocket attack by " + player.getName();
        bridge.getHistoryWriter().startEvent(transcriptText);

        Change rocketCharge = ChangeFactory.changeResourcesChange(attacked, ipcs, -cost);
        bridge.addChange(rocketCharge);
        
        //this is null in 3rd edition
        if(attackFrom != null)
        {
            List<Unit> units = attackFrom.getUnits().getMatches(new CompositeMatchAnd(Matches.UnitIsAA, Matches.unitIsOwnedBy(player) ));
            
            if(units.size() > 0)
            {
                //only one fired
                DelegateFinder.moveDelegate(data).markNoMovement( Collections.singleton(units.get(0)));
            }
            else
            {
                new IllegalStateException("No aa guns?" + attackFrom.getUnits().getUnits());
            }
        }
        

    }

    
    private ITripleaPlayer getRemote(IDelegateBridge bridge)
    {
        return (ITripleaPlayer) bridge.getRemote();
    }
    
}