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

/*
 * MapPanel.java
 * 
 * Created on November 5, 2001, 1:54 PM
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.*;
import games.strategy.engine.data.events.*;
import games.strategy.triplea.Constants;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.screen.*;
import games.strategy.triplea.util.*;
import games.strategy.ui.*;
import games.strategy.ui.Util;
import games.strategy.util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.*;

import javax.swing.SwingUtilities;

/**
 * Responsible for drawing the large map and keeping it updated.
 * 
 * @author Sean Bridges
 */
public class MapPanel extends ImageScrollerLargeView
{
    private static Logger s_logger = Logger.getLogger(MapPanel.class.getName());
    
    private ListenerList<MapSelectionListener> m_mapSelectionListeners = new ListenerList<MapSelectionListener>();
    private ListenerList<UnitSelectionListener> m_unitSelectionListeners = new ListenerList<UnitSelectionListener>();
    private ListenerList<MouseOverUnitListener> m_mouseOverUnitsListeners = new ListenerList<MouseOverUnitListener>();    

    private GameData m_data;
    private Territory m_currentTerritory; //the territory that the mouse is
    // currently over
    //could be null
    private MapPanelSmallView m_smallView;
    
    //units the mouse is currently over
    private Tuple<Territory, List<Unit>> m_currentUnits;

    private SmallMapImageManager m_smallMapImageManager;
    
    //keep a reference to the images from the last paint to
    //prevent them from being gcd
    private List<Object> m_images = new ArrayList<Object>();
    
    private RouteDescription m_routeDescription;

    private final TileManager m_tileManager;
    
    private final BackgroundDrawer m_backgroundDrawer;
    
    private BufferedImage m_mouseShadowImage = null;
    private final UIContext m_uiContext;
    

    private List<Unit> m_highlightUnits;
    
    /** Creates new MapPanel */
    public MapPanel(GameData data, MapPanelSmallView smallView, UIContext uiContext, ImageScrollModel model) throws IOException
    {
        super(uiContext.getMapData().getMapDimensions(), model);

        m_uiContext = uiContext;
        m_backgroundDrawer = new BackgroundDrawer(this);
        m_tileManager = new TileManager(m_uiContext);
        
        Thread t = new Thread(m_backgroundDrawer, "Map panel background drawer");
        t.setDaemon(true);
        t.start();
        
        setDoubleBuffered(false);
       

        m_smallView = smallView;
        m_smallMapImageManager = new SmallMapImageManager(smallView, m_uiContext.getMapImage().getSmallMapImage(),  m_tileManager);
        
        setGameData(data);

        this.addMouseListener(MOUSE_LISTENER);
        this.addMouseMotionListener(MOUSE_MOTION_LISTENER);
        
        this.addScrollListener(new ScrollListener()
        {

            public void scrolled(int x, int y)
            {
                SwingUtilities.invokeLater(new Runnable(){
                
                    public void run()
                    {
                        repaint();
                    }
                
                });
                
            }
            
        });
        m_tileManager.createTiles(new Rectangle(m_uiContext.getMapData().getMapDimensions()), data, m_uiContext.getMapData());
        m_tileManager.resetTiles(data, uiContext.getMapData());
        
        m_uiContext.addActive(new Active()
        {
        
            public void deactivate()
            {
                //super.deactivate
                MapPanel.this.deactivate();
            }
        
        });
        
        
       
    } 
     
    GameData getData()
    {
        return m_data;
    }

    // Beagle Code used to chnage map skin
    public void changeImage(Dimension newDimensions)
    {
        super.setDimensions(newDimensions);
        m_tileManager.createTiles(new Rectangle(newDimensions), m_data, m_uiContext.getMapData());
        m_tileManager.resetTiles(m_data, m_uiContext.getMapData());
    }
    
    

    @Override
    public Dimension getPreferredSize()
    {
       return getImageDimensions();
    }


    @Override
    public Dimension getMinimumSize()
    {
       return new Dimension(200,200);
    }

    public boolean isShowing(Territory territory)
    {

        Point territoryCenter = m_uiContext.getMapData().getCenter(territory);

        Rectangle screenBounds = new Rectangle(super.getXOffset(), super.getYOffset(), super.getWidth(), super.getHeight());
        return screenBounds.contains(territoryCenter);

    }

    /**
     * the units must all be in the same stack on the map, and exist in the given territory.
     * call with an null args
     */
    public void setUnitHighlight(List<Unit> units, Territory territory)
    {
        m_highlightUnits =units;
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                repaint();
            }
        
        });
        
    }
    
    public void centerOn(Territory territory)
    {
        if (territory == null)
            return;

        Point p = m_uiContext.getMapData().getCenter(territory);

        //when centering dont want the map to wrap around,
        //eg if centering on hawaii
        super.setTopLeft(p.x - (getWidth() / 2), p.y - (getHeight() / 2));
    }

    public void setRoute(Route route)
    {
        setRoute(route, null, null);
    }

    /**
     * Set the route, could be null.
     */
    public void setRoute(Route route, Point start, Point end)
    {
        if (route == null)
        {
            m_routeDescription = null;
            SwingUtilities.invokeLater(new Runnable()
            {
            
                public void run()
                {
                    repaint();
                }
            
            });
            
            return;
        }
        RouteDescription newVal = new RouteDescription(route, start, end);
        if (m_routeDescription != null && m_routeDescription.equals(newVal))
        {
            return;
        }

        m_routeDescription = newVal;
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                repaint();
            }
        
        });

    }

    public void addMapSelectionListener(MapSelectionListener listener)
    {
        m_mapSelectionListeners.add(listener);
    }

    public void removeMapSelectionListener(MapSelectionListener listener)
    {
        m_mapSelectionListeners.remove(listener);
    }

    public void addMouseOverUnitListener(MouseOverUnitListener listener)
    {
        m_mouseOverUnitsListeners.add(listener);
    }

    public void removeMouseOverUnitListener(MouseOverUnitListener listener)
    {
        m_mouseOverUnitsListeners.remove(listener);
    }
    
    
    private void notifyTerritorySelected(Territory t, MouseEvent me)
    {

        Iterator<MapSelectionListener> iter = m_mapSelectionListeners.iterator();

        while (iter.hasNext())
        {
            MapSelectionListener msl = iter.next();
            msl.territorySelected(t, me);
        }
    }

    private void notifyMouseMoved(Territory t, MouseEvent me)
    {

        Iterator<MapSelectionListener> iter = m_mapSelectionListeners.iterator();

        while (iter.hasNext())
        {
            MapSelectionListener msl = iter.next();
            msl.mouseMoved(t, me);
        }
    }

    private void notifyMouseEntered(Territory t)
    {

        Iterator<MapSelectionListener> iter = m_mapSelectionListeners.iterator();

        while (iter.hasNext())
        {
            MapSelectionListener msl = iter.next();
            msl.mouseEntered(t);
        }
    }

    
    public void addUnitSelectionListener(UnitSelectionListener listener)
    {
        m_unitSelectionListeners.add(listener);
    }
    
    public void removeUnitSelectionListener(UnitSelectionListener listener)
    {
        m_unitSelectionListeners.remove(listener);
    }

    private void notifyUnitSelected(List<Unit> units, Territory t, MouseEvent me)
    {
        for(UnitSelectionListener listener : m_unitSelectionListeners)
        {
            listener.unitsSelected(units,t, me);
        }
    }
    
    private void notifyMouseEnterUnit(List<Unit> units, Territory t, MouseEvent me)
    {
        for(MouseOverUnitListener listener : m_mouseOverUnitsListeners)
        {
            listener.mouseEnter(units,t, me);
        }
    }
    
    
    private Territory getTerritory(int x, int y)
    {
        String name = m_uiContext.getMapData().getTerritoryAt(normalizeX(x), y);
        if (name == null)
            return null;
        return m_data.getMap().getTerritory(name);
    }

    private int normalizeX(int x)
    {
        if(!m_uiContext.getMapData().scrollWrapX())
            return x;
        
        int imageWidth = (int) getImageDimensions().getWidth();
        if (x < 0)
            x += imageWidth;
        else if (x > imageWidth)
            x -= imageWidth;
        return x;
    }

    public void resetMap()
    {
        
       m_tileManager.resetTiles(m_data, m_uiContext.getMapData());
       
       SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                repaint();
            }
        });
       
       m_smallMapImageManager.update(m_data, m_uiContext.getMapData());
       
    }

    private MouseListener MOUSE_LISTENER = new MouseAdapter()
    {
        /**
         * Invoked when the mouse exits a component.
         */
        public void mouseExited(MouseEvent e)
        {
            if(unitsChanged(null))
            {
                m_currentUnits = null;
                notifyMouseEnterUnit(Collections.<Unit>emptyList(), getTerritory(e.getX(),e.getY()), e );
            }
        }
        
        public void mouseReleased(MouseEvent e)
        {
            int x = normalizeX(e.getX() + getXOffset());
            int y =  e.getY() + getYOffset();            
            
            Territory terr = getTerritory(x,y);
            if (terr != null)
                notifyTerritorySelected(terr, e);
            
            if(!m_unitSelectionListeners.isEmpty())
            {
                Tuple<Territory, List<Unit>> tuple = m_tileManager.getUnitsAtPoint(x,y, m_data);
                
                if(tuple == null)
                    tuple = new Tuple<Territory, List<Unit>>(getTerritory(x,y), new ArrayList<Unit>(0) );
                
                notifyUnitSelected(tuple.getSecond(), tuple.getFirst(), e );
            }

            
        }

    };
    

    

    private final MouseMotionListener MOUSE_MOTION_LISTENER = new MouseMotionAdapter()
    {

        public void mouseMoved(MouseEvent e)
        {

            Territory terr = getTerritory(e.getX() + getXOffset(), e.getY() + getYOffset());
            //we can use == here since they will be the same object.
            //dont use .equals since we have nulls
            if (terr != m_currentTerritory)
            {
                m_currentTerritory = terr;
                notifyMouseEntered(terr);
            }
            notifyMouseMoved(terr, e);

            
            int x = normalizeX(e.getX() + getXOffset());
            int y =  e.getY() + getYOffset();     
            
            Tuple<Territory, List<Unit>> tuple = m_tileManager.getUnitsAtPoint(x,y, m_data);
            
            if(unitsChanged(tuple))
            {
                m_currentUnits = tuple;
                if(tuple == null)
                    notifyMouseEnterUnit(Collections.<Unit>emptyList(), getTerritory(x,y), e );
                else
                    notifyMouseEnterUnit(tuple.getSecond(), tuple.getFirst(), e );
            }
                
            
            
        }
    };
    
    private boolean unitsChanged( Tuple<Territory, List<Unit>> newUnits)
    {
        //both are null
        if(newUnits == m_currentUnits)
            return false;
        
        //one is null
        if(newUnits == null || m_currentUnits == null)
            return true;

        
        if(!newUnits.getFirst().equals(m_currentUnits.getFirst()))
            return true;
        
        return !games.strategy.util.Util.equals(newUnits.getSecond(), m_currentUnits.getSecond());
        
    }
    

    public void updateCounties(Collection<Territory> countries)
    {
        m_tileManager.updateTerritories(countries, m_data, m_uiContext.getMapData());
        m_smallMapImageManager.update(m_data, m_uiContext.getMapData());
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                m_smallView.repaint();
                repaint();        
            }
        
        });
    }

    public void setGameData(GameData data)
    {
        //clean up any old listeners
        if (m_data != null)
        {
            m_data.removeTerritoryListener(TERRITORY_LISTENER);
            m_data.removeDataChangeListener(TECH_UPDATE_LISTENER);
        }

        m_data = data;
        m_data.addTerritoryListener(TERRITORY_LISTENER);
        
        m_data.addDataChangeListener(TECH_UPDATE_LISTENER);
        
        //stop painting in the background
        m_backgroundDrawer.setTiles(Collections.<Tile>emptySet());
        
        m_tileManager.resetTiles(m_data, m_uiContext.getMapData());

    }

    private final TerritoryListener TERRITORY_LISTENER = new TerritoryListener()
    {

        public void unitsChanged(Territory territory)
        {
            updateCounties(Collections.singleton(territory));
            SwingUtilities.invokeLater(new Runnable()
            {
            
                public void run()
                {
                    repaint();
                }
            
            });
            
        }

        public void ownerChanged(Territory territory)
        {
            m_smallMapImageManager.updateTerritoryOwner(territory, m_data, m_uiContext.getMapData());
            updateCounties(Collections.singleton(territory));
            SwingUtilities.invokeLater(new Runnable()
            {
            
                public void run()
                {
                    repaint();
                }
            
            });
        }
    };


    private final GameDataChangeListener TECH_UPDATE_LISTENER = new GameDataChangeListener()
    {

        public void gameDataChanged(Change aChange)
        {

            //find the players with tech changes
            Set<PlayerID> playersWithTechChange = new HashSet<PlayerID>();
            getPlayersWithTechChanges(aChange, playersWithTechChange);

            if (playersWithTechChange.isEmpty())
                return;

            m_tileManager.resetTiles(m_data, m_uiContext.getMapData());
            SwingUtilities.invokeLater(new Runnable()
            {
            
                public void run()
                {
                    repaint();
                }
            
            });
         
        }

       
        private void getPlayersWithTechChanges(Change aChange, Set<PlayerID> players)
        {

            if (aChange instanceof CompositeChange)
            {
                CompositeChange composite = (CompositeChange) aChange;
                Iterator iter = composite.getChanges().iterator();
                while (iter.hasNext())
                {
                    Change item = (Change) iter.next();
                    getPlayersWithTechChanges(item, players);

                }
            } else
            {
                if (aChange instanceof ChangeAttachmentChange)
                {
                    ChangeAttachmentChange changeAttatchment = (ChangeAttachmentChange) aChange;
                    if (changeAttatchment.getAttatchmentName().equals(Constants.TECH_ATTATCHMENT_NAME))
                    {
                        players.add((PlayerID) changeAttatchment.getAttatchedTo());
                    }
                }

            }
        }

    };

    

        
    public void paint(Graphics g)
    {
        super.paint(g);
        
        int x = m_model.getX();
        int y = m_model.getY();
        
        List<Tile> images = new ArrayList<Tile>();
        List<Tile> undrawnTiles = new ArrayList<Tile>();
        
        Stopwatch stopWatch = new Stopwatch(s_logger, Level.FINER, "Paint");

        //make sure we use the same data for the entire paint
        final GameData data = m_data;
        
        //if the map fits on screen, dont draw any overlap
        boolean mapFitsOnScreen = mapFitsOnScreen();
        
        //handle wrapping off the screen to the left
        if(!mapFitsOnScreen && x < 0  && m_uiContext.getMapData().scrollWrapX())
        {
            Rectangle leftBounds = new Rectangle(m_dimensions.width + x, y, -x, getHeight());
            drawTiles(g, images, data, leftBounds,0, undrawnTiles);
        }
        
        //handle the non overlap
	    Rectangle mainBounds = new Rectangle(x, y, getWidth(), getHeight());
	    drawTiles(g, images, data, mainBounds,0, undrawnTiles);
        
        double leftOverlap = x + getWidth() - m_dimensions.getWidth();
        //handle wrapping off the screen to the right
        if(!mapFitsOnScreen && leftOverlap > 0 && m_uiContext.getMapData().scrollWrapX())
        {
            Rectangle rightBounds = new Rectangle(0 , y, (int) leftOverlap, getHeight());
            drawTiles(g, images, data, rightBounds, leftOverlap, undrawnTiles);
        }

        
        MapRouteDrawer.drawRoute((Graphics2D) g, m_routeDescription, this, m_uiContext.getMapData());
        
        if(m_routeDescription != null && m_mouseShadowImage != null && m_routeDescription.getEnd() != null)
        {
            ((Graphics2D) g).drawImage(m_mouseShadowImage, (int)  m_routeDescription.getEnd().getX() - getXOffset(), (int) m_routeDescription.getEnd().getY() - getYOffset(), this);
        }
        
        //used to keep strong references to what is on the screen so it wont be garbage collected
        //other references to the images are week references
        m_images.clear();
        m_images.addAll(images);

        
        if(m_highlightUnits != null)
        {
            Rectangle r = m_tileManager.getUnitRect(m_highlightUnits, m_data );
            Unit first = m_highlightUnits.get(0);
            
            Image highlight = m_uiContext.getUnitImageFactory().getHighlightImage(first.getType(), first.getOwner(), m_data, first.getHits() != 0);
            ((Graphics2D) g).drawImage(highlight, (int) r.getX() - getXOffset(), (int) r.getY() - getYOffset(), this );
            
//            ((Graphics2D) g).setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
//            g.setColor(new Color(200,200,200, 80) );
//            ((Graphics2D) g).fillRoundRect(m_unitBox.x - getXOffset() , m_unitBox.y - getYOffset(), m_unitBox.width, m_unitBox.height, 15,15);
        }
            
                
        //draw the tiles nearest us first
        //then draw farther away
        updateUndrawnTiles(undrawnTiles, 30, true);
        updateUndrawnTiles(undrawnTiles, 257, true);
        //when we are this far away, dont force the tiles to stay in memroy
        updateUndrawnTiles(undrawnTiles, 513, false);
        updateUndrawnTiles(undrawnTiles, 767, false);
        
        
        m_backgroundDrawer.setTiles(undrawnTiles);
        
        stopWatch.done();
        
    }

    boolean mapFitsOnScreen()
    {
        return m_model.getMaxWidth() < getWidth();
    }

   
    /**
     * If we have nothing left undrawn, draw the tiles within preDrawMargin of us, optionally
     * forcing the tiles to remain in memory. 
     */
    private void updateUndrawnTiles(List<Tile> undrawnTiles, int preDrawMargin, boolean forceInMemory)
    {
        //draw tiles near us if we have nothing left to draw
        //that way when we scroll slowly we wont notice a glitch
        if(undrawnTiles.isEmpty())
        {
            
            Rectangle extendedBounds = new Rectangle( Math.max(m_model.getX() -preDrawMargin, 0),Math.max(m_model.getY() -preDrawMargin, 0), getWidth() + (2 * preDrawMargin),  getHeight() + (2 * preDrawMargin));
            Iterator tiles = m_tileManager.getTiles(extendedBounds).iterator();

            while (tiles.hasNext())
            {
                Tile tile = (Tile) tiles.next();
                if(tile.isDirty())
                {
                    undrawnTiles.add(tile);
                }
                else if(forceInMemory)
                {
                    m_images.add(tile.getRawImage());
                }
            }
            
            
        }
    }

    private void drawTiles(Graphics g, List<Tile> images, final GameData data, Rectangle bounds, double overlap, List<Tile> undrawn)
    {
        List tileList = m_tileManager.getTiles(bounds);
        Iterator tiles = tileList.iterator();

        if(overlap != 0)
        {
            bounds.translate((int) (overlap - getWidth()), 0);    
        }
        
        while (tiles.hasNext())
        {
            Image img = null;
            Tile tile = (Tile) tiles.next();

            synchronized(tile.getMutex())
            {
                if(tile.isDirty())
                {
                    //take what we can get to avoid screen flicker
                    undrawn.add(tile);
                    img = tile.getRawImage();
                    
                }
                else
                {
    	            img = tile.getImage(data, m_uiContext.getMapData());
    	            images.add(tile);
                }
                if(img != null)
                    g.drawImage(img, tile.getBounds().x -bounds.x, tile.getBounds().y - m_model.getY(), this);
            }
            
          
        }
    }

    public Image getTerritoryImage(Territory territory)
    {
        return m_tileManager.createTerritoryImage(territory, m_data,m_uiContext.getMapData());
    }

    public Image getTerritoryImage(Territory territory, Territory focusOn)
    {
        return m_tileManager.createTerritoryImage(territory, focusOn, m_data,m_uiContext.getMapData());
    }
    


    /**
     * 
     */
    public void initSmallMap()
    {
        Iterator territories = m_data.getMap().getTerritories().iterator();
        
        while (territories.hasNext())
        {
            Territory territory = (Territory) territories.next();
            m_smallMapImageManager.updateTerritoryOwner(territory, m_data, m_uiContext.getMapData());
            
        }
        
        m_smallMapImageManager.update(m_data, m_uiContext.getMapData());
        
    }  
    
    public void setMouseShadowUnits(Collection<Unit> units)
    {
        if(units == null || units.isEmpty())
        {
            m_mouseShadowImage = null;
            SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            repaint();
                        }
                    
                    });
            return;
        }
        
        Set<UnitCategory> categories =  UnitSeperator.categorize(units);
        
        final int icon_width =  m_uiContext.getUnitImageFactory().getUnitImageWidth();
        
        final int xSpace = 5;
         
        BufferedImage img = Util.createImage( categories.size() * (xSpace + icon_width), UnitImageFactory.UNIT_ICON_HEIGHT, true); 
        Graphics2D g = (Graphics2D) img.getGraphics();
        
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f) );
        
        Rectangle bounds = new Rectangle(0,0,0,0);
        
        
        int i = 0;
        for(UnitCategory category : categories)
        {
            Point place = new Point( i * (icon_width + xSpace), 0);
            UnitsDrawer drawer = new UnitsDrawer(category.getUnits().size(), category.getType().getName(), 
                    category.getOwner().getName(), place,category.getDamaged(), false, "", m_uiContext );
            drawer.draw(bounds, m_data, g, m_uiContext.getMapData());
            i++;
        }
        m_mouseShadowImage = img;
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                repaint();
            }
        
        });
        g.dispose();
    }
    
    public void setTerritoryOverlay(Territory territory, Color color, int alpha)
    {
        m_tileManager.setTerritoryOverlay(territory, color, alpha, m_data, m_uiContext.getMapData() );
    }

    public void clearTerritoryOverlay(Territory territory)
    {
        m_tileManager.clearTerritoryOverlay(territory, m_data, m_uiContext.getMapData());
    }

    public UIContext getUIContext()
    {
        return m_uiContext;
    }
}

class RouteDescription
{

    private final Route m_route;
    private final Point m_start;
    private final Point m_end;

    public RouteDescription(Route route, Point start, Point end)
    {
        m_route = route;
        m_start = start;
        m_end = end;
    }

    public boolean equals(Object o)
    {
        if (o == null)
            return false;
        if (o == this)
            return true;
        RouteDescription other = (RouteDescription) o;

        if (m_start == null && other.m_start != null || other.m_start == null && m_start != null
                || (m_start != other.m_start && !m_start.equals(other.m_start)))
            return false;

        if (m_route == null && other.m_route != null || other.m_route == null && m_route != null
                || (m_route != other.m_route && !m_route.equals(other.m_route)))
            return false;

        if (m_end == null && other.m_end != null || other.m_end == null && m_end != null)
            return false;

        //we dont want to be updating for every small change,
        //if the end points are close enough, they are close enough
        if (other.m_end == null && this.m_end != null)
            return false;
        if (other.m_end != null && this.m_end == null)
            return false;

        int xDiff = m_end.x - other.m_end.x;
        xDiff *= xDiff;
        int yDiff = m_end.y - other.m_end.y;
        yDiff *= yDiff;
        int endDiff = (int) Math.sqrt(xDiff + yDiff);

        return endDiff < 6;

    }

    public Route getRoute()
    {
        return m_route;
    }

    public Point getStart()
    {
        return m_start;
    }

    public Point getEnd()
    {
        return m_end;
    }
    
  
}

class BackgroundDrawer implements Runnable
{
    private final LinkedBlockingQueue<Tile> m_tiles = new LinkedBlockingQueue<Tile>();
    
    //use a weak reference, if we see the panel is gc'd, then we can stop this thread
    private final WeakReference<MapPanel> m_mapPanelRef;
    
    BackgroundDrawer(MapPanel panel)
    {
        m_mapPanelRef = new WeakReference<MapPanel>(panel);
    }
    
    public void stop()
    {        
        m_mapPanelRef.clear();
        m_tiles.clear();
        //the thread will eventually wake up and notice we are done
        
    }
    
    public void setTiles(Collection<Tile> aCollection) 
    {
        m_tiles.clear();
        m_tiles.addAll(aCollection);
    }
    
    public void run()
    {
        while(m_mapPanelRef.get() != null)
        {
            Tile tile;
            try
            {
                tile = m_tiles.poll(2000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e)
            {
               continue;
            }

            if(tile == null)
                continue;
            
            final MapPanel mapPanel = m_mapPanelRef.get();
            if(mapPanel == null)
            {
                continue;
            }
            
            GameData data = mapPanel.getData();
            tile.getImage(data, mapPanel.getUIContext().getMapData());
            
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               { 
                   mapPanel.repaint();
               }
            });
            
            
        }
    }
    
    
    
}
