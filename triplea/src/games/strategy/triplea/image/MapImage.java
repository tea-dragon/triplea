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
 * CountryImage.java
 *
 * Created on January 8, 2002, 9:15 PM
 */

package games.strategy.triplea.image;

import java.net.URL;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;

import games.strategy.util.PointFileReaderWriter;
import games.strategy.engine.data.*;
import games.strategy.triplea.attatchments.TerritoryAttatchment;
import games.strategy.util.*;

/**
 *
 * @author  Sean Bridges
 */
public class MapImage
{
  private static final String LOCATION = "countries/location.txt";
  private final static String LARGE_IMAGE_FILENAME = "images/largeMap.gif";
  private final static String SMALL_IMAGE_FILENAME = "images/smallMap.gif";

  private static MapImage s_instance;
  private static final ImageObserver s_observer = new NullImageObserver();


  public static synchronized MapImage getInstance()
  {
    if(s_instance == null)
    {
      s_instance = new MapImage();
    }
    return s_instance;
  }

  private static Image loadImage(String name)
  {
    Image img =  Toolkit.getDefaultToolkit().createImage(MapImage.class.getResource(name));
    MediaTracker tracker = new MediaTracker( new Panel());
    tracker.addImage(img,1 );
    try
    {
      tracker.waitForAll();
      return img;
    } catch(InterruptedException ie)
    {
      ie.printStackTrace();
      return loadImage(name);
    }
  }

  //Maps Country name -> Point
  private Map m_topCorners = new HashMap();
  private Image m_largeMapImage;
  private Image m_smallMapImage;
  private float m_smallLargeRatio;

  /** Creates a new instance of CountryImage */
    public MapImage()
  {
    initCorners();
    }


  public Image getSmallMapImage()
  {
    return m_smallMapImage;
  }

  public void loadMaps(GameData data)
  {
    loadMaps();
    initMaps(data);
  }

  public Image getLargeMapImage()
  {
    return m_largeMapImage;
  }

  private void loadMaps()
  {
    Image largeFromFile = loadImage(LARGE_IMAGE_FILENAME);
    Image smallFromFile = loadImage(SMALL_IMAGE_FILENAME);

    //create from a component to make screen drawing faster
    //if you create an image from a component then no operations
    //have to be done when drawing the image to the screen, just a simple
    //byte copy
    Frame frame = new Frame();
    frame.addNotify();
    m_largeMapImage = frame.createImage(largeFromFile.getWidth(s_observer), largeFromFile.getHeight(s_observer));
    m_smallMapImage = frame.createImage(smallFromFile.getWidth(s_observer), smallFromFile.getHeight(s_observer));

    frame.dispose();
    frame = null;

    m_largeMapImage.getGraphics().drawImage(largeFromFile, 0,0,s_observer);
    m_smallMapImage.getGraphics().drawImage(smallFromFile, 0,0,s_observer);


    largeFromFile = null;
    smallFromFile = null;
    System.gc();
  }

  private void initMaps(GameData data)
  {
    m_smallLargeRatio = ((float) m_largeMapImage.getHeight(s_observer)) / ((float) m_smallMapImage.getHeight(s_observer));

    Iterator territories = data.getMap().iterator();
    while(territories.hasNext())
    {
      Territory current = (Territory) territories.next();
      PlayerID id = current.getOwner();

      if(!current.isWater())
        setOwner(current, id);
    }
  }

  private void initCorners()
  {
    try
    {
      URL centers = MapImage.class.getResource(LOCATION);
      InputStream stream = centers.openStream();
      m_topCorners = new PointFileReaderWriter().readOneToOne(stream);
    } catch(IOException ioe)
    {
      System.err.println("Error reading " + LOCATION + "  file");
      ioe.printStackTrace();
      System.exit(0);
    }
  }

  public void setOwner(Territory territory, PlayerID id)
  {
    if(territory.isWater())
      return;

    BufferedImage country = TerritoryImageFactory.getInstance().getTerritoryImage(territory,id);
    String name = territory.getName();

    Point p = (Point) m_topCorners.get(name);
    if(p == null)
      throw new IllegalStateException("No top corner could be found for:" + name);

    m_largeMapImage.getGraphics().drawImage(country, p.x, p.y, s_observer);

    if (!territory.isWater())
    {
      TerritoryAttatchment ta = TerritoryAttatchment.get(territory);
      Graphics g = m_largeMapImage.getGraphics();
      FontMetrics fm = g.getFontMetrics();
      int x = p.x;
      int y = p.y;

//			if (ta.getProduction() > 0)
//				name += " (" + ta.getProduction() + ")";

      x += country.getWidth(s_observer) >> 1;
      y += country.getHeight(s_observer) >> 1;

      x -= fm.stringWidth(name) >> 1;
      y += fm.getAscent() >> 1;

      g.drawString(name, x, y);

      if (ta.getProduction() > 0)
      {
        String prod = new Integer(ta.getProduction()).toString();
        x = p.x + ((country.getWidth(s_observer) - fm.stringWidth(prod))>> 1);
        y += fm.getLeading() + fm.getAscent();
        g.drawString(prod, x, y);
      }
    }

    setOwnerOnSmallMap(territory, id, p);

  }

private void setOwnerOnSmallMap(Territory territory, PlayerID id, Point p)
{
    BufferedImage country = TerritoryImageFactory.getInstance().getTerritoryImageNoRelief(territory, id);

    int smallHeight = (int) (country.getHeight() / m_smallLargeRatio) + 3;
    int smallWidth = (int) (country.getWidth() / m_smallLargeRatio) + 3;

    Image small  = country.getScaledInstance(smallWidth, smallHeight , Image.SCALE_FAST);

    Point smallPoint = new Point( (int)( p.x / m_smallLargeRatio), (int) (p.y / m_smallLargeRatio));
    m_smallMapImage.getGraphics().drawImage(small, smallPoint.x, smallPoint.y,  s_observer);
}

}
