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
import java.util.*;
import games.strategy.engine.data.*;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.formatter.*;
import games.strategy.util.Match;

/**
 * Used to store information about a dice roll.
 *  # of rolls at 5, at 4, etc.<p>
 *  
 *  Externalizble so we can efficiently write out our dice as ints
 *  rather than as full objects.
 */
public class DiceRoll implements Externalizable
{

    private List<Die> m_rolls;
    //this does not need to match the Die with isHit true
    //since for low luch we get many hits with few dice
    private int m_hits;

    public static DiceRoll rollAA(int numberOfAirUnits, IDelegateBridge bridge, Territory location, GameData data)
    {
        int hits = 0;
        int[] dice = new int[0];
        List<Die> sortedDice = new ArrayList<Die>();

        if (data.getProperties().get(Constants.LOW_LUCK, false))
        {
            // Low luck rolling
            hits = numberOfAirUnits / Constants.MAX_DICE;
            int hitsFractional = numberOfAirUnits % Constants.MAX_DICE;

            if (hitsFractional > 0)
            {
                dice = bridge.getRandom(Constants.MAX_DICE, 1, "Roll aa guns in " + location.getName());
                boolean hit = hitsFractional > dice[0];
                Die die = new Die(dice[0], 1, hit);
                
                sortedDice.add(die);
                if (hit)
                {
                    hits++;
                }
            }
        } 
        else
        {
            
            // Normal rolling
            dice = bridge.getRandom(Constants.MAX_DICE, numberOfAirUnits, "Roll aa guns in " + location.getName());
            for (int i = 0; i < dice.length; i++)
            {
                boolean hit = dice[i] == 0;
                sortedDice.add(new Die(dice[i], 1, hit));
                if (hit)
                    hits++;
            }
        }

        DiceRoll roll = new DiceRoll(sortedDice, hits);
        bridge.getHistoryWriter().addChildToEvent("AA guns fire in" + location + " :" + MyFormatter.asDice(dice), roll);
        return roll;
    }

    /**
     * Roll dice for units.
     *  
     */
    public static DiceRoll rollDice(List<Unit> units, boolean defending, PlayerID player, IDelegateBridge bridge, GameData data, Battle battle)
    {
        // Decide whether to use low luck rules or normal rules.
        if (data.getProperties().get(Constants.LOW_LUCK, false))
        {
            return rollDiceLowLuck(units, defending, player, bridge,  battle);
        } else
        {
            return rollDiceNormal(units, defending, player, bridge, battle);
        }
    }

    /**
     * Roll dice for units using low luck rules. Low luck rules based on rules
     * in DAAK.
     */
    private static DiceRoll rollDiceLowLuck(List<Unit> units, boolean defending, PlayerID player, IDelegateBridge bridge, Battle battle)
    {
        String annotation = getAnnotation(units, player, battle);

        int rollCount = BattleCalculator.getRolls(units, player, defending);
        if (rollCount == 0)
        {
            return new DiceRoll(new ArrayList<Die>(0), 0);
        }

        int artillerySupportAvailable = 0;
        if (!defending)
            artillerySupportAvailable = Match.countMatches(units, Matches.UnitIsArtillery);

        Iterator iter = units.iterator();

        int power = 0;
        int hitCount = 0;

        // We iterate through the units to find the total strength of the units
        while (iter.hasNext())
        {
            Unit current = (Unit) iter.next();
            UnitAttachment ua = UnitAttachment.get(current.getType());
            int rolls = defending ? 1 : ua.getAttackRolls(player);
            for (int i = 0; i < rolls; i++)
            {
                int strength;
                if (defending)
                    strength = ua.getDefense(current.getOwner());
                else
                {
                    strength = ua.getAttack(current.getOwner());
                    if (ua.isArtillerySupportable() && artillerySupportAvailable > 0)
                    {
                        strength++;
                        artillerySupportAvailable--;
                    }
                    if (ua.getIsMarine() && battle.isAmphibious())
                    {
                        Collection<Unit> landUnits = battle.getAmphibiousLandAttackers();
                        if(!landUnits.contains(current))
                            ++strength;
                    } 
                }
               
                power += strength;
            }
        }

        // Get number of hits
        hitCount = power / Constants.MAX_DICE;

        int[] random = new int[0];

        List<Die> dice = new ArrayList<Die>();
        // We need to roll dice for the fractional part of the dice.
        power = power % Constants.MAX_DICE;
        if (power != 0)
        {
            random = bridge.getRandom(Constants.MAX_DICE, 1, annotation);
            boolean hit = power > random[0]; 
            if (hit)
            {
                hitCount++;
            }
            dice.add(new Die(random[0], power, hit));
        }

        // Create DiceRoll object
        DiceRoll rVal = new DiceRoll(dice, hitCount);
        bridge.getHistoryWriter().addChildToEvent(annotation + " : " + MyFormatter.asDice(random), rVal);
        return rVal;
    }

    /**
     * Roll dice for units per normal rules.
     */
    private static DiceRoll rollDiceNormal(List<Unit> units, boolean defending, PlayerID player, IDelegateBridge bridge, Battle battle)
    {
        String annotation = getAnnotation(units, player, battle);

        int rollCount = BattleCalculator.getRolls(units, player, defending);
        if (rollCount == 0)
        {
            return new DiceRoll(new ArrayList<Die>(), 0);
        }

        int artillerySupportAvailable = 0;
        if (!defending)
            artillerySupportAvailable = Match.countMatches(units, Matches.UnitIsArtillery);

        int[] random = bridge.getRandom(Constants.MAX_DICE, rollCount, annotation);
        List<Die> dice = new ArrayList<Die>();
        
        Iterator iter = units.iterator();

        int hitCount = 0;
        int diceIndex = 0;
        while (iter.hasNext())
        {
            Unit current = (Unit) iter.next();
            UnitAttachment ua = UnitAttachment.get(current.getType());

            int rolls = defending ? 1 : ua.getAttackRolls(player);

            for (int i = 0; i < rolls; i++)
            {
                int strength;
                if (defending)
                    strength = ua.getDefense(current.getOwner());
                else
                {
                    strength = ua.getAttack(current.getOwner());
                    if (ua.isArtillerySupportable() && artillerySupportAvailable > 0)
                    {
                        strength++;
                        artillerySupportAvailable--;
                    }
                    if (ua.getIsMarine() && battle.isAmphibious())
                    {
                        Collection<Unit> landUnits = battle.getAmphibiousLandAttackers();
                        if(!landUnits.contains(current))
                            ++strength;
                    } 
                }

                boolean hit = strength > random[diceIndex];
                dice.add(new Die(random[diceIndex], strength, hit ));

                if (hit)
                    hitCount++;
                diceIndex++;
            }
        }

        DiceRoll rVal = new DiceRoll(dice, hitCount);
        bridge.getHistoryWriter().addChildToEvent(annotation + " : " + MyFormatter.asDice(random), rVal);
        return rVal;
    }

    /**
     * @param units
     * @param player
     * @param battle
     * @return
     */
    private static String getAnnotation(List<Unit> units, PlayerID player, Battle battle)
    {
        String annotation = player.getName() + " roll dice for " + MyFormatter.unitsToTextNoOwner(units);
        if (battle != null)
            annotation = annotation + " in " + battle.getTerritory().getName() + ", round " + (battle.getBattleRound() + 1);
        return annotation;

    }

    /**
     * 
     * @param dice
     *            int[] the dice, 0 based
     * @param hits
     *            int - the number of hits
     * @param rollAt
     *            int - what we roll at, [0,Constants.MAX_DICE]
     * @param hitOnlyIfEquals
     *            boolean - do we get a hit only if we are equals, or do we hit
     *            when we are equal or less than for example a 5 is a hit when
     *            rolling at 6 for equal and less than, but is not for equals
     */
    public DiceRoll(int[] dice, int hits, int rollAt, boolean hitOnlyIfEquals)
    {
        m_hits = hits;
        m_rolls = new ArrayList<Die>(dice.length);
        
        for(int i =0; i < dice.length; i++)
        {
            boolean hit;
            if(hitOnlyIfEquals)
                hit = (rollAt == dice[i]);
            else
                hit = dice[i] <= rollAt;
           
            m_rolls.add(new Die(dice[i], rollAt, hit));
        }
    }

    //only for externalizable
    public DiceRoll()
    {
        
    }
    
    private DiceRoll(List<Die> dice, int hits)
    {
        m_rolls = new ArrayList<Die>(dice);
        m_hits = hits;
    }

    public int getHits()
    {
        return m_hits;
    }

    /**
     * @param rollAt
     *            the strength of the roll, eg infantry roll at 2, expecting a
     *            number in [1,6]
     * @return in int[] which shouldnt be modifed, the int[] is 0 based, ie
     *         0..MAX_DICE
     */
    public List<Die> getRolls(int rollAt)
    {
        List<Die> rVal = new ArrayList<Die>();
        for(Die die : m_rolls)
        {
            if(die.getRolledAt() == rollAt)
                rVal.add(die);
        }
        return rVal;
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        int[] dice = new int[m_rolls.size()];
        for(int i =0; i < m_rolls.size(); i++)
        {
            dice[i] = m_rolls.get(i).getCompressedValue();
        }
        out.writeObject(dice);
        out.writeInt(m_hits);
        
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        int[] dice = (int[]) in.readObject();
        m_rolls = new ArrayList<Die>(dice.length);
        for(int i=0; i < dice.length; i++)
        {
            m_rolls.add(Die.getFromWriteValue(dice[i]));
        }
        
        m_hits = in.readInt();
        
    }

}
