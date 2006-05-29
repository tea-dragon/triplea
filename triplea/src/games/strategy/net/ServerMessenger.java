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
 * ServerMessenger.java
 *
 * Created on December 11, 2001, 7:43 PM
 */

package games.strategy.net;

import java.util.*;
import java.util.logging.*;
import java.net.*;
import java.io.*;

import games.strategy.util.ListenerList;

/**
 * A Messenger that can have many clients connected to it.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class ServerMessenger implements IServerMessenger
{
    private Logger s_logger = Logger.getLogger(ServerMessenger.class.getName());
    
    private final ServerSocket m_socket;

    private final Node m_node;

    private final Set<INode> m_allNodes = Collections.synchronizedSet(new HashSet<INode>());

    private boolean m_shutdown = false;

    private final ListenerList<Connection> m_connections = new ListenerList<Connection>();

    private final ListenerList<IMessageListener> m_listeners = new ListenerList<IMessageListener>();

    private final ListenerList<IMessengerErrorListener> m_errorListeners = new ListenerList<IMessengerErrorListener>();

    private final ListenerList<IConnectionChangeListener> m_connectionListeners = new ListenerList<IConnectionChangeListener>();

    private boolean m_acceptNewConnection = false;

    private IObjectStreamFactory m_inStreamFactory;
    
    public ServerMessenger(String name, int portNumber, IObjectStreamFactory streamFactory) throws IOException
    {
        m_inStreamFactory = streamFactory;
        m_socket = new ServerSocket();
        m_socket.setReuseAddress(true);
        m_socket.bind(new InetSocketAddress(portNumber), 10);

        if (IPFinder.findInetAddress() != null)
            m_node = new Node(name, IPFinder.findInetAddress(), m_socket.getLocalPort());
        else
            m_node = new Node(name, InetAddress.getLocalHost(), m_socket.getLocalPort());

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
    public Set<INode> getNodes()
    {
        synchronized (m_allNodes)
        {
            return new HashSet<INode>(m_allNodes);
        }
    }

    public synchronized void shutDown()
    {
        if (!m_shutdown)
        {
            m_shutdown = true;

            try
            {
                m_socket.close();
            } catch (Exception e)
            {
            }

            Iterator<Connection> iter = m_connections.iterator();
            while (iter.hasNext())
            {
                Connection current = iter.next();
                current.shutDown();
            }
            m_allNodes.clear();
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
        Iterator<Connection> iter = m_connections.iterator();
        while (iter.hasNext())
        {
            Connection c = iter.next();
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
        if (msg.getMessage() instanceof ServerMessage)
            serverMessageReceived((ServerMessage) msg.getMessage());
        else
        {

            if (msg.getFor() == null || msg.getFor().equals(m_node))
            {
                notifyListeners(msg);
            }

            if (msg.getFor() == null)
                forwardBroadcast(msg);
            else
                forward(msg);
        }
    }

    private void forward(MessageHeader msg)
    {
        if (m_shutdown)
            return;
        INode destination = msg.getFor();
        Iterator<Connection> iter = m_connections.iterator();
        while (iter.hasNext())
        {
            Connection connection = iter.next();
            if (connection.getRemoteNode().equals(destination))
            {

                connection.send(msg);
                break;
            }
        }
    }

    private void forwardBroadcast(MessageHeader msg)
    {
        if (m_shutdown)
            return;

        INode source = msg.getFrom();
        Iterator<Connection> iter = m_connections.iterator();
        while (iter.hasNext())
        {
            Connection connection = iter.next();
            if (!connection.getRemoteNode().equals(source))
            {

                connection.send(msg);
            }
        }
    }

    private boolean isNameTaken(String nodeName)
    {
        synchronized (m_allNodes)
        {
            for (INode node : m_allNodes)
            {
                if (node.getName().equalsIgnoreCase(nodeName))
                    return true;
            }
        }

        return false;
    }

    private void ensureValidName(Connection c)
    {
        String currentName = c.getRemoteNode().getName();
        boolean replace = false;

        if (currentName.length() > 50)
        {
            currentName.substring(0, 50);
            replace = true;
        }
        if (currentName.length() < 2)
        {
            currentName = "aa" + currentName;
            replace = true;
        }

        if (isNameTaken(currentName))
        {
            replace = true;
            int i = 1;
            while (true)
            {
                String newName = currentName + "_" + i;
                if (!isNameTaken(newName))
                {
                    currentName = newName;
                    break;
                }
                i++;
            }
        }

        if (replace)
            c.setRemoteName(currentName);
    }

    private synchronized void addConnection(Socket s)
    {
        //TODO - dont synchronize this entire method
        Connection c = null;

        try
        {
            //TODO check the connection is valid before allowing the connection
            //to add itself as a listener
            c = new Connection(s, m_node, m_connectionListener, m_inStreamFactory);
        } catch (IOException ioe)
        {
            s_logger.log(Level.WARNING, "Error creating connection", ioe);
            return;
        }
        
        ensureValidName(c);

        m_allNodes.add(c.getRemoteNode());

        ClientInitServerMessage init;
        synchronized (m_allNodes)
        {
            init = new ClientInitServerMessage(new HashSet<INode>(m_allNodes));
        }
        MessageHeader header = new MessageHeader(m_node, c.getRemoteNode(), init);
        c.send(header);

        NodeChangeServerMessage change = new NodeChangeServerMessage(true, c.getRemoteNode());
        broadcast(change);
        m_connections.add(c);

        s_logger.info("Connection added to:" + c.getRemoteNode());
        
        notifyConnectionsChanged(true, c.getRemoteNode());
    }

    private void removeConnection(Connection c)
    {
        NodeChangeServerMessage change = new NodeChangeServerMessage(false, c.getRemoteNode());
        m_allNodes.remove(c.getRemoteNode());
        broadcast(change);
        notifyConnectionsChanged(false, c.getRemoteNode());
        
        s_logger.info("Connection removed:" + c.getRemoteNode());
    }

    private void notifyListeners(MessageHeader msg)
    {
        Iterator<IMessageListener> iter = m_listeners.iterator();
        while (iter.hasNext())
        {
            IMessageListener listener = iter.next();
            listener.messageReceived(msg.getMessage(), msg.getFrom());
        }
    }

    public void addErrorListener(IMessengerErrorListener listener)
    {
        m_errorListeners.add(listener);
    }

    public void removeErrorListener(IMessengerErrorListener listener)
    {
        m_errorListeners.remove(listener);
    }

    public void addConnectionChangeListener(IConnectionChangeListener listener)
    {
        m_connectionListeners.add(listener);
    }

    public void removeConnectionChangeListener(IConnectionChangeListener listener)
    {
        m_connectionListeners.remove(listener);
    }

    private void notifyConnectionsChanged(boolean added, INode node)
    {
        Iterator<IConnectionChangeListener> iter = m_connectionListeners.iterator();
        while (iter.hasNext())
        {
            if (added)
            {
                iter.next().connectionAdded(node);
            } else
            {
                iter.next().connectionRemoved(node);
            }

        }
    }

    public void setAcceptNewConnections(boolean accept)
    {
        m_acceptNewConnection = accept;
    }

    public boolean isAcceptNewConnections()
    {
        return m_acceptNewConnection;
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
            while (!m_shutdown)
            {
                try
                {
                    Socket s = m_socket.accept();
                    
                    s_logger.info("Socket opened from:" + s.getRemoteSocketAddress());
                    
                    if (m_acceptNewConnection)
                    {
                        addConnection(s);
                    } else
                    {
                        s_logger.info("Not accepting connections, close socket for:" + s.getRemoteSocketAddress());
                        s.close();
                    }

                } catch (IOException e)
                {
                    //accept throws an exception when we shutdown
                    if(!m_shutdown)
                        e.printStackTrace();
                }
            }
        }
    }

    private IConnectionListener m_connectionListener = new IConnectionListener()
    {
        public void messageReceived(Serializable message, Connection connection)
        {
            ServerMessenger.this.messageReceived((MessageHeader) message);
        }

        public void fatalError(Exception error, Connection connection, List unsent)
        {
            //notify other nodes
            removeConnection(connection);

            //nofity this node
            Iterator<IMessengerErrorListener> iter = m_errorListeners.iterator();
            while (iter.hasNext())
            {
                IMessengerErrorListener errorListener = iter.next();
                errorListener.connectionLost(connection.getRemoteNode(), error, unsent);
            }
        }
    };

    public boolean isServer()
    {
        return true;
    }

    public void removeConnection(INode node)
    {
        if(node.equals(m_node))
            throw new IllegalArgumentException("Cant remove ourself!");
        
        for(Connection c : m_connections)
        {
            if(c.getRemoteNode().equals(node))
            {
                //if we caused it to e shutdown, then send the fatal error
                if(c.shutDown())
                {
                    m_connectionListener.fatalError(new Exception("Connection manually closed"), c, Collections.<MessageHeader>emptyList());
                }
                
            
                return;
            }
                
        }

    }

    public INode getServerNode()
    {
        return m_node;
    }

}
