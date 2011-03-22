/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.triplea.Dynamix_AI.Group;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedInstanceCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.TacticalCenter;
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author Stephen
 */
public class UnitGroup
{
    private Collection<Unit> m_units = new ArrayList<Unit>();
    private Territory m_fromTer = null;
    private Territory m_movedTo = null;
    private Match<Territory> m_cmRouteMatch = null;
    private HashMap<Match<Territory>, Integer> m_ncmCRouteMatches = null;
    private GameData m_data = null;
    private List<Territory> m_neighbors = new ArrayList<Territory>();
    private int m_moveIndex = 0;

    public UnitGroup(Unit unit, Territory startTer, GameData data)
    {
        this(Collections.singleton(unit), startTer, data);
    }

    public UnitGroup(Collection<Unit> units, Territory startTer, GameData data)
    {        
        TacticalCenter.get(data, GlobalCenter.CurrentPlayer).AllDelegateUnitGroups.add(this);
        for(Unit unit : units)
            TacticalCenter.get(data, GlobalCenter.CurrentPlayer).SetUnitStartLocation_IfNotAlreadySet(unit, startTer);
        m_units = units;
        m_fromTer = startTer;
        m_data = data;
        GenerateRouteMatches();
        GenerateNeighbors();
    }

    @Override
    public int hashCode()
    {
        String hashString = m_units.hashCode() + "" + m_fromTer.getName();
        return hashString.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final UnitGroup other = (UnitGroup) obj;
        if (this.m_units != other.m_units && (this.m_units == null || !this.m_units.equals(other.m_units)))
        {
            return false;
        }
        if (this.m_fromTer != other.m_fromTer && (this.m_fromTer == null || !this.m_fromTer.equals(other.m_fromTer)))
        {
            return false;
        }
        return true;
    }

    private void GenerateNeighbors()
    {
        m_neighbors = new ArrayList<Territory>(m_data.getMap().getNeighbors(m_fromTer));
    }

    private void GenerateRouteMatches()
    {
        PlayerID player = null;
        boolean land = false;
        boolean air = false;
        boolean sea = false;
        for (Unit unit : m_units)
        {
            player = unit.getOwner();
            UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
            if(ua.isAir())
                air = true;
            else if(ua.isSea())
                sea = true;
            else
                land = true;
        }

        if(player == null || player.isNull())
            player = GlobalCenter.CurrentPlayer;

        if(air)
        {
            m_cmRouteMatch = Matches.TerritoryIsNotImpassable;
        }
        else if(sea)
        {
            m_cmRouteMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, m_data));
        }
        else
        {
             m_cmRouteMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.territoryHasNoEnemyUnits(player, m_data));
        }

        if(air)
        {
            m_ncmCRouteMatches = new HashMap<Match<Territory>, Integer>();
            m_ncmCRouteMatches.put(DUtils.CompMatchAnd(Matches.TerritoryIsNotImpassable, DUtils.CompMatchOr(DMatches.territoryIsOwnedByXOrAlly(m_data, player), Matches.territoryHasUnitsThatMatch(Matches.UnitIsAA).invert())), 1); //Is passible and without enemy AA's
            m_ncmCRouteMatches.put(Matches.TerritoryIsNotImpassable, 2); //Is any passable ter
        }
        else if(sea)
        {
            m_ncmCRouteMatches = new HashMap<Match<Territory>, Integer>();
            m_ncmCRouteMatches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasUnitsOwnedBy(player)), 10); //We love sea with our units, cause we can control the movement
            m_ncmCRouteMatches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasUnitsThatMatch(Matches.unitIsEnemyOf(m_data, player).invert())), 15); //Allied is ok, but they could move
            m_ncmCRouteMatches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, m_data)), 20); //Enemy free is fine
            m_ncmCRouteMatches.put(Matches.TerritoryIsWater, 25); //We don't like having to go through enemies
        }
        else
        {
            m_ncmCRouteMatches = new HashMap<Match<Territory>, Integer>();
            m_ncmCRouteMatches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, DMatches.territoryIsOwnedBy(player)), 10);
            m_ncmCRouteMatches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, DMatches.territoryIsOwnedByXOrAlly(m_data, player)), 11);
            m_ncmCRouteMatches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable, Matches.TerritoryIsNotNeutral), 15);
            m_ncmCRouteMatches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand), 1000); //We really don't want to go through impassibles, actually never, cause we can't... 8|
        }
    }

    public Collection<Unit> GetUnits()
    {
        return m_units;
    }

    public List<Unit> GetUnitsAsList()
    {
        return new ArrayList<Unit>(m_units);
    }

    public Territory GetFromTerritory()
    {
        return m_fromTer;
    }

    public Unit GetFirstUnit()
    {
        return ((Unit) m_units.toArray()[0]);
    }

    /**
     * Attempts to find a cm route from this unit group's start location to the target specified
     * @param target - The target to find a route to
     * @return null if move failed. If successful, returns the calculated ncm route.
     */
    public Route GetCMRoute(Territory target)
    {
        Route route = m_data.getMap().getRoute_IgnoreEnd(m_fromTer, target, m_cmRouteMatch);
        if (route == null || route.getTerritories().size() < 2 || route.getStart().getName().equals(route.getEnd().getName()))
            return null;
        int slowest = DUtils.GetSlowestMovementUnitInList(new ArrayList<Unit>(m_units));
        if(slowest < 1)
            return null;
        if(UnitAttachment.get(GetFirstUnit().getUnitType()).isAir())
            route = DUtils.TrimRoute_ToLength(route, slowest, GetFirstUnit().getOwner(), m_data);
        else if(UnitAttachment.get(GetFirstUnit().getUnitType()).isSea())
            route = DUtils.TrimRoute_AtFirstTerWithEnemyUnits(route, slowest, GetFirstUnit().getOwner(), m_data);
        else
            route = DUtils.TrimRoute_AtFirstTerWithEnemyUnits(route, slowest, GetFirstUnit().getOwner(), m_data);

        if (route == null || route.getTerritories().size() < 2 || route.getStart().getName().equals(route.getEnd().getName()))
            return null;

        if (StatusCenter.get(m_data, GlobalCenter.CurrentPlayer).GetStatusOfTerritory(route.getEnd()).WasAbandoned)
            route = DUtils.TrimRoute_BeforeFirstTerMatching(route, slowest, GlobalCenter.CurrentPlayer, m_data, DMatches.territoryMatchesDMatch(m_data, GlobalCenter.CurrentPlayer, DMatches.TS_WasAbandoned));

        if (route == null || route.getTerritories().size() < 2 || route.getStart().getName().equals(route.getEnd().getName()))
            return null;

        return route;
    }

    /**
     * Attempts to find an ncm route from this unit group's start location to the target specified
     * @param target - The target to find a route to
     * @return null if move failed. If successful, returns the calculated ncm route.
     */
    public Route GetNCMRoute(Territory target)
    {
        return GetNCMRoute(target, false);
    }

    /**
     * Attempts to find an ncm route from this unit group's start location to the target specified
     * @param target - The target to find a route to
     * @param extraChecks - If enabled, extra checks will be used during calculation, like route trimming so the units don't end up in an abandoned territory.
     * @return null if move failed. If successful, returns the calculated ncm route.
     */
    public Route GetNCMRoute(Territory target, boolean extraChecks)
    {
        Route route = m_data.getMap().getCompositeRoute(m_fromTer, target, m_ncmCRouteMatches);
        if (route == null || route.getTerritories() == null || route.getTerritories().size() < 2 || route.getStart().getName().equals(route.getEnd().getName()))
            return null;

        int slowest = DUtils.GetSlowestMovementUnitInList(new ArrayList<Unit>(m_units));
        if(slowest < 1)
            return null;
        if(UnitAttachment.get(GetFirstUnit().getUnitType()).isAir())
            route = DUtils.TrimRoute_ToLength(route, slowest, GetFirstUnit().getOwner(), m_data);
        else if(UnitAttachment.get(GetFirstUnit().getUnitType()).isSea())
            route = DUtils.TrimRoute_BeforeFirstTerWithEnemyUnits(route, slowest, GetFirstUnit().getOwner(), m_data);
        else
            route = DUtils.TrimRoute_AtLastFriendlyTer(route, slowest, GetFirstUnit().getOwner(), m_data);

        if (route == null || route.getTerritories().size() < 2 || route.getStart().getName().equals(route.getEnd().getName()))
            return null;

        if (extraChecks)
        {
            if(StatusCenter.get(m_data, GlobalCenter.CurrentPlayer).GetStatusOfTerritory(route.getEnd()).WasAbandoned)
                route = DUtils.TrimRoute_BeforeFirstTerMatching(route, slowest, GlobalCenter.CurrentPlayer, m_data, DMatches.territoryMatchesDMatch(m_data, GlobalCenter.CurrentPlayer, DMatches.TS_WasAbandoned));
        }

        if (route == null || route.getTerritories().size() < 2 || route.getStart().getName().equals(route.getEnd().getName()))
            return null;

        return route;
    }

    /**
     * Attempts to move the units given during initialization as far as possible to the target specified.
     * @param target - The territory for the units in this unit group to move to
     * @param mover - The move delegate that performs the move
     * @return an error message if move failed. If successful, returns null.
     */
    public String MoveAsFarTo_CM(Territory target, IMoveDelegate mover)
    {
        Route route = null;

        if (GetFromTerritory().equals(target))
            return null; //We signal that the move succeeded, since the units 'made it' to the target
        if (m_movedTo != null)
            return "Cannot move unit group that has already moved somewhere";

        route = GetCMRoute(target);
        if (route == null)
            return "Error calculating CM route...";

        if(s_isBufferring)
        {
            Route key = route;
            DUtils.AddObjToListValueForKeyInMap(s_bufferedMoves, key, this);
            return null;
        }
        else
        {
            String moveError = MoveUnitsInternal(mover, route, new ArrayList<Unit>(m_units));
            if (moveError != null)
                return moveError;
            else
                NotifySuccessfulMove(m_movedTo);
        }

        return null;
    }

    /**
     * Attempts to move the units given during initialization as far as possible to the target specified.
     * @param target - The territory for the units in this unit group to move to
     * @param mover - The move delegate that performs the move
     * @return an error message if move failed. If successful, returns null.
     */
    public String MoveAsFarTo_NCM(Territory target, IMoveDelegate mover)
    {
        return MoveAsFarTo_NCM(target, mover, false);
    }

    /**
     * Attempts to move the units given during initialization as far as possible to the target specified.
     * @param target - The territory for the units in this unit group to move to
     * @param mover - The move delegate that performs the move
     * @param extraChecks - If enabled, extra checks will be used in this move, like route trimming so the units don't end up in an abandoned territory.
     * @return an error message if move failed. If successful, returns null.
     */
    public String MoveAsFarTo_NCM(Territory target, IMoveDelegate mover, boolean extraChecks)
    {
        Route route = null;

        if (GetFromTerritory().equals(target))
            return null; //We signal that the move succeeded, since the units 'made it' to the target
        if (m_movedTo != null)
            return "Cannot move unit group that has already moved somewhere";

        route = GetNCMRoute(target, extraChecks);
        if (route == null)
            return "Error calculating NCM route...";

        if(s_isBufferring)
        {
            Route key = route;
            DUtils.AddObjToListValueForKeyInMap(s_bufferedMoves, key, this);
            return null;
        }
        else
        {
            String moveError = MoveUnitsInternal(mover, route, new ArrayList<Unit>(m_units));
            if (moveError != null)
                return moveError;
            else
                NotifySuccessfulMove(m_movedTo);
        }

        return null;
    }

    /**
     * Attempts to move the units given during initialization as far as possible along the route given.
     * @param fullRoute - The route that the units in this unit group will follow
     * @param mover - The move delegate that performs the move
     * @return an error message if move failed. If successful, returns null.
     */
    public String MoveAsFarAlongRoute_NCM(IMoveDelegate mover, Route fullRoute)
    {
        Route route = fullRoute;

        if (fullRoute != null && GetFromTerritory().equals(fullRoute.getEnd()))
            return null; //We signal that the move succeeded, since the units 'made it' to the target
        if (m_movedTo != null)
            return "Cannot move unit group that has already moved somewhere";

        int slowest = DUtils.GetSlowestMovementUnitInList(new ArrayList<Unit>(m_units));
        if (route == null || route.getTerritories().size() < 2)
            return "The route given is either null or too short(no actual route)";
        if (slowest < 1)
            return "Some of the units in this unit group don't have any movement left";
        if (UnitAttachment.get(GetFirstUnit().getUnitType()).isAir())
            route = DUtils.TrimRoute_ToLength(route, slowest, GetFirstUnit().getOwner(), m_data);
        else if (UnitAttachment.get(GetFirstUnit().getUnitType()).isSea())
            route = DUtils.TrimRoute_BeforeFirstTerWithEnemyUnits(route, slowest, GetFirstUnit().getOwner(), m_data);
        else
            route = DUtils.TrimRoute_AtLastFriendlyTer(route, slowest, GetFirstUnit().getOwner(), m_data);

        if (route == null || route.getTerritories().size() < 2)
            return "After trimming, the route given is either null or too short(no actual route)";

        if (StatusCenter.get(m_data, GlobalCenter.CurrentPlayer).GetStatusOfTerritory(route.getEnd()).WasAbandoned)
            route = DUtils.TrimRoute_BeforeFirstTerMatching(route, slowest, GlobalCenter.CurrentPlayer, m_data, DMatches.territoryMatchesDMatch(m_data, GlobalCenter.CurrentPlayer, DMatches.TS_WasAbandoned));

        if (route == null || route.getTerritories().size() < 2)
            return "After secondary trimming, the route given is either null or too short(no actual route)";

        if(s_isBufferring)
        {
            Route key = route;
            DUtils.AddObjToListValueForKeyInMap(s_bufferedMoves, key, this);
            return null;
        }
        else
        {
            String moveError = MoveUnitsInternal(mover, route, new ArrayList<Unit>(m_units));
            if (moveError != null)
                return moveError;
            else
                NotifySuccessfulMove(m_movedTo);
        }

        return null;
    }

    /**
     * Attempts to move the units given on the route given.
     * @param mover - The move delegate that performs the move
     * @param route - The route that the units in this unit group will follow
     * @param units - The units to move
     * @return an error message if move failed. If successful, returns null.
     */
    private static String MoveUnitsInternal(IMoveDelegate mover, Route route, Collection<Unit> units)
    {
        List<Unit> unitsToMove = new ArrayList<Unit>(units);

        List<Unit> frozenOnes = new ArrayList<Unit>(unitsToMove);
        frozenOnes.retainAll(TacticalCenter.get(CachedInstanceCenter.CachedGameData, GlobalCenter.CurrentPlayer).GetFrozenUnits());
        unitsToMove.removeAll(frozenOnes);

        if (unitsToMove.isEmpty())
            return "Move prepared, though there are no un-frozen units to move!";

        String moveError = mover.move(unitsToMove, route);
        if (moveError != null)
            return moveError;

        if (route.getEnd().getUnits().containsAll(unitsToMove))
            DUtils.Log(Level.FINER, "          Performed move on route: {0} Units: {1}", route, DUtils.UnitList_ToString(unitsToMove));
        else
            return DUtils.Format("Move failed(units are not at destination), though no errors occurred. Route: {0} Units: {1}", route, DUtils.UnitList_ToString(units));

        return null;
    }

    private void NotifySuccessfulMove(Territory movedTo)
    {
        m_moveIndex = movesCount;
        movesCount++;
        m_movedTo = movedTo;
    }

    public Territory GetMovedTo()
    {
        return m_movedTo;
    }

    public void UndoMove(IMoveDelegate mover)
    {
        if(m_movedTo == null)
            return;

        mover.undoMove(m_moveIndex);
        DUtils.Log(Level.FINER, "          Move undone. Initial Location: {0} Target: {1} Units: {2}", m_fromTer, m_movedTo, m_units);
        m_movedTo = null;
        movesCount--;
        for (UnitGroup ug2 : TacticalCenter.get(CachedInstanceCenter.CachedGameData, GlobalCenter.CurrentPlayer).AllDelegateUnitGroups)
            ug2.NotifyMoveUndo(GetMoveIndex());
    }

    public int GetMoveIndex()
    {
        return m_moveIndex;
    }

    public static int movesCount = 0;
    public void NotifyMoveUndo(int removedMoveIndex)
    {
        if(m_moveIndex > removedMoveIndex)
            m_moveIndex--;
    }

    public List<Territory> GetNeighbors()
    {
        return m_neighbors;
    }

    public Match<Territory> GetRouteMatch()
    {
        return m_cmRouteMatch;
    }

    /**
     * (UndoAllMovesEndingInTer_ReturnStartTersOfUnitGroupsUndone)
     * @param data - Game data
     * @param player - Current player
     * @param ter - All unit groups that have moved here will get undone by this method
     * @param mover - The move delegate this is used to undo the moves
     * @return the list of ters that the undone unit groups originated from
     */
    public static List<Territory> UndoAllMovesEndingInTer_RStartTersOfUGsUndone(GameData data, PlayerID player, Territory ter, IMoveDelegate mover)
    {
        DUtils.Log(Level.FINE, "    Undoing all moves ending at {0}", ter.getName());
        List<Territory> result = new ArrayList<Territory>();
        for (UnitGroup ug : TacticalCenter.get(data, player).AllDelegateUnitGroups)
        {
            if (ug.GetMovedTo() != null && ug.GetMovedTo().equals(ter))
            {
                ug.UndoMove(mover);
                result.add(ug.GetFromTerritory());
            }
        }
        return result;
    }

    private static boolean s_isBufferring = false;
    private static HashMap<Route, List<UnitGroup>> s_bufferedMoves = new HashMap<Route, List<UnitGroup>>();
    public static boolean IsBufferringMoves()
    {
        return s_isBufferring;
    }
    public static void EnableMoveBuffering()
    {
        s_isBufferring = true;
    }
    public static void ClearBufferedMoves()
    {
        s_bufferedMoves.clear();
    }
    /**
     * @param mover
     * @return a list of errors that occurred, if any. If there were not errors, null is returned.
     */
    public static String PerformBufferedMovesAndDisableMoveBufferring(IMoveDelegate mover)
    {
        String errors = performBufferedMoves(s_bufferedMoves, mover);
        s_bufferedMoves.clear();
        s_isBufferring = false;
        TacticalCenter.get(CachedInstanceCenter.CachedGameData, GlobalCenter.CurrentPlayer).PerformBufferedFreezes();
        return errors;
    }
    private static String performBufferedMoves(HashMap<Route, List<UnitGroup>> moves, IMoveDelegate mover)
    {
        StringBuilder errors = new StringBuilder();
        for (Route key : moves.keySet())
        {
            Route route = key;
            List<UnitGroup> ugs = moves.get(key);
            List<Unit> units = new ArrayList<Unit>();
            for(UnitGroup ug : ugs)
                units.addAll(ug.GetUnits());

            String moveError = MoveUnitsInternal(mover, route, units);
            if (moveError != null)
                errors.append(moveError).append("\r\n");
            else
            {
                for (UnitGroup ug : ugs)
                    ug.NotifySuccessfulMove(route.getEnd());
            }
        }
        if(errors.length() == 0)
            return null;
        else
            return errors.toString().substring(0, errors.length() - 2);
    }

    @Override
    public String toString()
    {
        return DUtils.UnitGroupList_ToString(Collections.singletonList(this));
    }
}
