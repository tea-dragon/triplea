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
 * BattleListingMessage.java
 *
 * Created on November 29, 2001, 6:12 PM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;
import games.strategy.engine.message.Message;

/**
 * Sent by the battle delegate to the game player to indicate 
 * which battles are left to be fought.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class BattleListing implements Message
{

	private Collection m_battles;
	private Collection m_strategicRaids;
	
	/** Creates new BattleListingMessage */
    public BattleListing(Collection battles, Collection strategicRaids) 
	{
		m_battles = battles;
		m_strategicRaids = strategicRaids;
    }
	
	public Collection getBattles()
	{
		return m_battles;
	}
	
	public Collection getStrategicRaids()
	{
		return m_strategicRaids;
	}
	
	public boolean isEmpty()
	{
		return m_battles.size() == 0 && m_strategicRaids.size() == 0;
	}

}
