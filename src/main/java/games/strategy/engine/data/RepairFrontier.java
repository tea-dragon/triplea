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
 * RepairFrontier.java
 * 
 * Created on October 13, 2001, 10:48 AM
 */
package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Kevin Comcowich
 * @version 1.0
 */
public class RepairFrontier extends DefaultNamed implements Iterable<RepairRule>
{
	private static final long serialVersionUID = -5148536624986056753L;
	private final List<RepairRule> m_rules = new ArrayList<RepairRule>();
	private List<RepairRule> m_cachedRules;
	
	/**
	 * Creates new RepairFrontier
	 * 
	 * @param name
	 *            name of new repair frontier
	 * @param data
	 *            game data
	 */
	public RepairFrontier(final String name, final GameData data)
	{
		super(name, data);
	}
	
	public void addRule(final RepairRule rule)
	{
		if (m_rules.contains(rule))
			throw new IllegalStateException("Rule already added:" + rule);
		m_rules.add(rule);
		m_cachedRules = null;
	}
	
	public void removeRule(final RepairRule rule)
	{
		if (!m_rules.contains(rule))
			throw new IllegalStateException("Rule not present:" + rule);
		m_rules.remove(rule);
		m_cachedRules = null;
	}
	
	public List<RepairRule> getRules()
	{
		if (m_cachedRules == null)
			m_cachedRules = Collections.unmodifiableList(m_rules);
		return m_cachedRules;
	}
	
	public Iterator<RepairRule> iterator()
	{
		return getRules().iterator();
	}
}
