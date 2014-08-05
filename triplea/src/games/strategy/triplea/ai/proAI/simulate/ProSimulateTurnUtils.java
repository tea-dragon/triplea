package games.strategy.triplea.ai.proAI.simulate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.ProAttackTerritoryData;
import games.strategy.triplea.ai.proAI.ProBattleResultData;
import games.strategy.triplea.ai.proAI.util.LogUtils;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMoveUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

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

/**
 * Pro AI attack options utilities.
 * 
 * <ol>
 * <li>Add support for considering carrier landing when calculating air routes</li>
 * </ol>
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProSimulateTurnUtils
{
	private final ProAI ai;
	private final ProUtils utils;
	private final ProBattleUtils battleUtils;
	private final ProMoveUtils moveUtils;
	
	public ProSimulateTurnUtils(final ProAI ai, final ProUtils utils, final ProBattleUtils battleUtils, final ProMoveUtils moveUtils)
	{
		this.ai = ai;
		this.utils = utils;
		this.battleUtils = battleUtils;
		this.moveUtils = moveUtils;
	}
	
	public void simulateBattles(final GameData data, final PlayerID player, final IDelegateBridge delegateBridge)
	{
		LogUtils.log(Level.FINE, "Starting battle simulation phase");
		
		final BattleDelegate battleDelegate = DelegateFinder.battleDelegate(data);
		final Map<BattleType, Collection<Territory>> battleTerritories = battleDelegate.getBattles().getBattles();
		for (final Entry<BattleType, Collection<Territory>> entry : battleTerritories.entrySet())
		{
			for (final Territory t : entry.getValue())
			{
				final IBattle battle = battleDelegate.getBattleTracker().getPendingBattle(t, entry.getKey().isBombingRun(), entry.getKey());
				final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
				final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();
				LogUtils.log(Level.FINER, "---" + t);
				LogUtils.log(Level.FINER, "attackers=" + attackers);
				LogUtils.log(Level.FINER, "defenders=" + defenders);
				final ProBattleResultData result = battleUtils.calculateBattleResults(player, t, attackers, defenders, true);
				final List<Unit> remainingUnits = result.getAverageUnitsRemaining();
				LogUtils.log(Level.FINER, "remainingUnits=" + remainingUnits);
				
				// Make updates to data
				final List<Unit> attackersToRemove = new ArrayList<Unit>(attackers);
				attackersToRemove.removeAll(remainingUnits);
				LogUtils.log(Level.FINER, "attackersToRemove=" + attackersToRemove);
				LogUtils.log(Level.FINER, "defendersToRemove=" + defenders);
				final Change attackerskilledChange = ChangeFactory.removeUnits(t, attackersToRemove);
				delegateBridge.addChange(attackerskilledChange);
				final Change defenderskilledChange = ChangeFactory.removeUnits(t, defenders);
				delegateBridge.addChange(defenderskilledChange);
				delegateBridge.addChange(ChangeFactory.changeOwner(t, player));
				battleDelegate.getBattleTracker().getConquered().add(t);
				final Territory updatedTerritory = data.getMap().getTerritory(t.getName());
				LogUtils.log(Level.FINER, "after changes owner=" + updatedTerritory.getOwner() + ", units=" + updatedTerritory.getUnits().getUnits());
			}
		}
	}
	
	public Map<Territory, ProAttackTerritoryData> transferMoveMap(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Territory> unitTerritoryMap, final GameData fromData,
				final GameData toData, final PlayerID player)
	{
		// TODO: Fix this
		System.out.println("Transferring move map");
		
		final Map<Territory, ProAttackTerritoryData> result = new HashMap<Territory, ProAttackTerritoryData>();
		final List<Unit> usedUnits = new ArrayList<Unit>();
		for (final Territory fromTerritory : moveMap.keySet())
		{
			final Territory toTerritory = toData.getMap().getTerritory(fromTerritory.getName());
			final ProAttackTerritoryData patd = new ProAttackTerritoryData(toTerritory);
			result.put(toTerritory, patd);
			final Map<Unit, List<Unit>> amphibAttackMap = moveMap.get(fromTerritory).getAmphibAttackMap();
			final Map<Unit, Boolean> isTransportingMap = moveMap.get(fromTerritory).getIsTransportingMap();
			System.out.println("Transferring " + fromTerritory + " to " + toTerritory);
			final List<Unit> amphibUnits = new ArrayList<Unit>();
			for (final Unit transport : amphibAttackMap.keySet())
			{
				Unit toTransport = null;
				final List<Unit> toUnits = new ArrayList<Unit>();
				if (isTransportingMap.get(transport))
				{
					System.out.println("-----Transferring loaded transport " + transport + " with " + amphibAttackMap.get(transport));
					toTransport = transferLoadedTransport(transport, amphibAttackMap.get(transport), unitTerritoryMap, usedUnits, toData, player);
					toUnits.addAll(TransportTracker.transporting(toTransport));
					System.out.println("-----Transferred loaded transport " + toTransport + " with " + TransportTracker.transporting(toTransport));
				}
				else
				{
					System.out.println("-----Transferring unloaded transport " + transport + " with " + amphibAttackMap.get(transport));
					toTransport = transferUnit(transport, unitTerritoryMap, usedUnits, toData, player);
					for (final Unit u : amphibAttackMap.get(transport))
					{
						final Unit toUnit = transferUnit(u, unitTerritoryMap, usedUnits, toData, player);
						toUnits.add(toUnit);
					}
					System.out.println("-----Transferred unloaded transport " + toTransport + " with " + toUnits);
				}
				patd.addUnits(toUnits);
				patd.putAmphibAttackMap(toTransport, toUnits);
				amphibUnits.addAll(amphibAttackMap.get(transport));
				System.out.println("---Transferring transport=" + transport + " with units=" + amphibAttackMap.get(transport) + " to transport=" + toTransport + " with units=" + toUnits);
			}
			for (final Unit u : moveMap.get(fromTerritory).getUnits())
			{
				if (!amphibUnits.contains(u))
				{
					final Unit toUnit = transferUnit(u, unitTerritoryMap, usedUnits, toData, player);
					patd.addUnit(toUnit);
					System.out.println("---Transferring unit " + u + " to " + toUnit);
				}
			}
		}
		
		return result;
	}
	
	private Unit transferUnit(final Unit u, final Map<Unit, Territory> unitTerritoryMap, final List<Unit> usedUnits, final GameData toData, final PlayerID player)
	{
		final Territory unitTerritory = unitTerritoryMap.get(u);
		final Match<Unit> ownedByPlayerAndUnitTypeMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.unitIsOfType(u.getType()), Matches.unitIsTransporting().invert());
		final List<Unit> toUnits = toData.getMap().getTerritory(unitTerritory.getName()).getUnits().getMatches(ownedByPlayerAndUnitTypeMatch);
		System.out.println("-----Transfer unit " + u + " to " + toUnits);
		for (final Unit toUnit : toUnits)
		{
			if (!usedUnits.contains(toUnit))
			{
				usedUnits.add(toUnit);
				return toUnit;
			}
		}
		return null;
	}
	
	private Unit transferLoadedTransport(final Unit transport, final List<Unit> transportingUnits, final Map<Unit, Territory> unitTerritoryMap, final List<Unit> usedUnits, final GameData toData,
				final PlayerID player)
	{
		final Territory unitTerritory = unitTerritoryMap.get(transport);
		final Match<Unit> ownedByPlayerAndTransportingMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.unitIsOfType(transport.getType()), Matches.unitIsTransporting());
		final List<Unit> toTransports = toData.getMap().getTerritory(unitTerritory.getName()).getUnits().getMatches(ownedByPlayerAndTransportingMatch);
		for (final Unit toTransport : toTransports)
		{
			if (!usedUnits.contains(toTransport))
			{
				final List<Unit> toTransportingUnits = (List<Unit>) TransportTracker.transporting(toTransport);
				if (transportingUnits.size() == toTransportingUnits.size())
				{
					boolean canTransfer = true;
					for (int i = 0; i < transportingUnits.size(); i++)
					{
						if (!transportingUnits.get(i).getType().equals(toTransportingUnits.get(i).getType()))
						{
							canTransfer = false;
							break;
						}
					}
					if (canTransfer)
					{
						usedUnits.add(toTransport);
						usedUnits.addAll(toTransportingUnits);
						return toTransport;
					}
				}
			}
		}
		return null;
	}
	
}
