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
 * Connection.java
 * 
 * 
 * This is the code that handles writing and reading objects over the socket.
 * Each connection handles the threads and communications for 1 socket to 1 remote party.
 * 
 * Connections write objects in the order that they are sent, and read them in the order that
 * they arrive, but since each object is processed in a seperate thread, it
 * is possible for messages to arrive out of sequence.
 * 
 * To fix this, an OrderedMessage marker interface was introduced.  An OrderedMessage will
 * not be processed until the previous OrderedMessage read on this connection
 * has finished processing.
 * 
 * Created on December 11, 2001, 8:23 PM
 */

package games.strategy.net;

import games.strategy.thread.ThreadPool;

import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * @author Sean Bridges
 * @version 1.0
 * 
 * Simple class to handle network connections.
 */
class Connection
{
    static final ThreadPool s_threadPool = new ThreadPool(10, "Connection Thread Pool");

    private Socket m_socket;
    private ObjectOutputStream m_out;
    private ObjectInputStream m_in;
    private boolean m_shutdown = false;
    private IConnectionListener m_listener;
    private INode m_localNode;
    private INode m_remoteNode;
    private Thread m_reader;
    private Thread m_writer;
    //all adding and removing from this list must be synchronized
    //on the list object
    private final List m_waitingToBeSent = Collections.synchronizedList(new LinkedList());
    private IObjectStreamFactory m_objectStreamFactory;
    private final Object m_lock = new Object();

    public Connection(Socket s, INode ident, IConnectionListener listener, IObjectStreamFactory fact) throws IOException
    {
        m_objectStreamFactory = fact;
        init(s, ident, listener);
    }

    /**
     * Creates new Connection s must be open.
     */
    public Connection(Socket s, INode ident, IConnectionListener listener) throws IOException
    {
        m_objectStreamFactory = new DefaultObjectStreamFactory();
        init(s, ident, listener);
    }

    private void init(Socket s, INode ident, IConnectionListener listener) throws IOException
    {
        m_socket = s;
        m_localNode = ident;
        m_listener = listener;

        //create the output
        BufferedOutputStream bufferedOut = new BufferedOutputStream(m_socket.getOutputStream());
        m_out = m_objectStreamFactory.create(bufferedOut);

        //write out our identity
        m_out.writeObject(m_localNode);
        m_out.flush();

        //create the input
        BufferedInputStream bufferedIn = new BufferedInputStream(m_socket.getInputStream());
        m_in = m_objectStreamFactory.create(bufferedIn);

        //read the remote connections identity
        try
        {
            m_remoteNode = (INode) m_in.readObject();
        } catch (ClassNotFoundException cnfe)
        {
            //should never happen
            cnfe.printStackTrace();
            throw new IllegalStateException("INode class not found");
        }

        m_reader = new Thread(new Reader(), "ConnectionReader for " + m_localNode.getName());
      
        m_reader.start();

        m_writer = new Thread(new Writer(), "ConnectionWriter for" + m_localNode.getName());
        m_writer.start();
    }

    /**
     * Blocks until no more data remains to be written or the socket is shutdown.
     */
    public void flush()
    {
        if (m_shutdown)
            return;

        synchronized (m_lock)
        {
            while (!m_shutdown && !m_waitingToBeSent.isEmpty())
            {
                try
                {
                    m_lock.wait();
                } catch (InterruptedException ie)
                {
                }
            }
        }
    }
    
    public INode getLocalNode()
    {
        return m_localNode;
    }

    public INode getRemoteNode()
    {
        return m_remoteNode;
    }

    public void send(MessageHeader msg)
    {
        m_waitingToBeSent.add(msg);
        synchronized(m_lock)
        {
            m_lock.notifyAll();
        }
    }

    public void shutDown()
    {
        synchronized (m_lock)
        {
            if (!m_shutdown)
            {
                m_shutdown = true;
                try
                {
                    m_socket.close();
                    m_lock.notifyAll();

                } catch (Exception e)
                {
                    System.err.println("Exception shutting down");
                    e.printStackTrace();
                }
            }
        }
    }
    public boolean isConnected()
    {
        return !m_shutdown;
    }

    private void messageReceived(Serializable obj)
    {
        if (obj != null)
            m_listener.messageReceived(obj, this);
    }

    class Writer implements Runnable
    {
        public void run()
        {
            while (!m_shutdown)
            {
                synchronized (m_lock)
                {
                    if (!m_waitingToBeSent.isEmpty())
                    {
                        Serializable next = (Serializable) m_waitingToBeSent.get(0);
                        write(next);

                        m_waitingToBeSent.remove(0);

                        /**
                         * flush() may need to be wokren up
                         */
                        if (m_waitingToBeSent.isEmpty())
                            m_lock.notifyAll();
                    } else
                    {
                        try
                        {
                            //the stream keeps a memory of objects that have been written to the
                            //stream, preventing them from being gc'd. reset stream when we
                            //are out of things to send
                            try
                            {
                                m_out.reset();
                            } catch (IOException ioe)
                            {
                                ioe.printStackTrace();
                            }
                            m_lock.wait();
                        } catch (InterruptedException ie)
                        {
                        }
                    }
                }

            }
        }

        private void write(Serializable next)
        {
            if (!m_shutdown)
            {
                try
                {

                    m_out.writeObject(next);
                    m_out.flush();
                } catch (IOException ioe)
                {
                    if (!m_shutdown)
                    {
                        ioe.printStackTrace();
                        shutDown();
                        List unsent = new ArrayList(m_waitingToBeSent);
                        unsent.add(next);
                        m_listener.fatalError(ioe, Connection.this, unsent);
                    }
                }
            }
        }
    }

    class Reader implements Runnable
    {
        private OrderedMessageHandler m_orderedMessageHandler = null; 
        
        public void run()
        {
            while (!m_shutdown)
            {
                try
                {
                    final MessageHeader msg = (MessageHeader) m_in.readObject();

                    Runnable r = new Runnable()
                    {
                        public void run()
                        {
                            messageReceived(msg);
                        }
                    };
                    
                    //only one ordered message can be processed at a time
                    //delay processing until the last ordered message is done
                    if(msg.getMessage() instanceof OrderedMessage)
                    {
                        if(m_orderedMessageHandler != null)
                            m_orderedMessageHandler.waitTillDone();
                        m_orderedMessageHandler = new OrderedMessageHandler(r);
                        s_threadPool.runTask(m_orderedMessageHandler);
                    }
                    else
                        s_threadPool.runTask(r);
              
                    //allow the message to be processed
                    Thread.yield();

                } catch (ClassNotFoundException cnfe)
                {
                    cnfe.printStackTrace();
                } catch (IOException ioe)
                {
                    if (!m_shutdown)
                    {
                        //these normally occur when the socket is closed
                        //ignore
                        if (!(ioe instanceof EOFException))
                            ioe.printStackTrace();
                        shutDown();
                        List unsent = new ArrayList(m_waitingToBeSent);
                        m_listener.fatalError(ioe, Connection.this, unsent);
                    }
                }
            }
        }
    }
}


/**
 * Prevents multiple OrderedMessages from being run at the same time.
 * 
 *  
 */
class OrderedMessageHandler implements Runnable
{
    private boolean m_done = false;
    private final Object m_lock = new Object();
    private final Runnable m_runnable;
    
    OrderedMessageHandler(Runnable target)
    {
        m_runnable = target;
    }
    
    public void run()
    {
        try
        {
            m_runnable.run();
        }
        finally
        {
	        synchronized(m_lock)
	        {
	            m_done = true;
	            m_lock.notifyAll();
	        }
        }
    }
    
    public void waitTillDone()
    {
        
        synchronized(m_lock)
        {
            while(!m_done)
            {
                try
                {
                    m_lock.wait();
                } catch (InterruptedException e)
                {
                    //ignore
                }
            }
        }
    }
}
