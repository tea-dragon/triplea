/**
 * Created on 15.03.2012
 */
package games.strategy.engine.framework.startup.launcher;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.ui.NewGameChooser;
import games.strategy.engine.framework.ui.background.WaitWindow;

import java.awt.Component;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Abstract class for launching a game.
 * 
 * @author Abstraction done by Frigoref, Original code by Sean Bridges
 * 
 */
abstract public class AbstractLauncher implements ILauncher
{
	protected final GameData m_gameData;
	protected final GameSelectorModel m_gameSelectorModel;
	protected final WaitWindow m_gameLoadingWindow;
	protected final boolean m_headless;
	
	protected AbstractLauncher(final GameSelectorModel gameSelectorModel)
	{
		this(gameSelectorModel, false);
	}
	
	protected AbstractLauncher(final GameSelectorModel gameSelectorModel, final boolean headless)
	{
		m_headless = headless;
		if (m_headless)
			m_gameLoadingWindow = null;
		else
			m_gameLoadingWindow = new WaitWindow("Loading game, please wait.");
		m_gameSelectorModel = gameSelectorModel;
		m_gameData = gameSelectorModel.getGameData();
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.engine.framework.startup.launcher.ILauncher#launch(java.awt.Component)
	 */
	public void launch(final Component parent)
	{
		if (!m_headless && !SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("Wrong thread");
		final Runnable r = new Runnable()
		{
			public void run()
			{
				NewGameChooser.clearNewGameChooserModel(); // we don't want to keep around all the memory for this, since we have the gamedata that we want
				launchInNewThread(parent);
			}
		};
		final Thread t = new Thread(r, "Triplea start thread");
		if (!m_headless && m_gameLoadingWindow != null)
		{
			m_gameLoadingWindow.setLocationRelativeTo(JOptionPane.getFrameForComponent(parent));
			m_gameLoadingWindow.setVisible(true);
			m_gameLoadingWindow.showWait();
		}
		if (parent != null)
			JOptionPane.getFrameForComponent(parent).setVisible(false);
		t.start();
	}
	
	abstract protected void launchInNewThread(Component parent);
	
}
