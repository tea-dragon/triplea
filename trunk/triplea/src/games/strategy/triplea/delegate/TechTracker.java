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
/*
 * TechTracker.java
 * 
 * Created on November 30, 2001, 2:20 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attatchments.TechAttachment;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Tracks which players have which technology advances.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class TechTracker implements java.io.Serializable
{
	private static final long serialVersionUID = 4705039229340373735L;
	
	/** Creates new TechTracker */
	public TechTracker()
	{
	}
	
	public static Collection<TechAdvance> getTechAdvances(final PlayerID id, final GameData data)
	{
		final Collection<TechAdvance> rVal = new ArrayList<TechAdvance>();
		final TechAttachment attachment = TechAttachment.get(id);
		for (final TechAdvance ta : TechAdvance.getTechAdvances(data, id))
		{
			if (ta.hasTech(attachment))
				rVal.add(ta);
		}
		/*
		TechAttachment attachment = TechAttachment.get(id);

		if(attachment.hasHeavyBomber())
		{
		  rVal.add(TechAdvance.HEAVY_BOMBER);
		}
		if(attachment.hasIndustrialTechnology())
		{
		  rVal.add(TechAdvance.INDUSTRIAL_TECHNOLOGY);
		}
		if(attachment.hasJetPower())
		{
		  rVal.add(TechAdvance.JET_POWER);
		}
		if(attachment.hasLongRangeAir())
		{
		  rVal.add(TechAdvance.LONG_RANGE_AIRCRAFT);
		}
		if(attachment.hasRocket())
		{
		  rVal.add(TechAdvance.ROCKETS);
		}
		if(attachment.hasSuperSub())
		{
		  rVal.add(TechAdvance.SUPER_SUBS);
		}
		if(attachment.hasDestroyerBombard())
		{
		  rVal.add(TechAdvance.DESTROYER_BOMBARD);
		}
		if(attachment.hasImprovedArtillerySupport())
		{
		  rVal.add(TechAdvance.IMPROVED_ARTILLERY_SUPPORT);
		}
		if(attachment.hasParatroopers())
		{
		  rVal.add(TechAdvance.PARATROOPERS);
		}
		if(attachment.hasIncreasedFactoryProduction())
		{
		  rVal.add(TechAdvance.INCREASED_FACTORY_PRODUCTION);
		}
		if(attachment.hasWarBonds())
		{
		  rVal.add(TechAdvance.WAR_BONDS);
		}
		if(attachment.hasMechanizedInfantry())
		{
		  rVal.add(TechAdvance.MECHANIZED_INFANTRY);
		}
		if(attachment.hasAARadar())
		{
		  rVal.add(TechAdvance.AA_RADAR);
		}
		if(attachment.hasShipyards())
		{
		  rVal.add(TechAdvance.IMPROVED_SHIPYARDS);
		}

		*/
		return rVal;
	}
	
	public static Collection<TechnologyFrontier> getTechCategories(final GameData data, final PlayerID id)
	{
		final Collection<TechnologyFrontier> rVal = new ArrayList<TechnologyFrontier>();
		final TechAttachment attachment = TechAttachment.get(id);
		for (final TechnologyFrontier tf : TechAdvance.getTechCategories(data, id))
		{
			boolean has = true;
			for (final TechAdvance t : tf.getTechs())
			{
				has = t.hasTech(attachment);
				if (!has)
					break;
			}
			if (has)
				rVal.add(tf);
		}
		/*
		if(attachment.hasSuperSub() && attachment.hasJetPower() && attachment.hasShipyards() &&
		                attachment.hasAARadar() && attachment.hasLongRangeAir() && attachment.hasHeavyBomber())
		{
		  rVal.add(TechAdvance.AIR_NAVAL_ADVANCES);
		}

		if(attachment.hasImprovedArtillerySupport() && attachment.hasRocket() && attachment.hasParatroopers() &&
		                attachment.hasIncreasedFactoryProduction() && attachment.hasWarBonds() && attachment.hasMechanizedInfantry())
		{
		  rVal.add(TechAdvance.LAND_PRODUCTION_ADVANCES);
		}
		*/
		return rVal;
	}
	
	public static int getTechCost(final PlayerID id)
	{
		final TechAttachment ta = TechAttachment.get(id);
		return ta.getTechCost();
	}
	
	public static synchronized void addAdvance(final PlayerID player, final IDelegateBridge bridge, final TechAdvance advance)
	{
		Change attachmentChange;
		if (advance instanceof GenericTechAdvance)
		{
			if (((GenericTechAdvance) advance).getAdvance() == null)
			{
				attachmentChange = ChangeFactory.genericTechChange(TechAttachment.get(player), true, advance.getProperty());
			}
			else
				attachmentChange = ChangeFactory.attachmentPropertyChange(TechAttachment.get(player), "true", advance.getProperty());
		}
		else
			attachmentChange = ChangeFactory.attachmentPropertyChange(TechAttachment.get(player), "true", advance.getProperty());
		bridge.addChange(attachmentChange);
		advance.perform(player, bridge);
	}
	
	/*
	private static String capitalizeFirstLetter(String aString)
	{
		char first = aString.charAt(0);
		first = Character.toUpperCase(first);
		return first + aString.substring(1);
	}
	*/
	public static boolean hasLongRangeAir(final PlayerID player)
	{
		return TechAttachment.get(player).getLongRangeAir();
	}
	
	public static boolean hasHeavyBomber(final PlayerID player)
	{
		return TechAttachment.get(player).getHeavyBomber();
	}
	
	public static boolean hasSuperSubs(final PlayerID player)
	{
		return TechAttachment.get(player).getSuperSub();
	}
	
	public static boolean hasJetFighter(final PlayerID player)
	{
		return TechAttachment.get(player).getJetPower();
	}
	
	public static boolean hasRocket(final PlayerID player)
	{
		return TechAttachment.get(player).getRocket();
	}
	
	public static boolean hasIndustrialTechnology(final PlayerID player)
	{
		return TechAttachment.get(player).getIndustrialTechnology();
	}
	
	public static boolean hasDestroyerBombard(final PlayerID player)
	{
		return TechAttachment.get(player).getDestroyerBombard();
	}
	
	public static boolean hasImprovedArtillerySupport(final PlayerID player)
	{
		return TechAttachment.get(player).getImprovedArtillerySupport();
	}
	
	public static boolean hasParatroopers(final PlayerID player)
	{
		return TechAttachment.get(player).getParatroopers();
	}
	
	public static boolean hasIncreasedFactoryProduction(final PlayerID player)
	{
		return TechAttachment.get(player).getIncreasedFactoryProduction();
	}
	
	public static boolean hasWarBonds(final PlayerID player)
	{
		return TechAttachment.get(player).getWarBonds();
	}
	
	public static boolean hasMechanizedInfantry(final PlayerID player)
	{
		return TechAttachment.get(player).getMechanizedInfantry();
	}
	
	public static boolean hasAARadar(final PlayerID player)
	{
		return TechAttachment.get(player).getAARadar();
	}
	
	public static boolean hasShipyards(final PlayerID player)
	{
		return TechAttachment.get(player).getShipyards();
	}
}
