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
 * MoveValidator.java
 *
 * Created on November 9, 2001, 4:05 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;

import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.*;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;

/**
 *
 * @author  Sean Bridges
 *
 * Provides some static methods for validating movement.
 */
public class MoveValidator
{

    public static final String TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE = "Transport has already unloaded units in a previous phase";
    public static final String TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO = "Transport has already unloaded units to ";
    public static final String CANNOT_LOAD_AND_UNLOAD_AN_ALLIED_TRANSPORT_IN_THE_SAME_ROUND = "Cannot load and unload an allied transport in the same round";
    public static final String CANT_MOVE_THROUGH_IMPASSIBLE = "Can't move through impassible territories";
    public static final String TOO_POOR_TO_VIOLATE_NEUTRALITY = "Not enough money to pay for violating neutrality";
    public static final String NOT_ALL_AIR_UNITS_CAN_LAND = "Not all air units can land";

    /**
     * Tests the given collection of units to see if they have the movement neccessary
     * to move.
     * @arg alreadyMoved maps Unit -> movement
     */
    public static boolean hasEnoughMovement(Collection<Unit> units, int length)
    {
        for (Unit unit : units)
        {
            if (!hasEnoughMovement(unit, length))
                return false;
        }
        return true;
    }

    /**
     * Tests the given unit to see if it has the movement neccessary
     * to move.
     * @arg alreadyMoved maps Unit -> movement
     */
    public static boolean hasEnoughMovement(Unit unit, int length)
    {
        int left = TripleAUnit.get(unit).getMovementLeft();
        if(left == -1 || left < length)
            return false;
        return true;
    }

    /**
     * Checks that there are no enemy units on the route except possibly at the end.
     * Submerged enemy units are not considered as they don't affect
     * movement.
     * AA and factory dont count as enemy.
     */
    public static boolean onlyAlliedUnitsOnPath(Route route, PlayerID player, GameData data)
    {
        CompositeMatch<Unit> alliedOrNonCombat = new CompositeMatchOr<Unit>(Matches.UnitIsAAOrFactory, Matches.alliedUnit(player, data));

        // Submerged units do not interfere with movement
        // only relevant for 4th edition
        alliedOrNonCombat.add(Matches.unitIsSubmerged(data));
        
        for(int i = 0; i < route.getLength() - 1; i++)
        {
            Territory current = route.at(i);
            if(!current.getUnits().allMatch( alliedOrNonCombat))
                return false;
        }
        return true;
    }

    public static boolean enemyDestroyerOnPath(Route route, PlayerID player, GameData data)
    {
        Match<Unit> enemyDestroyer = new CompositeMatchAnd<Unit>(Matches.UnitIsDestroyer, Matches.enemyUnit(player, data));
        for(int i = 0; i < route.getLength() - 1; i++)
        {
            Territory current = route.at(i);
            if(current.getUnits().someMatch( enemyDestroyer))
                return true;
        }
        return false;
    }

    
    private static boolean getEditMode(GameData data)
    {
        return EditDelegate.getEditMode(data);
    }

    public static boolean hasConqueredNonBlitzedOnRoute(Route route, GameData data)
    {
        for(int i = 0; i < route.getLength() - 1; i++)
        {
            Territory current = route.at(i);
            if(MoveDelegate.getBattleTracker(data).wasConquered(current) 
                    && !MoveDelegate.getBattleTracker(data).wasBlitzed(current))
                return true;
        }
        return false;

    }


    public static boolean isBlitzable(Territory current, GameData data, PlayerID player)
    {
        if(current.isWater())
            return false;

        //cant blitz on neutrals
        if(current.getOwner().isNull())
            return false;

        if(MoveDelegate.getBattleTracker(data).wasConquered(current) 
                && !MoveDelegate.getBattleTracker(data).wasBlitzed(current))
            return false;
        
        CompositeMatch<Unit> blitzableUnits = new CompositeMatchOr<Unit>();
        blitzableUnits.add(Matches.alliedUnit(player, data));
        boolean fourthEdition = data.getProperties().get(Constants.FOURTH_EDITION, false);
        //4th edition, cant blitz through factories and aa guns
        //2nd edition you can 
        if(!fourthEdition)
        {
            blitzableUnits.add(Matches.UnitIsAAOrFactory);
        }
        
        if(!current.getUnits().allMatch(blitzableUnits))
            return false;
        
        return true;
    }
    
    


    public static boolean isUnload(Route route)
    {
        if(route.getLength() == 0)
            return false;
        return route.getStart().isWater() && !route.getEnd().isWater();
    }

    public static boolean isLoad(Route route)
    {
        return !route.getStart().isWater() && route.getEnd().isWater();
    }




    public static boolean hasNeutralBeforeEnd(Route route)
    {
        for(int i = 0; i < route.getLength() - 1; i++)
        {
            Territory current = route.at(i);
            //neutral is owned by null and is not sea
            if(!current.isWater() && current.getOwner().equals(PlayerID.NULL_PLAYERID))
                return true;
        }
        return false;
    }

    public static int getTransportCost(Collection<Unit> units)
  {
    if(units == null)
      return 0;

    int cost = 0;
    Iterator<Unit> iter = units.iterator();
    while (iter.hasNext())
    {
      Unit item = (Unit) iter.next();
      cost += UnitAttachment.get(item.getType()).getTransportCost();
    }
    return cost;
  }

     public static Collection<Unit> getUnitsThatCantGoOnWater(Collection<Unit> units)
    {
        Collection<Unit> retUnits = new ArrayList<Unit>();
        for (Unit unit : units)
        {
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            if(!ua.isSea() && !ua.isAir() && ua.getTransportCost() == -1)
                retUnits.add(unit);
        }
        return retUnits;
    }

    public static boolean hasUnitsThatCantGoOnWater(Collection<Unit> units)
    {
        return !getUnitsThatCantGoOnWater(units).isEmpty();
    }


    public static int carrierCapacity(Collection<Unit> units)
    {
        int sum = 0;
        Iterator<Unit> iter = units.iterator();
        while(iter.hasNext())
        {
            Unit unit = (Unit) iter.next();
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            if(ua.getCarrierCapacity() != -1)
            {
                sum+=ua.getCarrierCapacity();
            }
        }
        return sum;
    }

    public static int carrierCost(Collection<Unit> units)
    {
        int sum = 0;
        Iterator<Unit> iter = units.iterator();
        while(iter.hasNext())
        {
            Unit unit = (Unit) iter.next();
            UnitAttachment ua = UnitAttachment.get(unit.getType());
            if(ua.getCarrierCost() != -1)
                sum+=ua.getCarrierCost();
        }
        return sum;
    }

    public static boolean hasWater(Route route)
    {
        if(route.getStart().isWater())
            return true;

        return route.someMatch(Matches.TerritoryIsWater);
    }


    public static boolean hasLand(Route route)
    {
        if(!route.getStart().isWater())
            return true;

        for(int i = 0; i < route.getLength(); i++)
        {
            Territory t = route.at(i);
            if(! t.isWater())
                return true;
        }
        return false;
    }

    /**
     * Returns true if the given air units can land in the
     * given territory.
     * Does not take into account whether a battle has been
     * fought in the territory already.
     *
     * Note units must only be air units
     */
    public static boolean canLand(Collection<Unit> airUnits, Territory territory, PlayerID player, GameData data)
    {
        if( !Match.allMatch(airUnits, Matches.UnitIsAir))
            throw new IllegalArgumentException("can only test if air will land");


        if(!territory.isWater() 
               && MoveDelegate.getBattleTracker(data).wasConquered(territory))
            return false;

        if(territory.isWater())
        {
            //if they cant all land on carriers
            if(! Match.allMatch(airUnits, Matches.UnitCanLandOnCarrier))
                return false;

            //when doing the calculation, make sure to include the units
            //in the territory
            Set<Unit> friendly = new HashSet<Unit>();
            friendly.addAll(getFriendly(territory, player, data));
            friendly.addAll(airUnits);

            //make sure we have the carrier capacity
            int capacity = carrierCapacity(friendly);
            int cost = carrierCost(friendly);
            return  capacity >=  cost;
        }
        else
        {
            return isFriendly(player, territory.getOwner(), data);
        }
    }

    public static Collection<Unit> getNonLand(Collection<Unit> units)
    {
        CompositeMatch<Unit> match = new CompositeMatchOr<Unit>();
        match.add(Matches.UnitIsAir);
        match.add(Matches.UnitIsSea);
        return Match.getMatches(units, match);
    }

    public static Collection<Unit> getFriendly(Territory territory, PlayerID player, GameData data)
    {
        return territory.getUnits().getMatches(Matches.alliedUnit(player,data));
    }

    public static boolean isFriendly(PlayerID p1, PlayerID p2, GameData data)
    {
        if(p1.equals(p2) )
            return true;
        else return data.getAllianceTracker().isAllied(p1,p2);
    }

    public static boolean ownedByFriendly(Unit unit, PlayerID player, GameData data)
    {
        PlayerID owner = unit.getOwner();
        return(isFriendly(owner, player, data));
    }


    public static int getMaxMovement(Collection<Unit> units)
    {
        if(units.size() == 0)
            throw new IllegalArgumentException("no units");
        int max = 0;
        Iterator<Unit> iter = units.iterator();
        while(iter.hasNext())
        {
            Unit unit = (Unit) iter.next();
            int left = TripleAUnit.get(unit).getMovementLeft();
            max = Math.max(left, max);
        }
        return max;
    }

    
    public static int getLeastMovement(Collection<Unit> units)
    {
        if(units.size() == 0)
            throw new IllegalArgumentException("no units");
        int least = Integer.MAX_VALUE;
        Iterator<Unit> iter = units.iterator();
        while(iter.hasNext())
        {
            Unit unit = (Unit) iter.next();
            int left = TripleAUnit.get(unit).getMovementLeft();
            least = Math.min(left, least);
        }
        return least;
    }


    public static int getTransportCapacityFree(Territory territory, PlayerID id, GameData data, TransportTracker tracker)
    {
        Match<Unit> friendlyTransports = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport,
                                                         Matches.alliedUnit(id, data));
        Collection<Unit> transports = territory.getUnits().getMatches(friendlyTransports);
        int sum = 0;
        Iterator<Unit> iter = transports.iterator();
        while(iter.hasNext())
        {
            Unit transport = (Unit) iter.next();
            sum += tracker.getAvailableCapacity(transport);
        }
        return sum;
    }

    public static boolean hasSomeLand(Collection<Unit> units)
    {
        Match<Unit> notAirOrSea = new CompositeMatchAnd<Unit>(Matches.UnitIsNotAir, Matches.UnitIsNotSea);
        return Match.someMatch(units, notAirOrSea);
    }

    private static boolean isFourEdition(GameData data)
    {
        return data.getProperties().get(Constants.FOURTH_EDITION, false);
    }

    private static int getNeutralCharge(GameData data, Route route)
    {
        return getNeutralCharge(data, MoveDelegate.getEmptyNeutral(route).size());
    }

    private static int getNeutralCharge(GameData data, int numberOfTerritories)
    {
        return numberOfTerritories * games.strategy.triplea.Properties.getNeutralCharge(data);
    }

    public static MoveValidationResult validateMove(Collection<Unit> units, 
                                                    Route route, 
                                                    PlayerID player, 
                                                    Collection<Unit> transportsToLoad, 
                                                    boolean isNonCombat, 
                                                    final List<UndoableMove> undoableMoves, 
                                                    GameData data)
    {

        MoveValidationResult result = new MoveValidationResult();

        if (isNonCombat)
        {
            if (validateNonCombat(data, units, route, player, result).getError() != null)
                return result;
        }
        else
        {
            if (validateCombat(data, units, route, player, result).getError() != null)
                return result;
        }

        if (validateNonEnemyUnitsOnPath(data, units, route, player, result).getError() != null)
            return result;

        if (validateBasic(isNonCombat, data, units, route, player, transportsToLoad, result).getError() != null)
            return result;

        if (validateAirCanLand(data, units, route, player, result).getError() != null)
            return result;

        if (validateTransport(data, undoableMoves, units, route, player, transportsToLoad, result).getError() != null)
            return result;

        if (validateCanal(data, units, route, player, result).getError() != null)
            return result;


        //dont let the user move out of a battle zone
        //the exception is air units and unloading units into a battle zone
        if (MoveDelegate.getBattleTracker(data).hasPendingBattle(route.getStart(), false)
                && Match.someMatch(units, Matches.UnitIsNotAir))
        {
            //if the units did not move into the territory, then they can move out
            //this will happen if there is a submerged sub in the area, and 
            //a different unit moved into the sea zone setting up a battle
            //but the original unit can still remain
            boolean unitsStartedInTerritory = true;
            for(Unit unit : units) 
            {
                if(MoveDelegate.getRouteUsedToMoveInto(undoableMoves, unit, route.getEnd()) != null)
                {
                    unitsStartedInTerritory = false;
                    break;
                }
            }
            
            if(!unitsStartedInTerritory)
            {
            
                boolean unload = MoveValidator.isUnload(route);
                PlayerID endOwner = route.getEnd().getOwner();
                boolean attack = !data.getAllianceTracker().isAllied(endOwner, player) 
                               || MoveDelegate.getBattleTracker(data).wasConquered(route.getEnd());
                //unless they are unloading into another battle
                if (!(unload && attack))
                    return result.setErrorReturnResult("Cannot move units out of battle zone");
            }
        }

        //make sure we can afford to pay neutral fees
        int cost = getNeutralCharge(data, route);
        int resources = player.getResources().getQuantity(Constants.IPCS);
        if (resources - cost < 0)
            return result.setErrorReturnResult(TOO_POOR_TO_VIOLATE_NEUTRALITY);

        return result;
    }

    private static MoveValidationResult validateCanal(GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
    {
        if (getEditMode(data))
            return result;

        //if no sea units then we can move
        if (Match.noneMatch(units, Matches.UnitIsSea))
            return result;

        //TODO: merge validateCanal here and provide granular unit warnings
        return result.setErrorReturnResult(validateCanal(route, player, data));
    }

    private static MoveValidationResult validateCombat(GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
    {
        if (getEditMode(data))
            return result;

        // Don't allow aa guns to move in combat unless they are in a
        // transport
        if (Match.someMatch(units, Matches.UnitIsAA) && (!route.getStart().isWater() || !route.getEnd().isWater()))
            for (Unit unit : Match.getMatches(units, Matches.UnitIsAA))
                result.addDisallowedUnit("Cannot move AA guns in combat movement phase", unit);

        return result;
    }

    private static MoveValidationResult validateNonCombat(GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
    {
        if (getEditMode(data))
            return result;

        if (route.someMatch(Matches.TerritoryIsImpassible))
            return result.setErrorReturnResult(CANT_MOVE_THROUGH_IMPASSIBLE);

        CompositeMatch<Territory> battle = new CompositeMatchOr<Territory>();
        battle.add(Matches.TerritoryIsNeutral);
        battle.add(Matches.isTerritoryEnemyAndNotNeutral(player, data));

        if (battle.match(route.getEnd()))
            return result.setErrorReturnResult("Cannot advance units to battle in non combat");

        if (route.getEnd().getUnits().someMatch(Matches.enemyUnit(player, data)))
        {
            CompositeMatch<Unit> friendlyOrSubmerged = new CompositeMatchOr<Unit>();
            friendlyOrSubmerged.add(Matches.alliedUnit(player, data));
            friendlyOrSubmerged.add(Matches.unitIsSubmerged(data));
            if (!route.getEnd().getUnits().allMatch(friendlyOrSubmerged))
                return result.setErrorReturnResult("Cannot advance to battle in non combat");
        }

        if (Match.allMatch(units, Matches.UnitIsAir))
        {
            if (route.someMatch(Matches.TerritoryIsNeutral))
                return result.setErrorReturnResult("Air units cannot fly over neutral territories in non combat");
        } else
        {
            CompositeMatch<Territory> neutralOrEnemy = new CompositeMatchOr<Territory>(Matches.TerritoryIsNeutral, Matches.isTerritoryEnemyAndNotNeutral(player, data));
            if (route.someMatch(neutralOrEnemy))
                return result.setErrorReturnResult("Cannot move units to neutral or enemy territories in non combat");
        }
        return result;
    }

    private static MoveValidationResult validateNonEnemyUnitsOnPath(GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
    {
        if (getEditMode(data))
            return result;

        //check to see no enemy units on path
        if (MoveValidator.onlyAlliedUnitsOnPath(route, player, data))
            return result;

        //if we are all air, then its ok
        if (Match.allMatch(units, Matches.UnitIsAir))
            return result;

        boolean submersibleSubsAllowed = data.getProperties().get(Constants.SUBMERSIBLE_SUBS, false);

        if (submersibleSubsAllowed && Match.allMatch(units, Matches.UnitIsSub))
        {
            //this is ok unless there are destroyer on the path
            if (MoveValidator.enemyDestroyerOnPath(route, player, data))
                return result.setErrorReturnResult("Cannot move submarines under destroyers");
            else
                return result;
        }

        return result.setErrorReturnResult("Enemy units on path");
    }

    private static MoveValidationResult validateBasic(boolean isNonCombat, GameData data, Collection<Unit> units, Route route, PlayerID player, Collection<Unit> transportsToLoad, MoveValidationResult result)
    {
        boolean isEditMode = getEditMode(data);

        if(units.size() == 0)
            return result.setErrorReturnResult("No units");
        
        for (Unit unit : units)
        {
            if(TripleAUnit.get(unit).getSubmerged())
                result.addDisallowedUnit("Cannot move submerged units", unit);
        }

        //make sure all units are actually in the start territory
        if (!route.getStart().getUnits().containsAll(units))
            return result.setErrorReturnResult("Not enough units in starting territory");

        //make sure transports in the destination
        if (!route.getEnd().getUnits().containsAll(transportsToLoad))
            return result.setErrorReturnResult("Transports not found in route end");

        if (!isEditMode)
        {
            //make sure all units are at least friendly
            for (Unit unit : Match.getMatches(units, Matches.enemyUnit(player, data)))
                result.addDisallowedUnit("Can only move friendly units", unit);

            //check we have enough movement
            //exclude transported units
            Collection<Unit> moveTest;
            if (route.getStart().isWater())
            {
                moveTest = MoveValidator.getNonLand(units);
            } else
            {
                moveTest = units;
            }
            // check units individually
            for (Unit unit : moveTest)
            {
            if (!MoveValidator.hasEnoughMovement(unit, route.getLength()))
                    result.addDisallowedUnit("Not all units have enough movement",unit);
            }

            //if there is a neutral in the middle must stop unless all are air
            if (MoveValidator.hasNeutralBeforeEnd(route))
            {
                if (!Match.allMatch(units, Matches.UnitIsAir))
                    return result.setErrorReturnResult("Must stop land units when passing through neutral territories");
            }

            if (!isNonCombat && Match.someMatch(units, Matches.UnitIsLand) && route.getLength() >= 1)
            {
                //check all the territories but the end,
                //if there are enemy territories, make sure they are blitzable
                //if they are not blitzable, or we arent all blit units
                //fail
                int enemyCount = 0;
                boolean allEnemyBlitzable = true;

                for (int i = 0; i < route.getLength() - 1; i++)
                {
                    Territory current = route.at(i);

                    if (current.isWater())
                        continue;

                    if (!data.getAllianceTracker().isAllied(current.getOwner(), player)
                            || MoveDelegate.getBattleTracker(data).wasConquered(current))
                    {
                        enemyCount++;
                        allEnemyBlitzable &= MoveValidator.isBlitzable(current, data, player);
                    }
                }

                if (enemyCount > 0 && !allEnemyBlitzable)
                {
                    return result.setErrorReturnResult("Cannot blitz on that route");
                } else if (enemyCount > 0 && allEnemyBlitzable)
                {
                    Match<Unit> blitzingUnit = new CompositeMatchOr<Unit>(Matches.UnitCanBlitz, Matches.UnitIsAir);
                    Match<Unit> nonBlitzing = new InverseMatch<Unit>(blitzingUnit);
                    for (Unit unit : Match.getMatches(units, nonBlitzing))
                        result.addDisallowedUnit("Not all units can blitz",unit);
                }

            }

            //make sure no conquered territories on route
            if (MoveValidator.hasConqueredNonBlitzedOnRoute(route, data))
            {
                //unless we are all air or we are in non combat
                if (!Match.allMatch(units, Matches.UnitIsAir) && !isNonCombat)
                    return result.setErrorReturnResult("Cannot move through newly captured territories");
            }

        } // !isEditMode

        //make sure that no non sea non transportable no carriable units
        //end at sea
        if (route.getEnd().isWater())
        {
            for (Unit unit : MoveValidator.getUnitsThatCantGoOnWater(units))
                result.addDisallowedUnit("Not all units can end at water",unit);
        }

        //if we are water make sure no land
        if (Match.someMatch(units, Matches.UnitIsSea))
        {
            if (MoveValidator.hasLand(route))
                for (Unit unit : Match.getMatches(units, Matches.UnitIsSea))
                    result.addDisallowedUnit("Sea units cannot go on land",unit);
        }

        //make sure that we dont send aa guns to attack
        if (Match.someMatch(units, Matches.UnitIsAA))
        {
            //TODO
            //dont move if some were conquered

            for (int i = 0; i < route.getLength(); i++)
            {
                Territory current = route.at(i);
                if (!(current.isWater() || current.getOwner().equals(player) || data.getAllianceTracker().isAllied(player, current.getOwner())))
                {
                    for (Unit unit : Match.getMatches(units, Matches.UnitIsAA))
                        result.addDisallowedUnit("AA units cannot advance to battle", unit);

                    break;
                }
            }
        }

        //only allow aa into a land territory if one already present.
        if (!isFourEdition(data) && Match.someMatch(units, Matches.UnitIsAA) && route.getEnd().getUnits().someMatch(Matches.UnitIsAA)
                && !route.getEnd().isWater())
        {
            for (Unit unit : Match.getMatches(units, Matches.UnitIsAA))
                result.addDisallowedUnit("Only one AA gun allowed in a territory",unit);
        }

        //only allow 1 aa to unload
        if (route.getStart().isWater() && !route.getEnd().isWater() && Match.countMatches(units, Matches.UnitIsAA) > 1)
        {
            Collection<Unit> aaGuns = Match.getMatches(units, Matches.UnitIsAA);
            Iterator<Unit> aaIter = aaGuns.iterator();
            aaIter.next(); // skip first unit
            for (; aaIter.hasNext(); )
                result.addUnresolvedUnit("Only one AA gun can unload in a territory",aaIter.next());
        }

        // don't allow move through impassible territories
        if (!isEditMode && route.someMatch(Matches.TerritoryIsImpassible))
            return result.setErrorReturnResult(CANT_MOVE_THROUGH_IMPASSIBLE);

        if (canCrossNeutralTerritory(data, route, player, result).getError() != null)
            return result;
        
        return result;
    }

    private static MoveValidationResult validateAirCanLand(final GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
    {
        if (getEditMode(data))
            return result;

        boolean allowKamikaze =  data.getProperties().get(Constants.KAMIKAZE, false);

        //nothing to check
        if (!Match.someMatch(units, Matches.UnitIsAir))
            return result;

        //these is a place where we can land
        //must be friendly and non conqueuerd land
        CompositeMatch<Territory> friendlyGround = new CompositeMatchAnd<Territory>();
        friendlyGround.add(Matches.isTerritoryAllied(player, data));
        friendlyGround.add(new Match<Territory>() 
                {
                    public boolean match(Territory o)
                    {
                        return !MoveDelegate.getBattleTracker(data).wasConquered((Territory) o);
                    }
                }
        );
        friendlyGround.add(new Match<Territory>() 
                {
                    public boolean match(Territory o)
                    {
                        return !MoveDelegate.getBattleTracker(data).hasPendingBattle((Territory) o, false);
                    }
                }
        );
        friendlyGround.add(Matches.TerritoryIsLand);
        
        
        //we can land at the end, nothing left to check
        if(friendlyGround.match(route.getEnd()))
            return result;
        

        //this is the farthese we need to look for places to land
        //the fighters cant move farther than this
        //note that this doesnt take into account the movement used to move the
        //units along the route
        int maxMovement = MoveValidator.getMaxMovement(units);
        
        Match<Territory> canMoveThrough = new InverseMatch<Territory>(Matches.TerritoryIsImpassible);
        Match<Territory> notNeutral = new InverseMatch<Territory>(Matches.TerritoryIsNeutral);
        
        Match<Territory> notNeutralAndNotImpassible = new CompositeMatchAnd<Territory>(canMoveThrough, notNeutral);
        
        //find the closest land territory where everyone can land
        int closestLandTerritory = Integer.MAX_VALUE;
        
        Iterator iter = data.getMap().getNeighbors(route.getEnd(), maxMovement).iterator();
    
        while (iter.hasNext())
        {

            Territory territory = (Territory) iter.next();
        
            //can we land there?
            if(!friendlyGround.match(territory))
                continue;
            
            //do we have an easy path to get there
            //can we do it without violating neutrals
            Route noNeutralRoute = data.getMap().getRoute(route.getEnd(), territory, notNeutralAndNotImpassible); 
            if(noNeutralRoute != null)
            {
                closestLandTerritory = Math.min(closestLandTerritory, noNeutralRoute.getLength());
            }
            //can we find a path with neutrals?
            //can we afford this path?
            Route neutralViolatingRoute = data.getMap().getRoute(route.getEnd(), territory, notNeutral);
            if((neutralViolatingRoute != null) && getNeutralCharge(data, neutralViolatingRoute) <= player.getResources().getQuantity(Constants.IPCS))
            {
                closestLandTerritory = Math.min(closestLandTerritory, neutralViolatingRoute.getLength());                    
            }
        }
        
        //these are the units we have to be sure that can land somewhere
        Match<Unit> ownedAirMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitOwnedBy(player) );
        Collection<Unit> ownedAir = new ArrayList<Unit>();
        ownedAir.addAll( Match.getMatches(units, ownedAirMatch ));
        ownedAir.addAll(Match.getMatches( route.getEnd().getUnits().getUnits(), ownedAirMatch ));

        
        //find out how much movement we have left  
        IntegerMap<Unit> movementLeft = new IntegerMap<Unit>();
        Iterator<Unit> ownedAirIter = ownedAir.iterator();
        while (ownedAirIter.hasNext())
        {
            TripleAUnit unit = (TripleAUnit) ownedAirIter.next();
            int movement = unit.getMovementLeft();
            
            if(units.contains(unit))
                movement -= route.getLength();
            
            movementLeft.put(unit, movement);
        }
        
        //find the air units that cant make it to land
        Collection<Unit> airThatMustLandOnCarriers = new ArrayList<Unit>();
        Iterator<Unit> ownedAirIter2 = ownedAir.iterator();
        while (ownedAirIter2.hasNext())
        {
            Unit unit = (Unit) ownedAirIter2.next();
            if(movementLeft.getInt(unit) < closestLandTerritory)
                airThatMustLandOnCarriers.add(unit);
        }
        
        //we are done, everything can find a place to land
        if(airThatMustLandOnCarriers.isEmpty())
            return result;
        
        //not everything can land on a carrier
        Match<Unit> cantLandMatch = new InverseMatch<Unit>(Matches.UnitCanLandOnCarrier);
        for (Unit unit : Match.getMatches(airThatMustLandOnCarriers, cantLandMatch))
        {
            if (allowKamikaze)
                result.addUnresolvedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
            else
                result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
        }
        
        //now, find out where we can land on carriers
        IntegerMap<Integer> carrierCapacity = new IntegerMap<Integer>();
        
        Iterator<Territory> candidates = data.getMap().getNeighbors(route.getEnd(), maxMovement).iterator();
        while (candidates.hasNext())
        {
            Territory territory = (Territory) candidates.next();
            Route candidateRoute = data.getMap().getRoute(route.getEnd(), territory, canMoveThrough);
            if(candidateRoute == null)
                continue;
            Integer distance = new Integer(candidateRoute.getLength());
            
            //we dont want to count untis that moved with us
            Collection<Unit> unitsAtLocation = territory.getUnits().getMatches(Matches.alliedUnit(player, data));
            unitsAtLocation.removeAll(units);
            
            //how much spare capacity do they have?
            int extraCapacity = MoveValidator.carrierCapacity(unitsAtLocation) - MoveValidator.carrierCost(unitsAtLocation);
            extraCapacity = Math.max(0, extraCapacity);
            
            carrierCapacity.put(distance, carrierCapacity.getInt(distance) + extraCapacity);
            
        }
        
        Collection<Unit> unitsAtEnd = route.getEnd().getUnits().getMatches(Matches.alliedUnit(player, data));
        unitsAtEnd.addAll(units);
        carrierCapacity.put(new Integer(0), MoveValidator.carrierCapacity(unitsAtEnd));

        
        for (Unit unit : Match.getMatches(airThatMustLandOnCarriers, Matches.UnitCanLandOnCarrier))
        {
            int carrierCost = UnitAttachment.get(unit.getType()).getCarrierCost();
            int movement = movementLeft.getInt(unit);
            for(int i = movement; i >=-1; i--)
            {
                if(i == -1)
                {
                    if (allowKamikaze)
                        result.addUnresolvedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
                    else
                        result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
                    break;
                }

                Integer current = new Integer(i);
                if(carrierCapacity.getInt(current) >= carrierCost && carrierCost != -1 )
                {
                    carrierCapacity.put(current,carrierCapacity.getInt(current) - carrierCost);
                    break;
                }
            }
            
        }
        
        return result;
    }

    // Determines whether we can pay the neutral territory charge for a
    // given route for air units. We can't cross neutral territories
    // in 4th Edition.
    private static MoveValidationResult canCrossNeutralTerritory(GameData data, Route route, PlayerID player, MoveValidationResult result)
    {
        //neutrals we will overfly in the first place
        Collection<Territory> neutrals = MoveDelegate.getEmptyNeutral(route);
        int ipcs = player.getResources().getQuantity(Constants.IPCS);

        if (ipcs < getNeutralCharge(data, neutrals.size()))
            return result.setErrorReturnResult(TOO_POOR_TO_VIOLATE_NEUTRALITY);

        return result;
    }

    private static Territory getTerritoryTransportHasUnloadedTo(final List<UndoableMove> undoableMoves, Unit transport)
    {
        
        for(UndoableMove undoableMove : undoableMoves)
        {
            if(undoableMove.wasTransportUnloaded(transport))
            {
                return undoableMove.getRoute().getEnd();
            }
        }
        return null;
    }

    private static MoveValidationResult validateTransport(GameData data, 
                                                          final List<UndoableMove> undoableMoves, 
                                                          Collection<Unit> units, 
                                                          Route route, 
                                                          PlayerID player, 
                                                          Collection<Unit> transportsToLoad, 
                                                          MoveValidationResult result)
    {
        boolean isEditMode = getEditMode(data);

        if (Match.allMatch(units, Matches.UnitIsAir))
            return result;

        if (!MoveValidator.hasWater(route))
            return result;

        TransportTracker transportTracker = new TransportTracker();

        //if unloading make sure length of route is only 1
        if (!isEditMode && MoveValidator.isUnload(route))
        {
            if (route.getLength() > 1)
                return result.setErrorReturnResult("Unloading units must stop where they are unloaded");

            for (Unit unit : transportTracker.getUnitsLoadedOnAlliedTransportsThisTurn(units))
                result.addDisallowedUnit(CANNOT_LOAD_AND_UNLOAD_AN_ALLIED_TRANSPORT_IN_THE_SAME_ROUND,unit);

            Collection<Unit> transports = MoveDelegate.mapTransports(route, units, null).values();
            for(Unit transport : transports)
            {
                // check whether transport has already unloaded
                if (transportTracker.hasTransportUnloadedInPreviousPhase(transport))
                {
                    for (Unit unit : transportTracker.transporting(transport))
                        result.addDisallowedUnit(TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE, unit);
                }
                // check whether transport is restricted to another territory
                else if (transportTracker.isTransportUnloadRestrictedToAnotherTerritory(transport, route.getEnd()))
                {
                    Territory alreadyUnloadedTo = getTerritoryTransportHasUnloadedTo(undoableMoves, transport);
                    for (Unit unit : transportTracker.transporting(transport))
                        result.addDisallowedUnit(TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO + alreadyUnloadedTo.getName(), unit);
                }
            }
        }

        //if we are land make sure no water in route except for transport
        // situations
        Collection<Unit> land = Match.getMatches(units, Matches.UnitIsLand);

        //make sure we can be transported
        Match<Unit> cantBeTransported = new InverseMatch<Unit>(Matches.UnitCanBeTransported);
        for (Unit unit : Match.getMatches(land, cantBeTransported))
            result.addDisallowedUnit("Not all units can be transported",unit);

        //make sure that the only the first or last territory is land
        //dont want situation where they go sea land sea
        if (!isEditMode && MoveValidator.hasLand(route) && !(route.getStart().isWater() || route.getEnd().isWater()))
            return result.setErrorReturnResult("Invalid move, only start or end can be land when route has water.");

        //simply because I dont want to handle it yet
        //checks are done at the start and end, dont want to worry about just
        //using a transport as a bridge yet
        //TODO handle this
        if (!isEditMode && !route.getEnd().isWater() && !route.getStart().isWater())
            return result.setErrorReturnResult("Must stop units at a transport on route");

        if (route.getEnd().isWater() && route.getStart().isWater())
        {
            //make sure units and transports stick together
            Iterator<Unit> iter = units.iterator();
            while (iter.hasNext())
            {
                Unit unit = (Unit) iter.next();
                UnitAttachment ua = UnitAttachment.get(unit.getType());
                //make sure transports dont leave their units behind
                if (ua.getTransportCapacity() != -1)
                {
                    Collection<Unit> holding = transportTracker.transporting(unit);
                    if (holding != null && !units.containsAll(holding))
                        result.addDisallowedUnit("Transports cannot leave their units",unit);
                }
                //make sure units dont leave their transports behind
                if (ua.getTransportCost() != -1)
                {
                    Unit transport = transportTracker.transportedBy(unit);
                    if (transport != null && !units.contains(transport))
                        result.addDisallowedUnit("Unit must stay with its transport while moving",unit);
                }
            }
        } //end if end is water

        if (MoveValidator.isLoad(route))
        {

            if (!isEditMode && route.getLength() != 1)
                return result.setErrorReturnResult("Units cannot move before loading onto transports");

            CompositeMatch<Unit> enemyNonSubmerged = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), new InverseMatch<Unit>(Matches
                    .unitIsSubmerged(data)));
            if (route.getEnd().getUnits().someMatch(enemyNonSubmerged))
                return result.setErrorReturnResult("Cannot load when enemy sea units are present");

            Map<Unit,Unit> unitsToTransports = MoveDelegate.mapTransports(route, land, transportsToLoad);

            Iterator<Unit> iter = units.iterator();
            while (!isEditMode && iter.hasNext())
            {
                TripleAUnit unit = (TripleAUnit) iter.next();
                if (unit.getAlreadyMoved() != 0)
                    result.addDisallowedUnit("Units cannot move before loading onto transports",unit);
                Unit transport = unitsToTransports.get(unit);
                if (transport == null)
                    continue;
                if (transportTracker.hasTransportUnloadedInPreviousPhase(transport))
                {
                    result.addDisallowedUnit(TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE, unit);
                }
                else if (transportTracker.isTransportUnloadRestrictedToAnotherTerritory(transport, route.getEnd()))
                {
                    Territory alreadyUnloadedTo = getTerritoryTransportHasUnloadedTo(undoableMoves, transport);
                    result.addDisallowedUnit(TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO + alreadyUnloadedTo.getName(), unit);
                }
            }

            if (! unitsToTransports.keySet().containsAll(land))
            {
                // some units didn't get mapped to a transport
                Collection<UnitCategory> unitsToLoadCategories = UnitSeperator.categorize(land);

                if (unitsToTransports.size() == 0 || unitsToLoadCategories.size() == 1)
                {
                    // set all unmapped units as disallowed if there are no transports
                    //   or only one unit category
                    for (Unit unit : land)
                    {
                        if (unitsToTransports.containsKey(unit))
                            continue;
                        UnitAttachment ua = UnitAttachment.get(unit.getType());
                        if (ua.getTransportCost() != -1)
                        {
                            result.addDisallowedUnit("Not enough transports", unit);
                            //System.out.println("adding disallowed unit (Not enough transports): "+unit);
                        }
                    }
                }
                else
                {
                    // set all units as unresolved if there is at least one transport 
                    //   and mixed unit categories
                    for (Unit unit : land)
                    {
                        UnitAttachment ua = UnitAttachment.get(unit.getType());
                        if (ua.getTransportCost() != -1)
                        {
                            result.addUnresolvedUnit("Not enough transports", unit);
                            //System.out.println("adding unresolved unit (Not enough transports): "+unit);
                        }
                    }
                }
            }

        }

        return result;
    }


    public static String validateCanal(Route route, PlayerID player, GameData data)
    {
        Collection<Territory> territories = route.getTerritories();
        
        for(Territory routeTerritory : territories)
        {   
            Set<CanalAttachment> canalAttachments = CanalAttachment.get(routeTerritory);
            if(canalAttachments.isEmpty())
                continue;
            
            Iterator<CanalAttachment> iter = canalAttachments.iterator();
            while(iter.hasNext() )
            {
                CanalAttachment attachment = iter.next();
                if(attachment == null)
                    continue;
                if(!territories.containsAll( CanalAttachment.getAllCanalSeaZones(attachment.getCanalName(), data) ))
                {
                    continue;
                }
            
            
                for(Territory borderTerritory : attachment.getLandTerritories())
                {
                    if (!data.getAllianceTracker().isAllied(player, borderTerritory.getOwner()))
                    {
                        return "Must own " + borderTerritory.getName() + " to go through " + attachment.getCanalName();
                    }
                    if(MoveDelegate.getBattleTracker(data).wasConquered(borderTerritory))
                    {
                        return "Cannot move through " + attachment.getCanalName() + " without owning " + borderTerritory.getName() + " for an entire turn";
                    }            
                }
            }
            return null;
        }
        return null;
    }

    /** Creates new MoveValidator */
    private MoveValidator()
    {
    }
}
