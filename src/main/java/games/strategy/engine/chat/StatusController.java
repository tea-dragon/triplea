/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.engine.chat;

import games.strategy.engine.message.MessageContext;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;

import java.util.HashMap;
import java.util.Map;

public class StatusController implements IStatusController
{
	private final Object m_mutex = new Object();
	private final Map<INode, String> m_status = new HashMap<INode, String>();
	private final Messengers m_messengers;
	
	public StatusController(final Messengers messengers)
	{
		m_messengers = messengers;
		((IServerMessenger) m_messengers.getMessenger()).addConnectionChangeListener(new IConnectionChangeListener()
		{
			public void connectionRemoved(final INode to)
			{
				StatusController.this.connectionRemoved(to);
			}
			
			public void connectionAdded(final INode to)
			{
			}
		});
	}
	
	protected void connectionRemoved(final INode to)
	{
		synchronized (m_mutex)
		{
			m_status.remove(to);
		}
		final IStatusChannel channel = (IStatusChannel) m_messengers.getChannelMessenger().getChannelBroadcastor(IStatusChannel.STATUS_CHANNEL);
		channel.statusChanged(to, null);
	}
	
	public Map<INode, String> getAllStatus()
	{
		synchronized (m_mutex)
		{
			return new HashMap<INode, String>(m_status);
		}
	}
	
	public void setStatus(final String newStatus)
	{
		final INode node = MessageContext.getSender();
		synchronized (m_mutex)
		{
			m_status.put(node, newStatus);
		}
		final IStatusChannel channel = (IStatusChannel) m_messengers.getChannelMessenger().getChannelBroadcastor(IStatusChannel.STATUS_CHANNEL);
		channel.statusChanged(node, newStatus);
	}
}
