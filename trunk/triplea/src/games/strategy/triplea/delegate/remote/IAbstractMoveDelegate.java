package games.strategy.triplea.delegate.remote;

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
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;

import java.util.List;

/**
 * Remote interface for MoveDelegate and PlaceDelegate
 * 
 * @author Sean Bridges
 */
public interface IAbstractMoveDelegate extends IRemote, IDelegate
{
	/**
	 * Get the moves already made
	 * 
	 * @return a list of UndoableMoves
	 */
	@SuppressWarnings("rawtypes")
	// need to be able to handle different Move types e.g. Placement and Movement
	public abstract List getMovesMade();
	
	/**
	 * 
	 * @param moveIndex
	 *            - an index in the list getMovesMade
	 * @return an error string if the move could not be undone, null otherwise
	 */
	public abstract String undoMove(int moveIndex);
}
