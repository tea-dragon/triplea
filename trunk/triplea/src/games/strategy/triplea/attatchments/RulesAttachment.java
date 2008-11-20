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
 * TerritoryAttachment.java
 *
 * Created on November 8, 2001, 3:08 PM
 */

package games.strategy.triplea.attatchments;

import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.OriginalOwnerTracker;

import java.io.File;
import java.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.xml.internal.fastinfoset.util.StringArray;

import games.strategy.engine.data.*;
import games.strategy.engine.framework.GameRunner;

/**
 *
 * @author  Kevin Comcowich
 * @version 1.0
 */
public class RulesAttachment extends DefaultAttachment
{
	/**
     * Convenience method.
     */
    public static RulesAttachment get(Rule r)
    {
    	RulesAttachment rVal =  (RulesAttachment) r.getAttachment(Constants.RULES_ATTATCHMENT_NAME);
        if(rVal == null)
            throw new IllegalStateException("No rule attachment for:" + r.getName());
        return rVal;
    }
    
    private PlayerID m_ruleOwner = null;
    private int m_objectiveValue = 0;
    private String m_alliedExclusion = null;
    private String m_enemyExclusion = null;
    private int m_territoryCount = -1;
    //Territory lists
    private String[] m_alliedOwnershipTerritories;
    private String[] m_alliedExcludedTerritories;
    private String[] m_enemyExcludedTerritories;
    
    private int m_perOwnedTerritories = -1;
    private String m_allowedUnitType = null;
    
  /** Creates new RulesAttachment */
  public RulesAttachment()
  {
  }

  public void setRuleOwner(PlayerID player)
  {
	  m_ruleOwner = player;
  }
  
  public PlayerID getRuleOwner()
  {
	  return m_ruleOwner;
  }

  public void setObjectiveValue(String value)
  {
      m_objectiveValue = getInt(value);
  }

  public int getObjectiveValue()
  {
      return m_objectiveValue;
  }
  
  public void setAlliedOwnershipTerritories(String value)
  {
	  m_alliedOwnershipTerritories = value.split(":");
  }

  public String[] getAlliedOwnershipTerritories()
  {
      return m_alliedOwnershipTerritories;
  }

  //exclusion types = controlled, original, all, or list
  public void setAlliedExclusionTerritories(String value)
  {	
	  m_alliedExcludedTerritories = value.split(":");
  }

  public String[]  getAlliedExclusionTerritories()
  {
      return m_alliedExcludedTerritories;
  }
  
  //exclusion types = original or list
  public void setEnemyExclusionTerritories(String value)
  {	
	  m_enemyExcludedTerritories = value.split(":");
  }

  public String[]  getEnemyExclusionTerritories()
  {
      return m_enemyExcludedTerritories;
  }
  
  public void setTerritoryCount(String value)
  {
	  m_territoryCount = getInt(value);
  }

  public int getTerritoryCount()
  {
      return m_territoryCount;
  }
  
  public void setPerOwnedTerritories(String value)
  {
      m_perOwnedTerritories = getInt(value);
  }

  public int getPerOwnedTerritories()
  {
      return m_perOwnedTerritories;
  }

  public void setAllowedUnitType(String value)
  {
	  m_allowedUnitType = value;
  }

  public String getAllowedUnitType()
  {
      return m_allowedUnitType;
  }

  

  /**
   * Called after the attatchment is created.
   */
  public void validate() throws GameParseException
  {
      if(m_objectiveValue == 0 || ((m_alliedOwnershipTerritories == null || m_alliedOwnershipTerritories.length == 0) && 
    		  (m_enemyExcludedTerritories == null || m_enemyExcludedTerritories.length == 0) && (m_alliedExcludedTerritories == null || m_alliedExcludedTerritories.length == 0) &&
    		  m_alliedExclusion == null && m_enemyExclusion == null))
          throw new IllegalStateException("ObjectiveAttachment error for:" + m_ruleOwner + " not all variables set");
      
      if(m_alliedOwnershipTerritories != null && (!m_alliedOwnershipTerritories.equals("controlled") && !m_alliedOwnershipTerritories.equals("original") && !m_alliedOwnershipTerritories.equals("all")))
    	  getListedTerritories(m_alliedOwnershipTerritories);

      if(m_enemyExcludedTerritories != null && (!m_enemyExcludedTerritories.equals("controlled") && !m_enemyExcludedTerritories.equals("original") && !m_enemyExcludedTerritories.equals("all")))
    	  getListedTerritories(m_enemyExcludedTerritories);

      if(m_alliedExcludedTerritories != null && (!getAlliedExclusionTerritories().equals("controlled") && !m_alliedExcludedTerritories.equals("original") && !m_alliedExcludedTerritories.equals("all")))
    	  getListedTerritories(m_alliedExcludedTerritories);      
  }
  
  //Validate that all listed territories actually exist
  public Collection<Territory> getListedTerritories(String[] list)    
  {
      List<Territory> rVal = new ArrayList<Territory>();
      
      for(String name : list)
      {
    	  //See if the first entry contains the number of territories needed to meet the criteria
    	  try
    	  {
    		  int temp = getInt(name);
    		  setTerritoryCount(name);
    		  continue;    		  
    	  }
    	  catch(Exception e)
    	  {    		  
    	  }
    	  
    	  //Skip looking for the territory if the original list contains one of the 'group' commands
    	  if(name.equals("controlled") || name.equals("original") || name.equals("all"))
    		  break;
    		  //continue;
    	  
    	  //Validate all territories exist
          Territory territory = getData().getMap().getTerritory(name);
          if(territory == null)
              throw new IllegalStateException("No territory called:" + name); 
          rVal.add(territory);
      }        
      return rVal;
  }
  
  
  
  
  
  //May need this for rules
  /*
  public static Set<RulesAttachment> get(Territory t)
  {
      Set<RulesAttachment> rVal = new HashSet<RulesAttachment>();
      Map<String, IAttachment> map = t.getAttachments();
      Iterator<String> iter = map.keySet().iterator();
      while(iter.hasNext() )
      {
          IAttachment attachment = map.get(iter.next());
          String name = attachment.getName();
          if (name.startsWith(Constants.RULES_OBJECTIVE_PREFIX))
          {
              rVal.add((RulesAttachment)attachment);
          }
      }
      return rVal;
      
  }
  
  
  public static String [] stringToArray(String str) {
	    StringTokenizer t = new StringTokenizer(str, ",");
	    String [] array = new String[t.countTokens()];

	    for(int i=0; t.hasMoreTokens(); ++i) {
	      array[i] = t.nextToken();
	    }
	    return(array);
	  }
*/
}
