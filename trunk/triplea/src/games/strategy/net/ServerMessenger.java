/*
 * ServerMessenger.java
 *
 * Created on December 11, 2001, 7:43 PM
 */

package games.strategy.net;

import java.util.*;
import java.net.*;
import java.io.*;

import games.strategy.util.ListenerList;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class ServerMessenger implements IServerMessenger
{
	private ServerSocket m_socket;
	private Node m_node;
	private Set m_allNodes;
	private boolean m_shutdown = false;
	private ListenerList m_connections = new ListenerList();
	private ListenerList m_listeners = new ListenerList();
	private ListenerList m_errorListeners = new ListenerList();
	private ListenerList m_connectionListeners = new ListenerList();
	private boolean m_acceptNewConnection = false;
	
	private IObjectStreamFactory m_inStreamFactory;
	
	public ServerMessenger(String name, int portNumber, IObjectStreamFactory streamFactory) throws IOException
	{
		m_inStreamFactory = streamFactory;
		m_socket = new ServerSocket(portNumber, 10);
		
		m_node = new Node(name, m_socket.getInetAddress(), m_socket.getLocalPort());
		m_allNodes = new HashSet();
		m_allNodes = Collections.synchronizedSet(m_allNodes);
		m_allNodes.add(m_node);
			
		Thread t = new Thread(new ConnectionHandler());
		t.start();		
	}

	/** Creates new ServerMessenger */
    public ServerMessenger(String name, int portNumber) throws IOException
	{
		this(name, portNumber, new DefaultObjectStreamFactory());
    }

	/*
	 * @see IMessenger#addMessageListener(Class, IMessageListener)
	 */
	public void addMessageListener(IMessageListener listener) 
	{
		m_listeners.add(listener);
	}

	/*
	 * @see IMessenger#removeMessageListener(Class, IMessageListener)
	 */
	public void removeMessageListener(IMessageListener listener) 
	{
		m_listeners.remove(listener);	
	}
	
	/**
	 * Get a list of nodes.
	 */
	public Set getNodes() 
	{
		return Collections.unmodifiableSet(m_allNodes);
	}
	
	public synchronized void shutDown() 
	{
		if(!m_shutdown)
		{	
			m_shutdown = true;
			
			try
			{
				m_socket.close();
			} catch(Exception e)
			{}
			
			Iterator iter = m_connections.iterator();
			while(iter.hasNext())
			{
				Connection current = (Connection) iter.next();
				current.shutDown();
			}
			m_allNodes = Collections.EMPTY_SET;
		}
	}
	
	public boolean isConnected() 
	{
		return !m_shutdown;
	}
	
	/**
	 * Send a message to the given node.
	 */
	public void send(Serializable msg, INode to) 
	{
		MessageHeader header = new MessageHeader(to, m_node, msg);
		forward(header);
	}

	public void flush()
	{
		Iterator iter = m_connections.iterator();
		while(iter.hasNext())
		{
			Connection c = (Connection) iter.next();
			c.flush();
		}	
	}

	
	/**
	 * Send a message to all nodes.
	 */
	public void broadcast(Serializable msg) 
	{
		MessageHeader header = new MessageHeader(m_node, msg);
		forwardBroadcast(header);
	}
	
	private void serverMessageReceived(ServerMessage msg)
	{
		
	}
	
	private void messageReceived(MessageHeader msg)
	{
		if(msg.getMessage() instanceof ServerMessage)
			serverMessageReceived( (ServerMessage) msg.getMessage());
		else
		{
			
			if(msg.getFor() == null || msg.getFor().equals(m_node))
			{
				notifyListeners(msg);
			}
			
			if(msg.getFor() == null)
				forwardBroadcast(msg);
			else
				forward(msg);	
		}
	}
	
	
	private void forward(MessageHeader msg)
	{
		if(m_shutdown)
			return;
		INode destination = msg.getFor();
		Iterator iter = m_connections.iterator();
		while(iter.hasNext())
		{
			Connection connection = (Connection) iter.next();
			if(connection.getRemoteNode().equals(destination))
			{
				
				connection.send(msg);
				break;
			}
		}
	}
	
	private void forwardBroadcast(MessageHeader msg)
	{
		if(m_shutdown)
			return;
		
		INode source = msg.getFrom();
		Iterator iter = m_connections.iterator();
		while(iter.hasNext())
		{
			Connection connection = (Connection) iter.next();
			if(!connection.getRemoteNode().equals(source))
			{
				
				connection.send(msg);
			}
		}
	}
	
	private synchronized void addConnection(Socket s)
	{
		Connection c = null;
		
		try
		{
			c = new Connection(s, m_node, m_connectionListener, m_inStreamFactory);
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		if(c == null)
			return;
		
		m_allNodes.add(c.getRemoteNode());
		
		ClientInitServerMessage init = new ClientInitServerMessage( new HashSet(m_allNodes));	
		MessageHeader header = new MessageHeader(m_node, c.getRemoteNode(), init);
		c.send(header);
		
		NodeChangeServerMessage change = new NodeChangeServerMessage(true, c.getRemoteNode());
		broadcast(change);
		m_connections.add(c);
		
		notifyConnectionsChanged();
	}
	
	private void removeConnection(Connection c)
	{
		NodeChangeServerMessage change = new NodeChangeServerMessage(false, c.getRemoteNode());
		m_allNodes.remove(c.getRemoteNode());
		broadcast(change);
		notifyConnectionsChanged();
	}
	
	private void notifyListeners(MessageHeader msg)
	{
		Iterator iter = m_listeners.iterator();
		while(iter.hasNext())
		{
			IMessageListener listener = (IMessageListener) iter.next();
			listener.messageReceived(msg.getMessage(), msg.getFrom());
		}
	}
	
	public void addErrorListener(IMessengerErrorListener  listener) 
	{
		m_errorListeners.add(listener);
	}

	public void removeErrorListener(IMessengerErrorListener  listener) 
	{
		m_errorListeners.remove(listener);
	}

	public void addConnectionChangeListener(IConnectionChangeListener  listener) 
	{
		m_connectionListeners.add(listener);
	}

	public void removeConnectionChangeListener(IConnectionChangeListener listener) 
	{
		m_connectionListeners.remove(listener);
	}

	private void notifyConnectionsChanged()
	{
		Iterator iter =  m_connectionListeners.iterator();
		while(iter.hasNext())
		{
			((IConnectionChangeListener) iter.next()).connectionsChanged();
		}
	}
	
	public void setAcceptNewConnections(boolean accept)
	{	
		m_acceptNewConnection = accept;	
	}	

	/**
	 * Get the local node
	 */
	public INode getLocalNode()
	{
		return m_node;
	}

	private class ConnectionHandler implements Runnable
	{
		public void run()
		{
			while(!m_shutdown)
			{
				try
				{
					Socket s = m_socket.accept();
					if(m_acceptNewConnection)
					{
						addConnection(s);
					}
					else
					{
						s.close();
					}
					
				} catch(IOException e)
				{
					//e.printStackTrace();
				}
			}
		}
	}		
	
	private IConnectionListener m_connectionListener = new IConnectionListener()
	{
		public void messageReceived(Serializable message, Connection connection)
		{
			ServerMessenger.this.messageReceived( (MessageHeader) message);
		}
		
		public void fatalError(Exception error, Connection connection, List unsent)
		{
			//notify other nodes
			removeConnection(connection);
			
			//nofity this node
			Iterator iter = m_errorListeners.iterator();
			while(iter.hasNext())
			{
				IMessengerErrorListener errorListener = (IMessengerErrorListener) iter.next();
				errorListener.connectionLost(connection.getRemoteNode(), error, unsent);
			}
		}
	};
}