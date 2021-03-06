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
 * RepairRule.java
 * 
 * Created on October 13, 2001, 10:05 AM
 */
package games.strategy.engine.data;

import games.strategy.util.IntegerMap;

import java.io.Serializable;

/**
 * 
 * @author Kevin Comcowich
 */
public class RepairRule extends DefaultNamed implements Serializable
{
	private static final long serialVersionUID = -45646671022993959L;
	private final IntegerMap<Resource> m_cost = new IntegerMap<Resource>();
	private final IntegerMap<NamedAttachable> m_results = new IntegerMap<NamedAttachable>();
	
	/** Creates new RepairRule */
	public RepairRule(final String name, final GameData data)
	{
		super(name, data);
	}
	
	protected void addCost(final Resource resource, final int quantity)
	{
		m_cost.put(resource, quantity);
	}
	
	/**
	 * Benefits must be a resource or a unit.
	 */
	protected void addResult(final NamedAttachable obj, final int quantity)
	{
		if (!(obj instanceof UnitType) && !(obj instanceof Resource))
			throw new IllegalArgumentException("results must be units or resources, not:" + obj.getClass().getName());
		m_results.put(obj, quantity);
	}
	
	public IntegerMap<Resource> getCosts()
	{
		return m_cost.copy();
	}
	
	public IntegerMap<NamedAttachable> getResults()
	{
		return m_results;
	}
	
	@Override
	public String toString()
	{
		return "RepairRule:" + getName();
	}
}
