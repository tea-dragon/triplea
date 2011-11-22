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
package games.strategy.common.player;

import games.strategy.common.ui.MainGameFrame;

/**
 * As a rule, nothing that changes GameData should be in here (it should be in a delegate, and done through an IDelegate using a change).
 * 
 * @author Lane Schwartz
 * 
 */
public abstract class AbstractHumanPlayer<CustomGameFrame extends MainGameFrame> extends AbstractBasePlayer
{
	protected CustomGameFrame m_ui;
	
	/**
	 * @param name
	 *            - the name of the player.
	 */
	public AbstractHumanPlayer(final String name, final String type)
	{
		super(name, type);
	}
	
	@Override
	public abstract void start(String stepName);
	
	public final void setFrame(final CustomGameFrame frame)
	{
		m_ui = frame;
	}
}
