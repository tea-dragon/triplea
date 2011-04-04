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
 * UnitAttatchment.java
 *
 * Created on November 8, 2001, 1:35 PM
 */

package games.strategy.triplea.attatchments;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import games.strategy.engine.data.*;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.util.IntegerMap;


/**
 *  Despite the mis leading name, this attatches not to individual Units but to UnitTypes.
 *  
 * @author  Sean Bridges
 * @version 1.0
 */
public class UnitAttachment extends DefaultAttachment
{
  /**
   * Conveniente method.
   */
  public static UnitAttachment get(UnitType type)
  {
    return (UnitAttachment) type.getAttachment(Constants.UNIT_ATTATCHMENT_NAME);
  }

  private boolean m_isAir = false;
  private boolean m_isSea = false;
  private boolean m_isAA = false;
  private boolean m_isFactory = false;
  private boolean m_canBlitz = false;
  private boolean m_isAirTransport = false;
  private boolean m_isAirTransportable = false;
  private boolean m_isSub = false;
  private boolean m_canBombard = false;
  private boolean m_isStrategicBomber = false;
  private boolean m_isTwoHit = false;
  private boolean m_isDestroyer = false;
  private boolean m_isArtillery = false;
  private boolean m_isArtillerySupportable = false;
  private boolean m_isMarine = false;
  private boolean m_isInfantry = false;
  private boolean m_isLandTransport = false;
  private boolean m_canScramble = false;
  private boolean m_isAirBase = false;
  private boolean m_isInfrastructure = false;
  private boolean m_isCombatInfrastructure = false;
  private boolean m_canBeDamaged = false;
  private boolean m_isSuicide = false;
  private boolean m_isKamikaze = false;
  private boolean m_isCombatTransport = false;
  private boolean m_isConstruction = false;
  
  // a colon delimited list of territories where this unit may not be placed
  private String[] m_unitPlacementRestrictions;
  
  // a colon delimited list of the units this unit can repair. (units must be in same territory, unless this unit is land and the repaired unit is sea)
  private String[] m_repairsUnits;
  
  // can be any String except for "none" if isConstruction is true
  private String m_constructionType = "none";

  // -1 if not set, is meaningless
  private int m_constructionsPerTerrPerTypePerTurn = -1;
  // -1 if not set, is meaningless
  private int m_maxConstructionsPerTypePerTerr = -1;
  
  //-1 if can't scramble
  private int m_maxScrambleDistance = -1;
  //-1 if can't be damaged
  private int m_maxOperationalDamage = -1;
  //-1 if can't be damaged
  private int m_maxDamage = -1;
  //-1 if can't be damaged
  private int m_unitDamage = 0;
  //-1 if cant transport
  private int m_transportCapacity = -1;
  //-1 if cant be transported
  private int m_transportCost = -1;

  //-1 if cant act as a carrier
  private int m_carrierCapacity = -1;
  //-1 if cant land on a carrier
  private int m_carrierCost = -1;
  
  //-1 if infinite (infinite is default)
  private int m_maxBuiltPerPlayer = -1;

  private int m_bombard = -1;
  private int m_unitSupportCount = -1;
  private int m_blockade = 0;
  
  private int m_bombingMaxDieSides = -1;
  private int m_bombingBonus = -1;
  private int m_attackAA = 1;
  private int m_attackAAmaxDieSides = 1;
  

  private int m_movement = 0;
  private int m_attack = 0;
  private int m_defense = 0;
  
  private Collection<PlayerID> m_canBeGivenByTerritoryTo = new ArrayList<PlayerID>();
  private Collection<PlayerID> m_destroyedWhenCapturedBy = new ArrayList<PlayerID>();
  
  private IntegerMap<UnitType> m_givesMovement = new IntegerMap<UnitType>();


  /** Creates new UnitAttatchment */
  public UnitAttachment()
  {
  }

  //does nothing, kept to avoid breaking maps
  public void setIsParatroop(String s) {}

  //does nothing, used to keep compatibility with older xml files
  public void setIsMechanized(String s) {}

  public void setIsAirTransport(String s)
  {
	  m_isAirTransport = getBool(s);
  }

  public boolean isAirTransport()
  {
    return m_isAirTransport;
  }

  public void setIsAirTransportable(String s)
  {
	  m_isAirTransportable = getBool(s);
  }

  public boolean isAirTransportable()
  {
    return m_isAirTransportable;
  }
  
  public void setCanBeGivenByTerritoryTo(String value)
  {
  	String[] temp = value.split(":");
  	for (String name : temp)
  	{
  		PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
  		if (tempPlayer != null)
  			m_canBeGivenByTerritoryTo.add(tempPlayer);
		else if (name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false"))
			m_canBeGivenByTerritoryTo.clear();
  		else
  			throw new IllegalStateException("Unit Attachments: No player named: " + name);
  	}
  }

  public Collection<PlayerID> getCanBeGivenByTerritoryTo()
  {
      return m_canBeGivenByTerritoryTo;
  }
  
  public void setDestroyedWhenCapturedBy(String value)
  {
  	String[] temp = value.split(":");
  	for (String name : temp)
  	{
  		PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
  		if (tempPlayer != null)
  			m_destroyedWhenCapturedBy.add(tempPlayer);
  		else
  			throw new IllegalStateException("Unit Attachments: No player named: " + name);
  	}
  }

  public Collection<PlayerID> getDestroyedWhenCapturedBy()
  {
      return m_destroyedWhenCapturedBy;
  }

  public void setCanBlitz(String s)
  {
    m_canBlitz = getBool(s);
  }

  public boolean getCanBlitz()
  {
    return m_canBlitz;
  }
  
  public void setIsSub(String s)
  {
    m_isSub = getBool(s);
  }

  public boolean isCombatTransport()
  {
    return m_isCombatTransport;
  }
  
  public void setIsCombatTransport(String s)
  {
	  m_isCombatTransport = getBool(s);
  }

  public boolean isSub()
  {
    return m_isSub;
  }

  public boolean isStrategicBomber()
  {
    return m_isStrategicBomber;
  }

  public void setIsStrategicBomber(String s)
  {
    m_isStrategicBomber = getBool(s);
  }

  
  public void setIsDestroyer(String s)
  {
    m_isDestroyer = getBool(s);
  }

  public boolean getIsDestroyer()
  {
    return m_isDestroyer;
  }


  public void setCanBombard(String s)
  {
    m_canBombard = getBool(s);
  }

  public boolean getCanBombard(PlayerID player)
  {
    if(m_canBombard)
        return true;
    if(m_isDestroyer && TechAttachment.get(player).hasDestroyerBombard())
      return true;
    
    return false;
  }

  public void setIsAir(String s)
  {
    m_isAir = getBool(s);
  }

  public boolean isAir()
  {
    return m_isAir;
  }

  public void setIsSea(String s)
  {
    m_isSea = getBool(s);
  }

  public boolean isSea()
  {
    return m_isSea;
  }

  public void setIsAA(String s)
  {
    m_isAA = getBool(s);
  }

  public boolean isAA()
  {
    return m_isAA;
  }

  public boolean isInfantry()
  {
    return m_isInfantry;
  }

  public boolean isMarine()
  {
    return m_isMarine;
  }

  public boolean isLandTransport()
  {
    return m_isLandTransport;
  }

  public void setIsFactory(String s)
  {
    m_isFactory = getBool(s);
  }

  public boolean isFactory()
  {
    return m_isFactory;
  }
  
  public void setUnitPlacementRestrictions(String value)
  {
	  m_unitPlacementRestrictions = value.split(":");
  }
  
  public String[] getUnitPlacementRestrictions()
  {
	  return m_unitPlacementRestrictions;
  }
  
  public void setRepairsUnits(String value)
  {
	  m_repairsUnits = value.split(":");
  }
  
  public String[] getRepairsUnits()
  {
	  return m_repairsUnits;
  }
  
  public boolean isConstruction()
  {
	  return m_isConstruction;
  }
  
  public void setIsConstruction(String s)
  {
	  m_isConstruction = getBool(s);
  }

  public void setConstructionType(String s)
  {
	  m_constructionType = s;
  }

  public String getConstructionType()
  {
    return m_constructionType;
  }

  public void setConstructionsPerTerrPerTypePerTurn(String s)
  {
	  m_constructionsPerTerrPerTypePerTurn = getInt(s);
  }

  public int getConstructionsPerTerrPerTypePerTurn()
  {
    return m_constructionsPerTerrPerTypePerTurn;
  }

  public void setMaxConstructionsPerTypePerTerr(String s)
  {
	  if (games.strategy.triplea.Properties.getUnlimitedConstructions(getData()))
		  m_maxConstructionsPerTypePerTerr = 10000;
	  else
		  m_maxConstructionsPerTypePerTerr = getInt(s);
  }

  public int getMaxConstructionsPerTypePerTerr()
  {
    return m_maxConstructionsPerTypePerTerr;
  }

  public void setIsMarine(String s)
  {
    m_isMarine = getBool(s);
  }

  public boolean getIsMarine()
  {
    return m_isMarine;
  }

  public void setIsInfantry(String s)
  {
    m_isInfantry = getBool(s);
  }
  
  public boolean getIsInfantry()
  {
    return m_isInfantry;
  }

  public void setIsLandTransport(String s)
  {
    m_isLandTransport = getBool(s);
  }
  
  public boolean getIsLandTransport()
  {
    return m_isLandTransport;
  }
  
  public void setTransportCapacity(String s)
  {
    m_transportCapacity = getInt(s);
  }

  public int getTransportCapacity()
  {
    return m_transportCapacity;
  }

  public void setIsTwoHit(String s)
  {
      m_isTwoHit = getBool(s);
  }

  public String getIsTwoHit()
  {
    return "" + m_isTwoHit;
  }

  public boolean isTwoHit()
  {
      return m_isTwoHit;
  }

  public void setTransportCost(String s)
  {
    m_transportCost = getInt(s);
  }

  public int getTransportCost()
  {
    return m_transportCost;
  }

  public void setMaxBuiltPerPlayer(String s)
  {
	  m_maxBuiltPerPlayer = getInt(s);
  }

  public int getMaxBuiltPerPlayer()
  {
    return m_maxBuiltPerPlayer;
  }

  public void setCarrierCapacity(String s)
  {
    m_carrierCapacity = getInt(s);
  }

  public int getCarrierCapacity()
  {
    return m_carrierCapacity;
  }

  public void setCarrierCost(String s)
  {
    m_carrierCost = getInt(s);
  }

  public boolean isArtillery()
  {
    return m_isArtillery;
  }

  public void setArtillery(String s) throws GameParseException
  {
    m_isArtillery = getBool(s);
    if(m_isArtillery)
    	UnitSupportAttachment.addRule((UnitType) getAttatchedTo(),getData(),false);
  }
  
  public boolean isArtillerySupportable()
  {
    return m_isArtillerySupportable;
  }

  public void setArtillerySupportable(String s) throws GameParseException
  {
    m_isArtillerySupportable = getBool(s);
    if( m_isArtillerySupportable )
    	UnitSupportAttachment.addTarget((UnitType) getAttatchedTo(),getData() );
  }

  public void setUnitSupportCount(String s)
  {
	  m_unitSupportCount = getInt(s);
	  UnitSupportAttachment.setOldSupportCount((UnitType) getAttatchedTo(),getData(),s);
  }

  
  
  
  public int getCarrierCost()
  {
    return m_carrierCost;
  }

  public void setMovement(String s)
  {
    m_movement = getInt(s);
  }

  public int getMovement(PlayerID player)
  {
    if(m_isAir)
    {
      
      if(TechTracker.hasLongRangeAir(player))
        return m_movement + 2;
    }
    return m_movement;
  }

  public void setAttack(String s)
  {
    m_attack = getInt(s);
  }

  public void setBombard(String s)
  {
    m_bombard = getInt(s);
  }
  
  public int getAttack(PlayerID player)
  {
    if(m_isSub)
    {      
      if(TechTracker.hasSuperSubs(player))
        return m_attack + 1;
    }
    
    if(m_isAir && !m_isStrategicBomber)
    {      
      if(TechTracker.hasJetFighter(player) && isWW2V3TechModel(player.getData()))
          return m_attack + 1;
    }

    return m_attack;
  }


  public int getBombard(PlayerID player)
  {
	  return m_bombard > 0 ? m_bombard : m_attack;
  }

  public int getUnitSupportCount(PlayerID player)
  {
	  return m_unitSupportCount >0 ? m_unitSupportCount : 1;
  }
  
  int getRawAttack()
  {
      return m_attack;
  }

  public void setDefense(String s)
  {
    m_defense = getInt(s);
  }

  public int getDefense(PlayerID player)
  {
    if(m_isAir && !m_isStrategicBomber)
    {      
        if(TechTracker.hasJetFighter(player) && !isWW2V3TechModel(player.getData()))
            return m_defense + 1;
    }
    if(m_isSub && TechTracker.hasSuperSubs(player))
    {

    	int bonus = games.strategy.triplea.Properties.getSuper_Sub_Defense_Bonus(player.getData());

    	if(bonus > 0)
            return m_defense + bonus;
    }
    
    return m_defense;
  }


  public int getAttackRolls(PlayerID player)
  {
    if(getAttack(player) == 0)
      return 0;

    if(m_isStrategicBomber && TechTracker.hasHeavyBomber(player))
    {        	
        return new Integer(games.strategy.triplea.Properties.getHeavy_Bomber_Dice_Rolls(getData()));
    }
    
    return 1;
  }
  
  public int getDefenseRolls(PlayerID player)
  {
    if(getDefense(player) == 0)
      return 0;
    
    if(m_isStrategicBomber && TechTracker.hasHeavyBomber(player) && games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(player.getData())) 
    {        	
        return new Integer(games.strategy.triplea.Properties.getHeavy_Bomber_Dice_Rolls(getData()));
    }
    
    return 1;
  }
  

  //Kev new properties
  public void setCanScramble(String s)
  {
	  m_canScramble = getBool(s);
  }

  public boolean getCanScramble()
  {
    return m_canScramble;
  }
  
  public void setMaxScrambleDistance(String s)
  {
	  m_maxScrambleDistance = getInt(s);
  }

  public int getMaxScrambleDistance()
  {
    return m_maxScrambleDistance;
  }
  
  public void setMaxOperationalDamage(String s)
  {
	  m_maxOperationalDamage = getInt(s);
  }

  public int getMaxOperationalDamage()
  {
    return m_maxOperationalDamage;
  }
  
  public void setMaxDamage(String s)
  {
	  m_maxDamage = getInt(s);
  }

  public int getMaxDamage()
  {
    return m_maxDamage;
  }
  
  public void setUnitDamage(String s)
  {
	  m_unitDamage = getInt(s);
  }

  public int getUnitDamage()
  {
    return m_unitDamage;
  }
  
  public void setIsAirBase(String s)
  {
	  m_isAirBase = getBool(s);
  }

  public boolean getIsAirBase()
  {
    return m_isAirBase;
  }
  
  public void setIsInfrastructure(String s)
  {
	  m_isInfrastructure = getBool(s);
  }

  public boolean getIsInfrastructure()
  {
    return m_isInfrastructure;
  }
  
  public void setIsCombatInfrastructure(String s)
  {
	  m_isCombatInfrastructure = getBool(s);
  }

  public boolean getIsCombatInfrastructure()
  {
    return m_isCombatInfrastructure;
  }
  
  
  public void setCanBeDamaged(String s)
  {
	  m_canBeDamaged = getBool(s);
  }

  public boolean getCanBeDamaged()
  {
    return m_canBeDamaged;
  }
  
  public void setIsSuicide(String s)
  {
	  m_isSuicide = getBool(s);
  }

  public boolean getIsSuicide()
  {
    return m_isSuicide;
  }
  
  public void setIsKamikaze(String s)
  {
	  m_isKamikaze = getBool(s);
  }

  public boolean getIsKamikaze()
  {
    return m_isKamikaze;
  }
  
  public void setBlockade(String s)
  {
	  m_blockade = getInt(s);
  }

  public int getBlockade()
  {
    return m_blockade;
  }

  public void setGivesMovement(String value)
  {
	  String[] s = value.split(":");
	  if (s.length <= 0 || s.length > 2)
		  throw new IllegalStateException("Unit Attachments: givesMovement can not be empty or have more than two fields");
	  
	  String unitTypeToProduce;
	  unitTypeToProduce = s[1];
	  
	  // validate that this unit exists in the xml
	  UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
      if(ut == null)
          throw new IllegalStateException("Unit Attachments: No unit called:" + unitTypeToProduce);
      
      int n = getInt(s[0]);
      //if(n <= 0)
      //	throw new IllegalStateException("Unit Attachments: givesMovement must be a positive integer");
      
      m_givesMovement.put(ut, n);
  }

  public IntegerMap<UnitType> getGivesMovement()
  {
      return m_givesMovement;
  }
  
  public void setBombingBonus(String s)
  {
	  m_bombingBonus = getInt(s);
  }

  public int getBombingBonus()
  {
    return m_bombingBonus;
  }
  
  public void setBombingMaxDieSides(String s)
  {
	  m_bombingMaxDieSides = getInt(s);
  }

  public int getBombingMaxDieSides()
  {
    return m_bombingMaxDieSides;
  }
  
  public void setAttackAA(String s)
  {
	  m_attackAA = getInt(s);
  }

  public int getAttackAA()
  {
    return m_attackAA;
  }
  
  public void setAttackAAmaxDieSides(String s)
  {
	  m_attackAAmaxDieSides = getInt(s);
  }

  public int getAttackAAmaxDieSides()
  {
    return m_attackAAmaxDieSides;
  }
  
  
  
  public String getRawProperty(String property) {
	  String s = "";
	  try {
		  Field field = getClass().getDeclaredField("m_"+property);
		  field.setAccessible(true);
		  s += field.get(this);
	  }catch (Exception e) {
		  throw new IllegalStateException("No such Property: m_" +property);
	  }
	  return s;
  }
  public void validate(GameData data) throws GameParseException
  {
    if(m_isAir)
    {
      if(m_isSea ||
        m_isFactory ||
        m_isSub ||
        m_isAA ||
        m_transportCost != -1 ||
        //m_transportCapacity != -1 ||
        m_carrierCapacity != -1 ||
        m_canBlitz ||
        m_canBombard || 
        m_isMarine ||
        m_isInfantry ||
        m_isLandTransport ||
        m_isAirTransportable || 
        m_isCombatTransport
        )
        throw new GameParseException("Invalid Unit attatchment" + this);

    }
    else if(m_isSea)
    {
      if(	m_canBlitz ||
        m_isAA ||
        m_isAir ||
        m_isFactory ||
        m_isStrategicBomber ||
        m_carrierCost != -1 ||
        m_transportCost != -1 ||
        m_isMarine ||
        m_isInfantry ||
        m_isLandTransport ||
        m_isAirTransportable ||
        m_isAirTransport || 
        m_isKamikaze
        )
        throw new GameParseException("Invalid Unit Attatchment" + this);
    }
    else //if land
    {
      if(m_canBombard ||
        m_isStrategicBomber ||
        m_isSub ||
        m_carrierCapacity != -1 ||
        m_bombard != -1 ||
        m_transportCapacity != -1 ||
        m_isAirTransport || 
        m_isCombatTransport || 
        m_isKamikaze
        )
        throw new GameParseException("Invalid Unit Attatchment" + this);
    }

    if(m_attackAA < 0 || m_attackAAmaxDieSides < -1 || m_attackAAmaxDieSides > 200)
    {
      throw new GameParseException("Invalid Unit Attatchment" + this);
    }

    if(m_carrierCapacity != -1 && m_carrierCost != -1)
    {
      throw new GameParseException("Invalid Unit Attatchment" + this);
    }

    if(m_transportCost != -1 && m_transportCapacity != -1)
    {
      throw new GameParseException("Invalid Unit Attatchment" + this);
    }

    if(((m_bombingBonus >= 0 || m_bombingMaxDieSides >= 0) && !(m_isStrategicBomber || m_isAA))
    		|| (m_bombingBonus < -1 || m_bombingMaxDieSides < -1)
    		|| (m_bombingBonus > 10000 || m_bombingMaxDieSides > 200))
    {
      throw new GameParseException("Invalid Unit Attatchment" + this);
    }

    if(m_maxBuiltPerPlayer < -1)
    {
      throw new GameParseException("Invalid Unit Attatchment" + this);
    }

    if(m_isCombatTransport && m_transportCapacity < 1)
    {
      throw new GameParseException("Invalid Unit Attatchment" + this);
    }

    if(m_isSea && m_transportCapacity != -1 && Properties.getTransportCasualtiesRestricted(data) && (m_attack > 0 || m_defense > 0) && !m_isCombatTransport) 
    {
    	throw new GameParseException("Restricted transports cannot have attack or defense");
    }
    
    if(m_isConstruction && (m_constructionType == "none" || m_constructionType == "" || m_constructionType == null || m_constructionsPerTerrPerTypePerTurn < 0 || m_maxConstructionsPerTypePerTerr < 0))
    {
    	throw new GameParseException("Constructions must have constructionType and positive constructionsPerTerrPerType and maxConstructionsPerType");
    }
    
    if(!m_isConstruction && ((m_constructionType != "none" && m_constructionType != "" && m_constructionType != null) || m_constructionsPerTerrPerTypePerTurn >= 0 || m_maxConstructionsPerTypePerTerr >= 0))
    {
    	throw new GameParseException("Constructions must have isConstruction true");
    }
    
    if(m_constructionsPerTerrPerTypePerTurn > m_maxConstructionsPerTypePerTerr)
    {
    	throw new GameParseException("Constructions must have constructionsPerTerrPerTypePerTurn Less than maxConstructionsPerTypePerTerr");
    }
    
    if(m_unitPlacementRestrictions != null)
    	getListedTerritories(m_unitPlacementRestrictions);
    
    if(m_repairsUnits != null)
    	getListedUnits(m_repairsUnits);
  }
  
  public Collection<UnitType> getListedUnits(String[] list)
  {
      List<UnitType> rVal = new ArrayList<UnitType>();
      
      for(String name : list)
      {
    	  //Validate all units exist
    	  UnitType ut = getData().getUnitTypeList().getUnitType(name);
          if(ut == null)
              throw new IllegalStateException("Unit Attachments: No unit called:" + name);
          rVal.add(ut);
      }
      return rVal;
  }
  
  public Collection<Territory> getListedTerritories(String[] list)    
  {
      List<Territory> rVal = new ArrayList<Territory>();
      
      for(String name : list)
      {
    	  //Validate all territories exist
          Territory territory = getData().getMap().getTerritory(name);
          if(territory == null)
              throw new IllegalStateException("Unit Attachments: No territory called:" + name); 
          rVal.add(territory);
      }        
      return rVal;
  }

  private boolean isWW2V3TechModel(GameData data)
  {
      return games.strategy.triplea.Properties.getWW2V3TechModel(data);
  }

  public String toString()
  {
    return
    "  air:" + m_isAir +
    "  sea:" + m_isSea +
    "  aa:" + m_isAA +
    "  factory:" + m_isFactory +
    "  blitz:" + m_canBlitz +
    "  airTransport:" + m_isAirTransport +
    "  airTransportable:" + m_isAirTransportable +
    "  sub:" + m_isSub +
    "  canBombard:" + m_canBombard +
    "  strategicBomber:" + m_isStrategicBomber +
    "  twoHit:" + m_isTwoHit +
    "  destroyer:" + m_isDestroyer +
    "  artillery:" + m_isArtillery +
    "  artillerySupportable:" + m_isArtillerySupportable +
    "  marine:" + m_isMarine +
    "  infantry:" + m_isInfantry +
    "  landTransport:" + m_isLandTransport +
    "  canScramble:" + m_canScramble +
    "  airBase:" + m_isAirBase +
    "  infrastructure:" + m_isInfrastructure +
    "  combatInfrastructure:" + m_isCombatInfrastructure +
    "  canBeDamaged:" + m_canBeDamaged +
    "  isSuicide:" + m_isSuicide + 
    "  isKamikaze:" + m_isKamikaze + 
    "  combatTransport:" + m_isCombatTransport +
    "  construction:" + m_isConstruction +
    
    "  constructionType:" + m_constructionType +
    
    "  constructionsPerTerrPerType:" + m_constructionsPerTerrPerTypePerTurn +
    "  maxConstructionsPerType:" + m_maxConstructionsPerTypePerTerr +
    "  maxScrambleDistance:" + m_maxScrambleDistance +
    "  maxOperationalDamage:" + m_maxOperationalDamage +
    "  maxDamage:" + m_maxDamage +
    "  unitDamage:" + m_unitDamage +
    "  transportCapacity:" + m_transportCapacity +
    "  transportCost:" + m_transportCost +
    "  carrierCapacity:" + m_carrierCapacity +
    "  carrierCost:" + m_carrierCost +
    "  maxBuiltPerPlayer:" + m_maxBuiltPerPlayer + 
    "  bombard:" + m_bombard +
    "  unitSupportCount:" + m_unitSupportCount +
    "  blockade:" + m_blockade +
    "  bombingMaxDieSides:" + m_bombingMaxDieSides + 
    "  bombingBonus:" + m_bombingBonus + 
    "  attackAA:" + m_attackAA + 
    "  attackAAmaxDieSides:" + m_attackAAmaxDieSides + 
    "  movement:" + m_movement +
    "  attack:" + m_attack +
    "  defense:" + m_defense;
  }
}
