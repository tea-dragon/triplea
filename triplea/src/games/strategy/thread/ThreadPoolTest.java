/*
 * ThreadPoolTest.java
 *
 * Created on January 25, 2002, 3:34 PM
 */

package games.strategy.thread;

import junit.framework.*;

import java.util.*;

/**
 *
 * @author  Sean Bridges
 */
public class ThreadPoolTest extends TestCase
{

  /** Creates a new instance of ThreadPoolTest */
    public ThreadPoolTest(String s)
  {
    super(s);
    }

  public static Test suite()
  {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(ThreadPoolTest.class);
    return suite;
  }

  public void testRunOneTask()
  {
    ThreadPool pool = new ThreadPool(50, "test");
    Task task = new Task();
    pool.runTask(task);
    pool.waitForAll();
    assertTrue(task.isDone());
  }

  public void testSingleThread()
  {
    ThreadPool pool = new ThreadPool(1, "test");
    Collection tasks = new ArrayList();

    for(int i = 0; i < 30; i++)
    {
      Runnable task = new Task();
      tasks.add(task);
      pool.runTask(task);

    }

    pool.waitForAll();

    Iterator iter = tasks.iterator();
    while(iter.hasNext())
    {
      assertTrue( ((Task) iter.next()).isDone());
    }
    pool.shutDown();
  }


  public void testSimple()
  {
    ThreadPool pool = new ThreadPool(5, "test");
    Collection tasks = new ArrayList();

    for(int i = 0; i < 30; i++)
    {
      Runnable task = new Task();
      tasks.add(task);
      pool.runTask(task);
    }

    pool.waitForAll();

    Iterator iter = tasks.iterator();
    while(iter.hasNext())
    {
      assertTrue( ((Task) iter.next()).isDone());
    }
    pool.shutDown();
  }



  public void testBlocked()
  {
    Collection threads = new ArrayList();

    for(int j = 0; j < 20; j++)
    {
      Runnable r = new Runnable() {

        public void run()
        {
          threadTestBlock();
        }
      };
      Thread t = new Thread(r);
      threads.add(t);
      t.start();
    }


    Iterator iter = threads.iterator();
    while(iter.hasNext())
    {
      try {
        ((Thread) iter.next()).join();
      }
      catch (InterruptedException ex) {
        ex.printStackTrace();
      }
    }
  }

  private void threadTestBlock()
  {
    ThreadPool pool = new ThreadPool(10, "test");

    ArrayList blockedTasks = new ArrayList();
    for(int i = 0; i < 50; i++)
    {
      BlockedTask task = new BlockedTask();
      blockedTasks.add(task);
      pool.runTask(task);
    }

    pool.waitForAll();

    Iterator iter = blockedTasks.iterator();
    while(iter.hasNext())
    {
      BlockedTask task = (BlockedTask) iter.next();
      assertTrue(task.isDone());
    }

    pool.shutDown();

  }



}

class Task implements Runnable
{
  private boolean done = false;

  public synchronized boolean isDone()
  {
    return done;
  }

  public void run()
  {
    done = true;
  }
}

class BlockedTask extends Task
{
  public void run()
  {
    synchronized(this)
    {
      try
      {

        wait(400);
      }
      catch(InterruptedException ie) {}
      super.run();
    }
  }

}
