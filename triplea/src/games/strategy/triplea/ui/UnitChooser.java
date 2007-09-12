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
* UnitChooser.java
*
* Created on December 3, 2001, 7:32 PM
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.*;
import games.strategy.util.*;
import games.strategy.triplea.util.*;
import games.strategy.triplea.TripleAUnit;
import games.strategy.ui.*;
import games.strategy.util.IntegerMap;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class UnitChooser extends JPanel
{
  private static boolean s_imagesLoaded;
  private static Icon s_left;
  private static Icon s_right;

  private synchronized static void loadImages()
  {
      if (s_imagesLoaded)
          return;

      s_left = new ImageIcon(ScrollableTextField.class.getResource("images/left.gif"));
      s_right = new ImageIcon(ScrollableTextField.class.getResource("images/right.gif"));

      s_imagesLoaded = true;
  }

  private List<ChooserEntry> m_entries = new ArrayList<ChooserEntry>();
  private final Map<Unit, Collection<Unit>> m_dependents;
  private JTextArea m_title;
  private int m_total = -1;
  private JLabel m_leftToSelect = new JLabel();
  private GameData m_data;
  private boolean m_allowTwoHit = false;
  private JButton m_autoSelectButton;
  private JButton m_selectNoneButton;
  private final UIContext m_uiContext;
  private boolean m_categorizeMovement;
  private Match<Collection<Unit>> m_match;

  private UnitAutoChooser m_unitAutoChooser;
  private JButton m_solutionBrowseLeft;
  private JButton m_solutionBrowseRight;
  private JLabel m_solutionLabel;
  private JLabel m_solutionTypeLabel;
  private int m_previousSolution = -1;
  private int m_currentSolution = -1;
  
  /** Creates new UnitChooser */ 
  public UnitChooser(Collection<Unit> units, Map<Unit, Collection<Unit>> dependent, boolean categorizeMovement, GameData data, UIContext context)
  {
    m_dependents = dependent;
    m_data = data;
    m_uiContext = context;
    m_match = null;
    m_unitAutoChooser = null;
    m_categorizeMovement = categorizeMovement;
    createEntries(units, dependent, categorizeMovement, Collections.<Unit>emptyList());
    layoutEntries();
    
  }
  public UnitChooser(Collection<Unit> units, Map<Unit, Collection<Unit>> dependent, boolean categorizeMovement, GameData data, UIContext context, Match<Collection<Unit>> match)
  {
    m_match = match;
    m_unitAutoChooser = null;
    m_dependents = dependent;
    m_data = data;
    m_uiContext = context;
    m_categorizeMovement = categorizeMovement;
    createEntries(units, dependent, categorizeMovement, Collections.<Unit>emptyList());
    layoutEntries();
    
  }

 
  public UnitChooser(Collection<Unit> units, Map<Unit, Collection<Unit>> dependent, GameData data, boolean allowTwoHit, UIContext uiContext)
  {
    this(units, Collections.<Unit>emptyList(), dependent, data, allowTwoHit, uiContext);
  }

  public UnitChooser(Collection<Unit> units, Collection<Unit> defaultSelections, Map<Unit, Collection<Unit>> dependent, GameData data, boolean allowTwoHit, UIContext uiContext)
  {
      this(units, defaultSelections, dependent, false, data, allowTwoHit, uiContext);
  }
  
  public UnitChooser(Collection<Unit> units, Collection<Unit> defaultSelections, Map<Unit, Collection<Unit>> dependent, boolean categorizeMovement, GameData data, boolean allowTwoHit, UIContext uiContext)
  {
    m_dependents = dependent;
    m_data = data;
    m_allowTwoHit = allowTwoHit;
    m_uiContext = uiContext;
    m_match = null;
    m_unitAutoChooser = null;
    m_categorizeMovement = categorizeMovement;
    createEntries(units, dependent, categorizeMovement, defaultSelections);
    layoutEntries();
  }
  
  public UnitChooser(Collection<Unit> units, Collection<Unit> defaultSelections, Map<Unit, Collection<Unit>> dependent, boolean categorizeMovement, GameData data, boolean allowTwoHit, UIContext uiContext, Match<Collection<Unit>> match)
  {
    m_dependents = dependent;
    m_data = data;
    m_allowTwoHit = allowTwoHit;
    m_uiContext = uiContext;
    m_match = match;
    m_unitAutoChooser = null;
    m_categorizeMovement = categorizeMovement;
    createEntries(units, dependent, categorizeMovement, defaultSelections);
    layoutEntries();
  }

// TODO: Enable this constructor and call it from MovePanel 
//   once TripleAUnit supports carrier dependents
/*
  public UnitChooser(Collection<Unit> units, 
                     Collection<Unit> defaultSelections, 
                     boolean categorizeDependents,
                     boolean categorizeMovement, 
                     GameData data, 
                     boolean allowTwoHit, 
                     UIContext uiContext, 
                     Match<Collection<Unit>> match)
  {
    if (categorizeDependents)
    {
        m_dependents = new HashMap<Unit,Collection<Unit>>();
        for (Unit unit: units)
            m_dependents.put(unit, TripleAUnit.get(unit).getDependents());
    }
    else
        m_dependents = null;
    m_data = data;
    m_allowTwoHit = allowTwoHit;
    m_uiContext = uiContext;
    m_match = match;
    m_unitAutoChooser = null;
    m_categorizeMovement = categorizeMovement;
    createEntries(units, m_dependents, categorizeMovement, defaultSelections);
    layoutEntries();
  }
*/
  public UnitChooser(UnitAutoChooser unitAutoChooser,
                     Map<Unit, Collection<Unit>> dependent, 
                     boolean categorizeMovement, 
                     GameData data, 
                     boolean allowTwoHit, 
                     UIContext uiContext,
                     Match<Collection<Unit>> match)
  {
    m_unitAutoChooser = unitAutoChooser;
    m_currentSolution = 0;
    Collection<Unit> units = unitAutoChooser.getCandidateUnits(false);
    Collection<Unit> defaultSelections = unitAutoChooser.getSolution(m_currentSolution, false);
    m_dependents = dependent;
    m_data = data;
    m_allowTwoHit = allowTwoHit;
    m_uiContext = uiContext;
    m_match = match;
    m_categorizeMovement = categorizeMovement;
    createEntries(units, m_dependents, categorizeMovement, defaultSelections);
    layoutEntries();
    if (m_match != null)
    {
        m_autoSelectButton.setVisible(false);
        m_selectNoneButton.setVisible(false);
        checkMatches();
    }
  }


  /**
   * Set the maximum number of units that we can choose.
   */
  public void setMax(int max)
  {
    m_total = max;
    m_textFieldListener.changedValue(null);
    m_autoSelectButton.setVisible(false);
    m_selectNoneButton.setVisible(false);
  }
  
  public void setMaxAndShowMaxButton(int max)
  {
      
    m_total = max;
    m_textFieldListener.changedValue(null);
    
    
    m_autoSelectButton.setText("Max");
  }


  public void setTitle(String title)
  {
    m_title.setText(title);
  }

  private void updateLeft()
  {
    if(m_total == -1)
      return;

    Iterator<ChooserEntry> iter;
    int selected = getSelectedCount();

    m_leftToSelect.setText("Left to select:" + (m_total - selected));

    iter = m_entries.iterator();
    while(iter.hasNext())
    {
      ChooserEntry entry = iter.next();
      entry.setLeftToSelect(m_total - selected);
    }

    m_leftToSelect.setText("Left to select:" + (m_total - selected));

  }

  private void checkMatches()
  {
    Collection<Unit> allSelectedUnits = new ArrayList<Unit>();
    for (ChooserEntry entry : m_entries)
        addToCollection(allSelectedUnits, entry, entry.getTotalHits(), false);

    // check match against each scroll button
    for (ChooserEntry entry : m_entries)
    {
        Collection<Unit> newSelectedUnits = new ArrayList<Unit>(allSelectedUnits);
        int totalHits = entry.getTotalHits();
        int totalUnits = entry.getCategory().getUnits().size();
        int leftToSelect = 0;
        Iterator<Unit> unitIter = entry.getCategory().getUnits().iterator();
        for (int i=1; i <= totalUnits; i++)
        {
            Unit unit = unitIter.next();
            if (i > totalHits)
                newSelectedUnits.add(unit);
            if (i >= totalHits)
            {
                if (m_match.match(newSelectedUnits))
                    leftToSelect = i - totalHits;
                else
                    break;
            }
        }
        entry.setLeftToSelect(leftToSelect);
    }
  }

  private int getSelectedCount()
  {
    int selected = 0;
    Iterator<ChooserEntry> iter = m_entries.iterator();
    while(iter.hasNext())
    {
      ChooserEntry entry = iter.next();
      selected += entry.getTotalHits();
    }
    return selected;
  }



  private void createEntries(Collection<Unit> units, Map<Unit, Collection<Unit>> dependent, boolean categorizeMovement, Collection<Unit> defaultSelections)
  {
    Collection categories = UnitSeperator.categorize(units, dependent, categorizeMovement);
    Collection defaultSelectionsCategorized = UnitSeperator.categorize(defaultSelections, dependent, categorizeMovement);
    IntegerMap<UnitCategory> defaultValues = createDefaultSelectionsMap(defaultSelectionsCategorized);
    Iterator iter = categories.iterator();
    while(iter.hasNext())
    {
      UnitCategory category = (UnitCategory) iter.next();
      addCategory(category, defaultValues.getInt(category));
    }
  }

  private IntegerMap<UnitCategory> createDefaultSelectionsMap(Collection categories)
  {
    IntegerMap<UnitCategory> defaultValues = new IntegerMap<UnitCategory>();
    Iterator iter = categories.iterator();
    while (iter.hasNext()) {
      UnitCategory category = (UnitCategory) iter.next();
      int defaultValue = category.getUnits().size();
      defaultValues.put(category, defaultValue);
    }
    return defaultValues;
  }

  private void addCategory(UnitCategory category, int defaultValue)
  {
    ChooserEntry entry = new ChooserEntry(category, m_textFieldListener, m_data, m_allowTwoHit, defaultValue, m_uiContext);
    m_entries.add(entry);

  }

  private void layoutEntries()
  {
    this.setLayout(new GridBagLayout());

    loadImages();

    m_title = new JTextArea("Choose units");
    m_title.setBackground(this.getBackground());
    m_title.setEditable(false);
    //m_title.setColumns(15);
    m_title.setWrapStyleWord(true);
    Insets nullInsets = new Insets(0,0,0,0);

    Dimension buttonSize = new Dimension(80,20);

    m_selectNoneButton = new JButton("None");
    m_selectNoneButton.setPreferredSize(buttonSize);
    m_autoSelectButton = new JButton("All");
    m_autoSelectButton.setPreferredSize(buttonSize);

    add(m_title, new GridBagConstraints(0,0,7 ,1,0,0.5,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,nullInsets, 0,0 )  );


    m_selectNoneButton.addActionListener
       (
       new ActionListener()
       {
         public void actionPerformed(ActionEvent e)
         {
           selectNone();
         }
       }
     );


    m_autoSelectButton.addActionListener
      (
      new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          autoSelect();
        }
      }
    );

    int yIndex = 1;
    Iterator<ChooserEntry> iter = m_entries.iterator();
    while(iter.hasNext())
    {
      ChooserEntry entry = iter.next();
      entry.createComponents(this, yIndex);
      yIndex++;
    }

    if (m_unitAutoChooser == null)
    {
        add(m_autoSelectButton, new GridBagConstraints(0,yIndex,7 ,1,0,0.5,GridBagConstraints.EAST, GridBagConstraints.NONE,nullInsets, 0,0 )  );
        yIndex++;
        add(m_leftToSelect, new GridBagConstraints(0,yIndex,5,2,0,0.5,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,nullInsets, 0,0 ));
    }
    else
    {
        m_solutionLabel = new JLabel("", JLabel.RIGHT);
        m_solutionTypeLabel = new JLabel("", JLabel.RIGHT);
        m_solutionBrowseLeft = new JButton(s_left);
        m_solutionBrowseRight = new JButton(s_right);

        add(m_solutionLabel, new GridBagConstraints(0,yIndex,3,1,1,0,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,new Insets(0,0,0,4), 0,0 ));
        add(m_solutionBrowseLeft, new GridBagConstraints(3,yIndex,1,2,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE,nullInsets, 0,0 ));
        add(m_solutionBrowseRight, new GridBagConstraints(3,yIndex,1,2,0,0,GridBagConstraints.CENTER, GridBagConstraints.NONE,nullInsets, 0,0 ));
        yIndex++;
        add(m_solutionTypeLabel, new GridBagConstraints(0,yIndex,3,1,1,0,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,new Insets(0,0,0,4), 0,0 ));

        m_solutionBrowseLeft.addActionListener
          (
          new ActionListener()
          {
            public void actionPerformed(ActionEvent e)
            {
                m_currentSolution--;
                updateSolutions();
            }
          }
        );
        m_solutionBrowseRight.addActionListener
          (
          new ActionListener()
          {
            public void actionPerformed(ActionEvent e)
            {
                m_currentSolution++;
                updateSolutions();
            }
          }
        );

    }

    if (m_match != null)
    {
        m_autoSelectButton.setVisible(false);
        m_selectNoneButton.setVisible(false);
        checkMatches();
    }

    updateSolutions();

  }

  private void updateSolutions()
  {

      if (m_unitAutoChooser == null)
          return;

      m_solutionBrowseLeft.setEnabled(m_currentSolution >= 0);

      m_solutionBrowseRight.setEnabled((m_currentSolution + 1) < m_unitAutoChooser.solutionCount());

      StringBuilder solutionSb = new StringBuilder("Solution ")
                               .append(m_currentSolution + 1)
                               .append(" of ")
                               .append(m_unitAutoChooser.solutionCount());
      if (m_currentSolution >= 0 && m_unitAutoChooser.foundCompleteSolution())
      {
          if (m_currentSolution < m_unitAutoChooser.exactSolutionCount())
              m_solutionTypeLabel.setText("(exact solution "+(m_currentSolution+1)+" of "+m_unitAutoChooser.exactSolutionCount()+")");
          else
              m_solutionTypeLabel.setText("(greedy solution)");
      }
      else
          m_solutionTypeLabel.setText("(incomplete solution)");

      m_solutionLabel.setText(solutionSb.toString());

      // if a complete solution is selected, update the UI to reflect it
      if (m_currentSolution != -1)
      {
          selectNone();

          Collection<Unit> defaultSelections = m_unitAutoChooser.getSolution(m_currentSolution, false);
          Collection defaultSelectionsCategorized = UnitSeperator.categorize(defaultSelections, m_dependents, m_categorizeMovement);
          IntegerMap<UnitCategory> defaultValues = createDefaultSelectionsMap(defaultSelectionsCategorized);

          System.out.println("categoryMap: "+defaultValues);

          for (ChooserEntry entry : m_entries)
          {
              System.out.println("  entry: " + entry.getCategory());
              System.out.println("  value: " + defaultValues.getInt(entry.getCategory()));
              for (int i=1; i <= defaultValues.getInt(entry.getCategory()); i++)
              {
                  entry.set(i);
                  if (m_match != null)
                      checkMatches();
                  else
                      updateLeft();
              }
          }
      }
      // if a complete solution is NOT selected, and we got here 
      // via the Solution browser buttons, then deselect all.
      else if (m_currentSolution != m_previousSolution)
          selectNone();

      // update state so we can tell how we got in here next time
      m_previousSolution = m_currentSolution;
  }

  public Collection<Unit> getSelected()
  {
    return getSelected(true);
  }

  /**
   * get the units selected.
   * If units are two hit enabled, returns those with two hits.
   */
  public List<Unit> getSelected(boolean selectDependents)
  {
    List<Unit> selectedUnits = new ArrayList<Unit>();

    Iterator<ChooserEntry> entries = m_entries.iterator();
    while(entries.hasNext())
    {
      ChooserEntry chooserEntry = entries.next();
      if(chooserEntry.isTwoHit())
          addToCollection(selectedUnits, chooserEntry,  chooserEntry.getSecondHits(), selectDependents);
      else
          addToCollection(selectedUnits, chooserEntry, chooserEntry.getFirstHits(), selectDependents);

    }
    return selectedUnits;
  }

  /**
   * Only applicable if this dialog was constructed using twoHits
   */
  public List<Unit> getSelectedFirstHit()
  {
      List<Unit> selectedUnits = new ArrayList<Unit>();

      Iterator<ChooserEntry> entries = m_entries.iterator();
      while(entries.hasNext())
      {
        ChooserEntry chooserEntry = entries.next();
        if(chooserEntry.isTwoHit())
            addToCollection(selectedUnits, chooserEntry, chooserEntry.getFirstHits(), false);
      }
      return selectedUnits;
  }

  private void selectNone()
  {
    Iterator<ChooserEntry> iter = m_entries.iterator();
    while (iter.hasNext())
    {
      ChooserEntry entry = iter.next();
      entry.selectNone();
    }
  }

  private void autoSelect()
  {
    
    if(m_total == -1)
    {
        Iterator<ChooserEntry> iter = m_entries.iterator();
        while (iter.hasNext())
        {
          ChooserEntry entry = iter.next();
          
          entry.selectAll();
        }
    }
    else
    {
        int leftToSelect = m_total - getSelectedCount();
        for(ChooserEntry entry : m_entries)
        {
            int canSelect = entry.getMax() - entry.getFirstHits();
            if(leftToSelect >= canSelect)
            {
                entry.selectAll();
                leftToSelect -= canSelect;
            }
            else
            {
                entry.set(entry.getFirstHits() + canSelect);
                 leftToSelect = 0;
                break;
            }
        }
    }
  }

  private void addToCollection(Collection<Unit> addTo, ChooserEntry entry, int quantity, boolean addDependents)
  {

    Collection<Unit> possible = entry.getCategory().getUnits();
    if(possible.size() < quantity)
      throw new IllegalStateException("Not enough units");

    Iterator<Unit> iter = possible.iterator();
    for(int i = 0; i < quantity; i++)
    {
      Unit current = (Unit) iter.next();
      addTo.add(current);
      if(addDependents)
      {
        Collection<Unit> dependents =  m_dependents.get(current);
        if(dependents != null)
          addTo.addAll(dependents);
      }
    }
  }

  private ScrollableTextFieldListener m_textFieldListener = new ScrollableTextFieldListener()
  {
    public void changedValue(ScrollableTextField field)
    {
        if (m_match != null)
            checkMatches();
        else
            updateLeft();

        // this condition ensures we weren't called from updateSolutions,
        // to protect against unwanted recursion
        if (m_unitAutoChooser != null 
                && m_previousSolution == m_currentSolution)
        {
            // coming from incomplete solution... see if we found a solution 
            if (m_currentSolution == -1)
            {
                Set<Unit> allSelectedUnits = new HashSet<Unit>();
                for (ChooserEntry entry : m_entries)
                    addToCollection(allSelectedUnits, entry, entry.getTotalHits(), true);
                if (!allSelectedUnits.isEmpty())
                {
                    for (int i=0; i<m_unitAutoChooser.solutionCount(); i++)
                    {
                        if (m_unitAutoChooser.getSolution(i, true).equals(allSelectedUnits))
                        {
                            m_currentSolution = i;
                            updateSolutions();
                            break;
                        }
                    }
                }
            }
            // coming from complete solution... set as incomplete
            else
            {
                m_currentSolution = -1;
                m_previousSolution = -1;
                updateSolutions();
            }
        }

    }
  };
 
  
}

class ChooserEntry
{
  private final UnitCategory m_category;
  private ScrollableTextField m_hitText;
  private final ScrollableTextFieldListener m_hitTextFieldListener;
  private final GameData m_data;
  private final boolean m_hasSecondHit;

  private final int m_defaultValueFirstHits;
  private final int m_defaultValueSecondHits;

  private ScrollableTextField m_secondHitText;

  private JLabel m_secondHitLabel;
  private int m_leftToSelect = 0;
  private static Insets nullInsets = new Insets(0,0,0,0);
  private final UIContext m_uiContext;


  ChooserEntry(UnitCategory category, ScrollableTextFieldListener listener, GameData data, boolean allowTwoHit, int defaultValue, UIContext uiContext)
  {
    m_hitTextFieldListener = listener;
    m_data = data;
    m_category = category;
    m_hasSecondHit = allowTwoHit && category.isTwoHit() && ! category.getDamaged();
    int numUnits = category.getUnits().size();
    m_defaultValueFirstHits = numUnits < defaultValue ? numUnits : defaultValue;
    m_defaultValueSecondHits = numUnits < defaultValue ? defaultValue - numUnits : 0;
    m_uiContext = uiContext;
    //    System.out.println("Default hits: " + m_defaultValueFirstHits + " " + m_defaultValueSecondHits);
  }

  public void createComponents(JPanel panel, int yIndex)
  {

      panel.add(new UnitChooserEntryIcon(false, m_uiContext) ,
                new GridBagConstraints(0,yIndex,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,nullInsets, 0,0 ) );

      if (m_category.getMovement() != -1)
          panel.add(new JLabel("mvt " + m_category.getMovement()),
                new GridBagConstraints(1,yIndex,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,new Insets(0,4,0,4), 0,0 ) );

      panel.add(new JLabel("x" + m_category.getUnits().size()),
           new GridBagConstraints(2,yIndex,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,nullInsets, 0,0 ) );

      m_hitText = new ScrollableTextField(0, m_category.getUnits().size());
      m_hitText.setValue(m_defaultValueFirstHits);
      m_hitText.addChangeListener(m_hitTextFieldListener);
      panel.add(m_hitText,
        new GridBagConstraints(3,yIndex,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,4,0,0), 0,0 ) );


      if (m_hasSecondHit)
      {
          panel.add(new UnitChooserEntryIcon(true, m_uiContext),
              new GridBagConstraints(4,yIndex,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,new Insets(0,8,0,0), 0,0 ) );

          m_secondHitLabel = new JLabel("x0");

          m_secondHitText = new ScrollableTextField(0, 0);
          m_secondHitText.setValue(m_defaultValueSecondHits);
          updateLeftToSelect();
          panel.add(m_secondHitLabel,
                    new GridBagConstraints(5,yIndex,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,new Insets(0,0,0,4), 0,0 ) );

          panel.add(m_secondHitText,
                    new GridBagConstraints(6,yIndex,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,nullInsets, 0,0 ) );

          m_hitText.addChangeListener(
              new ScrollableTextFieldListener()
          {
              public void changedValue(ScrollableTextField field)
              {
                  m_secondHitLabel.setText("x" + field.getValue());
                  updateLeftToSelect();
              }
          }
          );

           m_secondHitText.addChangeListener(m_hitTextFieldListener);
      }
  }

  public int getMax()
  {
      return m_hitText.getMax();
  }
  
  public void set(int value)
  {
      m_hitText.setValue(value);
  }

  public UnitCategory getCategory()
  {
    return m_category;
  }

  public void selectAll()
  {
    m_hitText.setValue(m_hitText.getMax());
  }

  public void selectNone()
  {
    m_hitText.setValue(0);
  }

  public void setLeftToSelect(int leftToSelect)
  {
    m_leftToSelect = leftToSelect;
    updateLeftToSelect();

  }

  private void updateLeftToSelect()
  {
      int newMax = m_leftToSelect + m_hitText.getValue();

      m_hitText.setMax(Math.min(newMax, m_category.getUnits().size()));

      if (m_hasSecondHit)
      {
          int newSecondHitMax = m_leftToSelect + m_secondHitText.getValue();
          m_secondHitText.setMax(Math.min(newSecondHitMax, m_hitText.getValue()));
      }
  }


  public int getTotalHits()
  {
      return getFirstHits() + getSecondHits();
  }

  public int getFirstHits()
  {
      return m_hitText.getValue();
  }

  public int getSecondHits()
  {
      if(!m_hasSecondHit)
          return 0;
      return m_secondHitText.getValue();
  }

  public boolean isTwoHit()
  {
      return m_hasSecondHit;
  }

  class UnitChooserEntryIcon extends JComponent
  {
      private boolean m_forceDamaged;
      private final UIContext m_uiContext;

    UnitChooserEntryIcon(boolean forceDamaged, UIContext uiContext)
    {
        m_forceDamaged = forceDamaged;
        m_uiContext = uiContext;
    }



    public void paint(Graphics g)
    {
      super.paint(g);
      g.drawImage( m_uiContext.getUnitImageFactory().getImage(m_category.getType(), m_category.getOwner(), m_data, m_forceDamaged || m_category.getDamaged()), 0,0,this);
      Iterator iter = m_category.getDependents().iterator();
      int index = 1;
      while(iter.hasNext())
      {
        UnitOwner holder = (UnitOwner) iter.next();
        int x = m_uiContext.getUnitImageFactory().getUnitImageWidth() * index;
        Image unitImg = m_uiContext.getUnitImageFactory().getImage(holder.getType(), holder.getOwner(), m_data, false);
        g.drawImage(unitImg, x, 0, this);


        index++;
      }
    }

    public int getWidth()
    {
      //we draw a unit symbol for each dependent
      return m_uiContext.getUnitImageFactory().getUnitImageWidth() * (1 + m_category.getDependents().size());
    }

    public int getHeight()
    {
      return m_uiContext.getUnitImageFactory().getUnitImageHeight();
    }

    public Dimension getMaximumSize()
    {
      return getDimension();
    }

    public Dimension getMinimumSize()
    {
      return getDimension();
    }

    public Dimension getPreferredSize()
    {
      return getDimension();
    }

    public Dimension getDimension()
    {
      return new Dimension(getWidth(), getHeight());
    }
  }
}

