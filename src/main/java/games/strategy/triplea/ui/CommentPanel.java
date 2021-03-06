package games.strategy.triplea.ui;

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
 * CommentPanel.java Swing ui for comment logging.
 * 
 * Created on September 24, 2007
 */
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.history.Event;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.triplea.delegate.remote.IEditDelegate;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.tree.TreeNode;

/**
 * A Comment logging window.
 * 
 * @author Tony Clayton
 */
public class CommentPanel extends JPanel
{
	private static final long serialVersionUID = -9122162393288045888L;
	private JTextPane m_text;
	private JScrollPane m_scrollPane;
	private JTextField m_nextMessage;
	private JButton m_save;
	private final GameData m_data;
	private final TripleAFrame m_frame;
	private Map<PlayerID, Icon> m_iconMap;
	private final SimpleAttributeSet bold = new SimpleAttributeSet();
	private final SimpleAttributeSet italic = new SimpleAttributeSet();
	private final SimpleAttributeSet normal = new SimpleAttributeSet();
	
	public CommentPanel(final TripleAFrame frame, final GameData data)
	{
		m_frame = frame;
		m_data = data;
		init();
	}
	
	private void init()
	{
		createComponents();
		layoutComponents();
		setupKeyMap();
		StyleConstants.setBold(bold, true);
		StyleConstants.setItalic(italic, true);
		setSize(300, 200);
		loadHistory();
		setupListeners();
	}
	
	private void layoutComponents()
	{
		final Container content = this;
		content.setLayout(new BorderLayout());
		m_scrollPane = new JScrollPane(m_text);
		content.add(m_scrollPane, BorderLayout.CENTER);
		content.add(m_scrollPane, BorderLayout.CENTER);
		final JPanel savePanel = new JPanel();
		savePanel.setLayout(new BorderLayout());
		savePanel.add(m_nextMessage, BorderLayout.CENTER);
		savePanel.add(m_save, BorderLayout.WEST);
		content.add(savePanel, BorderLayout.SOUTH);
	}
	
	private void createComponents()
	{
		m_text = new JTextPane();
		m_text.setEditable(false);
		m_text.setFocusable(false);
		m_nextMessage = new JTextField(10);
		// when enter is pressed, send the message
		final Insets inset = new Insets(3, 3, 3, 3);
		m_save = new JButton(m_saveAction);
		m_save.setMargin(inset);
		m_save.setFocusable(false);
		// create icon map
		m_iconMap = new HashMap<PlayerID, Icon>();
		for (final PlayerID playerId : m_data.getPlayerList().getPlayers())
		{
			m_iconMap.put(playerId, new ImageIcon(m_frame.getUIContext().getFlagImageFactory().getSmallFlag(playerId)));
		}
	}
	
	private void setupListeners()
	{
		m_data.getHistory().addTreeModelListener(new TreeModelListener()
		{
			public void treeNodesChanged(final TreeModelEvent e)
			{
			}
			
			public void treeNodesInserted(final TreeModelEvent e)
			{
				readHistoryTreeEvent(e);
			}
			
			public void treeNodesRemoved(final TreeModelEvent e)
			{
			}
			
			public void treeStructureChanged(final TreeModelEvent e)
			{
				readHistoryTreeEvent(e);
			}
		});
	}
	
	private void readHistoryTreeEvent(final TreeModelEvent e)
	{
		final TreeModelEvent tme = e;
		final Runnable runner = new Runnable()
		{
			public void run()
			{
				m_data.acquireReadLock();
				try
				{
					final Document doc = m_text.getDocument();
					final HistoryNode node = (HistoryNode) (tme.getTreePath().getLastPathComponent());
					final TreeNode child = node == null ? null : (node.getChildCount() > 0 ? node.getLastChild() : null);
					final String title = child != null ? (child instanceof Event ? ((Event) child).getDescription() : child.toString()) : (node != null ? node.getTitle() : "");
					final Pattern p = Pattern.compile("^COMMENT: (.*)");
					final Matcher m = p.matcher(title);
					if (m.matches())
					{
						final PlayerID playerId = m_data.getSequence().getStep().getPlayerID();
						final int round = m_data.getSequence().getRound();
						final String player = playerId.getName();
						final Icon icon = m_iconMap.get(playerId);
						try
						{
							// insert into ui document
							final String prefix = " " + player + "(" + round + ") : ";
							m_text.insertIcon(icon);
							doc.insertString(doc.getLength(), prefix, bold);
							doc.insertString(doc.getLength(), m.group(1) + "\n", normal);
						} catch (final BadLocationException ble)
						{
							ble.printStackTrace();
						}
					}
				} finally
				{
					m_data.releaseReadLock();
				}
			}
		};
		// invoke in the swing event thread
		if (SwingUtilities.isEventDispatchThread())
			runner.run();
		else
			SwingUtilities.invokeLater(runner);
	}
	
	private void setupKeyMap()
	{
		final InputMap nextMessageKeymap = m_nextMessage.getInputMap();
		nextMessageKeymap.put(KeyStroke.getKeyStroke('\n'), m_saveAction);
	}
	
	private void cleanupKeyMap()
	{
		final InputMap nextMessageKeymap = m_nextMessage.getInputMap();
		nextMessageKeymap.remove(KeyStroke.getKeyStroke('\n'));
	}
	
	private void loadHistory()
	{
		final Document doc = m_text.getDocument();
		final HistoryNode rootNode = (HistoryNode) m_data.getHistory().getRoot();
		final Enumeration nodeEnum = rootNode.preorderEnumeration();
		final Pattern p = Pattern.compile("^COMMENT: (.*)");
		String player = "";
		int round = 0;
		Icon icon = null;
		while (nodeEnum.hasMoreElements())
		{
			final HistoryNode node = (HistoryNode) nodeEnum.nextElement();
			if (node instanceof Round)
			{
				round++;
				continue;
			}
			else if (node instanceof Step)
			{
				final PlayerID playerId = ((Step) node).getPlayerID();
				if (playerId != null)
				{
					player = playerId.getName();
					icon = m_iconMap.get(playerId);
				}
				continue;
			}
			else
			{
				final String title = node.getTitle();
				final Matcher m = p.matcher(title);
				if (m.matches())
				{
					try
					{
						// insert into ui document
						final String prefix = " " + player + "(" + round + ") : ";
						m_text.insertIcon(icon);
						doc.insertString(doc.getLength(), prefix, bold);
						doc.insertString(doc.getLength(), m.group(1) + "\n", normal);
					} catch (final BadLocationException ble)
					{
						ble.printStackTrace();
					}
				}
			}
		}
	}
	
	/** thread safe */
	public void addMessage(final String message)
	{
		final Runnable runner = new Runnable()
		{
			public void run()
			{
				try
				{
					final Document doc = m_text.getDocument();
					// save history entry
					final IEditDelegate delegate = m_frame.getEditDelegate();
					String error;
					if (delegate == null)
						error = "You can only add comments during your turn";
					else
						error = delegate.addComment(message);
					if (error != null)
					{
						doc.insertString(doc.getLength(), error + "\n", italic);
					}
				} catch (final BadLocationException ble)
				{
					ble.printStackTrace();
				}
				final BoundedRangeModel scrollModel = m_scrollPane.getVerticalScrollBar().getModel();
				scrollModel.setValue(scrollModel.getMaximum());
			}
		};
		// invoke in the swing event thread
		if (SwingUtilities.isEventDispatchThread())
			runner.run();
		else
			SwingUtilities.invokeLater(runner);
	}
	
	/**
	 * Show only the first n lines
	 */
	public static void trimLines(final Document doc, final int lineCount)
	{
		if (doc.getLength() < lineCount)
			return;
		try
		{
			final String text = doc.getText(0, doc.getLength());
			int returnsFound = 0;
			for (int i = text.length() - 1; i >= 0; i--)
			{
				if (text.charAt(i) == '\n')
				{
					returnsFound++;
				}
				if (returnsFound == lineCount)
				{
					doc.remove(0, i);
					return;
				}
			}
		} catch (final BadLocationException e)
		{
			e.printStackTrace();
		}
	}
	
	private final Action m_saveAction = new AbstractAction("Add Comment")
	{
		private static final long serialVersionUID = -5771971912942033713L;
		
		public void actionPerformed(final ActionEvent e)
		{
			if (m_nextMessage.getText().trim().length() == 0)
				return;
			addMessage(m_nextMessage.getText());
			m_nextMessage.setText("");
		}
	};
	
	public void cleanUp()
	{
		cleanupKeyMap();
	}
}
