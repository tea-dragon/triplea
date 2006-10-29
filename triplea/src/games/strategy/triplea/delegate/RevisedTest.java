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

package games.strategy.triplea.delegate;

import java.io.*;
import java.lang.reflect.*;
import java.util.List;

import junit.framework.TestCase;

import games.strategy.engine.data.*;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatchAnd;

public class RevisedTest extends TestCase 
{

    private GameData m_data;

    @Override
    protected void setUp() throws Exception
    {
        File gameRoot  = GameRunner.getRootFolder();
        File gamesFolder = new File(gameRoot, "games");
        File lhtr = new File(gamesFolder, "revised.xml");
        
        if(!lhtr.exists())
            throw new IllegalStateException("revised does not exist");
        
        InputStream input = new BufferedInputStream(new FileInputStream(lhtr));
        
        try
        {
            m_data = (new GameParser()).parse(input);
        }
        finally
        {
            input.close();    
        }
    }

    @Override
    protected void tearDown() throws Exception
    {
        m_data = null;
    }
    
    public void testSubAdvance()
    {
        UnitType sub = m_data.getUnitTypeList().getUnitType("submarine");
        UnitAttachment attachment = UnitAttachment.get(sub);
        
        PlayerID japanese = m_data.getPlayerList().getPlayerID("Japanese");
        
        //before the advance, subs defend and attack at 2
        assertEquals(2, attachment.getDefense(japanese));
        assertEquals(2, attachment.getAttack(japanese));
        
        TestDelegateBridge bridge = new TestDelegateBridge(m_data, japanese);
        
        TechTracker.addAdvance(japanese, m_data, bridge, TechAdvance.SUPER_SUBS);
        
        
        //after tech advance, this is now 3
        assertEquals(2, attachment.getDefense(japanese));
        assertEquals(3, attachment.getAttack(japanese));
    }
    
    public void testRetreatBug()
    {
        
        PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
        PlayerID americans = m_data.getPlayerList().getPlayerID("Americans");
        
        TestDelegateBridge bridge = new TestDelegateBridge(m_data, russians);

        //we need to initialize the original owner
        InitializationDelegate initDel = (InitializationDelegate) m_data.getDelegateList().getDelegate("initDelegate");
        initDel.start(bridge, m_data);
        initDel.end();

        
        //make sinkian japanese owned, put one infantry in it
        Territory sinkiang = m_data.getMap().getTerritory("Sinkiang");
        new ChangePerformer(m_data).perform(ChangeFactory.removeUnits(sinkiang, sinkiang.getUnits().getUnits()));
        
        PlayerID japanese = m_data.getPlayerList().getPlayerID("Japanese");
        sinkiang.setOwner(japanese);

        UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
        new ChangePerformer(m_data).perform(ChangeFactory.addUnits(sinkiang, infantryType.create(1, japanese)));
        
        
        
        //now move to attack it
        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);
        Territory novo = m_data.getMap().getTerritory("Novosibirsk");
        moveDelegate.move(novo.getUnits().getUnits(), m_data.getMap().getRoute(novo, sinkiang));
        
        
        moveDelegate.end();
        
        BattleDelegate battle = (BattleDelegate) m_data.getDelegateList().getDelegate("battle");
        battle.start(bridge, m_data);
        
        //fight the battle
        bridge.setRandomSource(new ScriptedRandomSource(new int[] {0,0,0}));
        bridge.setRemote(getDummyPlayer());
        battle.fightBattle(sinkiang, false);
        
        
        battle.end();
        
        assertEquals(sinkiang.getOwner(), americans);
        assertTrue(battle.getBattleTracker().wasConquered(sinkiang));
        
        bridge.setStepName("NonCombatMove");
        moveDelegate.start(bridge, m_data);
        
        Territory russia = m_data.getMap().getTerritory("Russia");
        
        
        //move two tanks from russia, then undo
        Route r = new Route();
        r.setStart(russia);
        r.add(novo);
        r.add(sinkiang);
        assertNull(moveDelegate.move(russia.getUnits().getMatches(Matches.UnitCanBlitz), r) );
        
        
        moveDelegate.undoMove(0);
        
        assertTrue(battle.getBattleTracker().wasConquered(sinkiang));
        
        //now move the planes into the territory
        assertNull(moveDelegate.move(russia.getUnits().getMatches(Matches.UnitIsAir), r) );
        //make sure they can't land, they can't because the territory was conquered
        assertEquals(1, moveDelegate.getTerritoriesWhereAirCantLand().size());
        
        
    }
    
    public void testMoveSubAwayFromSubmergedSubsInBattleZone()
    {
        Territory sz45 = m_data.getMap().getTerritory("45 Sea Zone");
        Territory sz50 = m_data.getMap().getTerritory("50 Sea Zone");
        PlayerID british = m_data.getPlayerList().getPlayerID("British");
        PlayerID japanese = m_data.getPlayerList().getPlayerID("Japanese");

        //put 1 british sub in sz 45, this simulates a submerged enemy sub
        UnitType sub = m_data.getUnitTypeList().getUnitType("submarine");
        Change c = ChangeFactory.addUnits(sz45, sub.create(1, british));
        new ChangePerformer(m_data).perform(c);
        
        
        //new move delegate
        MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
        TestDelegateBridge bridge = new TestDelegateBridge(m_data, japanese);
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);


        //move a fighter into the sea zone, this will cause a battle
        Route sz50To45 = new Route();
        sz50To45.setStart(sz50);
        sz50To45.add(sz45);
        String error = moveDelegate.move(sz50.getUnits().getMatches(Matches.UnitIsAir), sz50To45);
        assertNull(error);
        
        assertEquals(1, moveDelegate.getBattleTracker().getPendingBattleSites(false).size());
        
        
        //we should be able to move the sub out of the sz
        Route sz45To50 = new Route();
        sz45To50.setStart(sz45);
        sz45To50.add(sz50);
        
        List<Unit> japSub = sz45.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.UnitIsSub, Matches.unitIsOwnedBy(japanese)));
        error = moveDelegate.move(japSub, sz45To50);
        //make sure no error
        assertNull(error);
        //make sure the battle is still there
        assertEquals(1, moveDelegate.getBattleTracker().getPendingBattleSites(false).size());
        
        
        //we should be abe to undo the move of the sub
        error = moveDelegate.undoMove(1);
        assertNull(error);
        
        //undo the move of the fighter, should be no battles now
        error = moveDelegate.undoMove(0);
        assertNull(error);
        assertEquals(0, moveDelegate.getBattleTracker().getPendingBattleSites(false).size());
    }

    
    public void testStratBombRaidWithHeavyBombers()
    {
        Territory germany = m_data.getMap().getTerritory("Germany");
        Territory uk = m_data.getMap().getTerritory("United Kingdom");
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
        PlayerID british = m_data.getPlayerList().getPlayerID("British");
        
        
        BattleTracker tracker = new BattleTracker();
        StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, m_data, british, germans,  tracker );
        
        battle.addAttack(m_data.getMap().getRoute(uk, germany), uk.getUnits().getMatches(Matches.UnitIsStrategicBomber));

        TestDelegateBridge bridge = new TestDelegateBridge(m_data, british);
        TechTracker.addAdvance(british, m_data, bridge, TechAdvance.HEAVY_BOMBER);
        
        //aa guns rolls 3, misses, bomber rolls 2 dice at 3
        bridge.setRandomSource(new ScriptedRandomSource( new int[] {3,2,2} ));

        
        //if we try to move aa, then the game will ask us if we want to move
        //fail if we are called
        InvocationHandler handler = new InvocationHandler()
        {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
                return null;
            }
        };
        
        ITripleaPlayer player = (ITripleaPlayer) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {ITripleaPlayer.class}, handler ); 
        bridge.setRemote(player);
        
        
        int ipcsBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.IPCS));
        
        battle.fight(bridge);
        
        int ipcsAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.IPCS));
        assertEquals(ipcsBeforeRaid - 6, ipcsAfterRaid);
        

        
        
    }
    
    

    private ITripleaPlayer getDummyPlayer()
    {
        InvocationHandler handler = new InvocationHandler()
        {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
                return null;
            }
        };
        
        return (ITripleaPlayer) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {ITripleaPlayer.class}, handler ); 
        
        
    }
    
}


