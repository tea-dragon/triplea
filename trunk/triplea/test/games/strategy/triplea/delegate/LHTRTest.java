package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.ChangePerformer;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.xml.LoadGameUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class LHTRTest extends TestCase
{
	private GameData m_data;
	
	@Override
	protected void setUp() throws Exception
	{
		m_data = LoadGameUtil.loadGame("revised", "lhtr.xml");
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		m_data = null;
	}
	
	private ITestDelegateBridge getDelegateBridge(final PlayerID player)
	{
		return GameDataTestUtil.getDelegateBridge(player);
	}
	
	public void testFightersCanLandOnNewPlacedCarrier()
	{
		final MoveDelegate delegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
		delegate.initialize("MoveDelegate", "MoveDelegate");
		final PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
		final ITestDelegateBridge bridge = getDelegateBridge(germans);
		bridge.setStepName("GermansNonCombatMove");
		delegate.start(bridge);
		final Territory baltic = m_data.getMap().getTerritory("5 Sea Zone");
		final Territory easternEurope = m_data.getMap().getTerritory("Eastern Europe");
		final UnitType carrirType = m_data.getUnitTypeList().getUnitType("carrier");
		// move a fighter to the baltic
		final Route route = new Route();
		route.setStart(easternEurope);
		route.add(baltic);
		final UnitType fighterType = m_data.getUnitTypeList().getUnitType("fighter");
		delegate.move(easternEurope.getUnits().getMatches(Matches.unitIsOfType(fighterType)), route);
		// add a carrier to be produced in germany
		final TripleAUnit carrier = new TripleAUnit(carrirType, germans, m_data);
		new ChangePerformer(m_data).perform(ChangeFactory.addUnits(germans, Collections.singleton((Unit) carrier)));
		// end the move phase
		delegate.end();
		// make sure the fighter is still there
		// in lhtr fighters can hover, and carriers placed beneath them
		assertTrue(baltic.getUnits().someMatch(Matches.unitIsOfType(fighterType)));
	}
	
	public void testFightersDestroyedWhenNoPendingCarriers()
	{
		final MoveDelegate delegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
		delegate.initialize("MoveDelegate", "MoveDelegate");
		final PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
		final ITestDelegateBridge bridge = getDelegateBridge(germans);
		bridge.setStepName("GermansNonCombatMove");
		delegate.start(bridge);
		final Territory baltic = m_data.getMap().getTerritory("5 Sea Zone");
		final Territory easternEurope = m_data.getMap().getTerritory("Eastern Europe");
		// move a fighter to the baltic
		final Route route = new Route();
		route.setStart(easternEurope);
		route.add(baltic);
		final UnitType fighterType = m_data.getUnitTypeList().getUnitType("fighter");
		delegate.move(easternEurope.getUnits().getMatches(Matches.unitIsOfType(fighterType)), route);
		// end the move phase
		delegate.end();
		// there is no pending carrier to be placed
		// the fighter cannot hover
		assertFalse(baltic.getUnits().someMatch(Matches.unitIsOfType(fighterType)));
	}
	
	public void testAAGunsDontFireNonCombat()
	{
		final MoveDelegate delegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
		delegate.initialize("MoveDelegate", "MoveDelegate");
		final PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
		final ITestDelegateBridge bridge = getDelegateBridge(germans);
		bridge.setStepName("GermansNonCombatMove");
		delegate.start(bridge);
		// if we try to move aa, then the game will ask us if we want to move
		// fail if we are called
		final InvocationHandler handler = new InvocationHandler()
		{
			public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
			{
				fail("method called:" + method);
				// never reached
				return null;
			}
		};
		final ITripleaPlayer player = (ITripleaPlayer) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] { ITripleaPlayer.class }, handler);
		bridge.setRemote(player);
		// move 1 fighter over the aa gun in caucus
		final Route route = new Route();
		route.setStart(m_data.getMap().getTerritory("Ukraine S.S.R."));
		route.add(m_data.getMap().getTerritory("Caucasus"));
		route.add(m_data.getMap().getTerritory("West Russia"));
		final List<Unit> fighter = route.getStart().getUnits().getMatches(Matches.UnitIsAir);
		delegate.move(fighter, route);
	}
	
	public void testSubDefenseBonus()
	{
		final UnitType sub = m_data.getUnitTypeList().getUnitType("submarine");
		final UnitAttachment attachment = UnitAttachment.get(sub);
		final PlayerID japanese = m_data.getPlayerList().getPlayerID("Japanese");
		// before the advance, subs defend and attack at 2
		assertEquals(2, attachment.getDefense(japanese));
		assertEquals(2, attachment.getAttack(japanese));
		final ITestDelegateBridge bridge = getDelegateBridge(japanese);
		TechTracker.addAdvance(japanese, bridge, TechAdvance.SUPER_SUBS);
		// after tech advance, this is now 3
		assertEquals(3, attachment.getDefense(japanese));
		assertEquals(3, attachment.getAttack(japanese));
		// make sure this only changes for the player with the tech
		final PlayerID americans = m_data.getPlayerList().getPlayerID("Americans");
		assertEquals(2, attachment.getDefense(americans));
		assertEquals(2, attachment.getAttack(americans));
	}
	
	public void testLHTRBombingRaid()
	{
		final Territory germany = m_data.getMap().getTerritory("Germany");
		final Territory uk = m_data.getMap().getTerritory("United Kingdom");
		final PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
		final PlayerID british = m_data.getPlayerList().getPlayerID("British");
		final BattleTracker tracker = new BattleTracker();
		final StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, m_data, british, germans, tracker);
		battle.addAttackChange(m_data.getMap().getRoute(uk, germany), uk.getUnits().getMatches(Matches.UnitIsStrategicBomber));
		final ITestDelegateBridge bridge = getDelegateBridge(british);
		TechTracker.addAdvance(british, bridge, TechAdvance.HEAVY_BOMBER);
		// aa guns rolls 3, misses, bomber rolls 2 dice at 3 and 4
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 2, 2, 3 }));
		// if we try to move aa, then the game will ask us if we want to move
		// fail if we are called
		final InvocationHandler handler = new InvocationHandler()
		{
			public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
			{
				return null;
			}
		};
		final ITripleaPlayer player = (ITripleaPlayer) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] { ITripleaPlayer.class }, handler);
		bridge.setRemote(player);
		final int PUsBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
		battle.fight(bridge);
		final int PUsAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
		// targets dice is 4, so damage is 1 + 4 = 5
		// Changed to match StrategicBombingRaidBattle changes
		assertEquals(PUsBeforeRaid - 5, PUsAfterRaid);
	}
	
	public void testLHTRBombingRaid2Bombers()
	{
		final Territory germany = m_data.getMap().getTerritory("Germany");
		final Territory uk = m_data.getMap().getTerritory("United Kingdom");
		final PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
		final PlayerID british = m_data.getPlayerList().getPlayerID("British");
		// add a unit
		final Unit bomber = m_data.getUnitTypeList().getUnitType("bomber").create(british);
		final Change change = ChangeFactory.addUnits(uk, Collections.singleton(bomber));
		new ChangePerformer(m_data).perform(change);
		final BattleTracker tracker = new BattleTracker();
		final StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, m_data, british, germans, tracker);
		battle.addAttackChange(m_data.getMap().getRoute(uk, germany), uk.getUnits().getMatches(Matches.UnitIsStrategicBomber));
		final ITestDelegateBridge bridge = getDelegateBridge(british);
		TechTracker.addAdvance(british, bridge, TechAdvance.HEAVY_BOMBER);
		// aa guns rolls 3,3 both miss, bomber 1 rolls 2 dice at 3,4 and bomber 2 rolls dice at 1,2
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 3, 3, 2, 3, 0, 1 }));
		// if we try to move aa, then the game will ask us if we want to move
		// fail if we are called
		final InvocationHandler handler = new InvocationHandler()
		{
			public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
			{
				return null;
			}
		};
		final ITripleaPlayer player = (ITripleaPlayer) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] { ITripleaPlayer.class }, handler);
		bridge.setRemote(player);
		final int PUsBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
		battle.fight(bridge);
		final int PUsAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
		// targets dice is 4, so damage is 1 + 4 = 5
		// bomber 2 hits at 2, so damage is 3, for a total of 8
		// Changed to match StrategicBombingRaidBattle changes
		assertEquals(PUsBeforeRaid - 8, PUsAfterRaid);
	}
}


/**
 * a random source that throws when asked for random
 * usefule for testing
 */
class ThrowingRandomSource implements IRandomSource
{
	public int getRandom(final int max, final String annotation)
	{
		throw new IllegalStateException("not allowed");
	}
	
	public int[] getRandom(final int max, final int count, final String annotation)
	{
		throw new IllegalStateException("not allowed");
	}
}
