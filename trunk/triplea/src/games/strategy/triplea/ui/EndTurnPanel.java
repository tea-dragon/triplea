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
 * EndTurnPanel.java
 *
 * Created on December 2, 2006, 10:04 AM
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.*;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.history.*;
import games.strategy.engine.pbem.IPBEMMessenger;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.delegate.remote.IAbstractEndTurnDelegate;
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.ui.ProgressWindow;
import java.awt.event.ActionEvent;
import java.io.*;
import javax.swing.*;

/**
 * 
 * @author Tony Clayton
 * @version 1.0
 */
public class EndTurnPanel extends ActionPanel
{
    private JLabel m_actionLabel;
    private IPlayerBridge m_bridge;
    private PBEMMessagePoster m_poster;
    private TripleAFrame m_frame;
    private HistoryLog m_historyLog;
    private JButton m_postButton;
    private Action GetViewAction;
    private Action GetPostAction;
    private Action DontBother;

    public EndTurnPanel(GameData data, MapPanel map)
    {
        super(data, map);
        m_actionLabel = new JLabel();
        GetViewAction = new AbstractAction("View Turn Summary") {

            public void actionPerformed(ActionEvent event)
            {
                m_historyLog.setVisible(true);
            }
        };
        GetPostAction = new AbstractAction("Post Turn Summary") {

            public void actionPerformed(ActionEvent event)
            {
                String ok = "Post";
                String cancel = "Cancel";
                String options[] = {
                    ok, cancel
                };
                String message = "";
                final IPBEMMessenger screenshotMsgr = m_poster.getScreenshotMessenger();
                final IPBEMMessenger saveGameMsgr = m_poster.getSaveGameMessenger();
                final IPBEMMessenger turnSummaryMsgr = m_poster.getTurnSummaryMessenger();
                if(screenshotMsgr != null)
                    message = (new StringBuilder()).append(message).append("Post screenshot to ").append(screenshotMsgr.getName()).append("?\n").toString();
                if(saveGameMsgr != null)
                    message = (new StringBuilder()).append(message).append("Post save game file to ").append(saveGameMsgr.getName()).append("?\n").toString();
                if(turnSummaryMsgr != null)
                    message = (new StringBuilder()).append(message).append("Post turn summary to ").append(turnSummaryMsgr.getName()).append("?\n").toString();
                int choice = JOptionPane.showConfirmDialog(getTopLevelAncestor(), message, "Post Turn Summary?", 2, -1, null);
                if(choice != 0)
                {
                    return;
                } else
                {
                    m_postButton.setEnabled(false);
                    Runnable t = new Runnable() {

                        public void run()
                        {
                            boolean postOk = true;
                            ProgressWindow progressWindow = new ProgressWindow(m_frame, "Posting Turn Summary...");
                            progressWindow.setVisible(true);
                            IAbstractEndTurnDelegate delegate = (IAbstractEndTurnDelegate)m_bridge.getRemote();
                            delegate.setHasPostedTurnSummary(true);
                            File screenshotFile = null;
                            File saveGameFile = null;
                            if(screenshotMsgr != null)
                            {
                                try
                                {
                                    screenshotFile = File.createTempFile("triplea", ".png");
                                    if(screenshotFile != null && m_frame.saveScreenshot(getData().getHistory().getLastNode(), screenshotFile))
                                        m_poster.setScreenshot(screenshotFile);
                                }
                                catch(Exception e)
                                {
                                    postOk = false;
                                    e.printStackTrace();
                                }
                            }
                            if(saveGameMsgr != null)
                            {
                                try
                                {
                                    saveGameFile = File.createTempFile("triplea", ".tsvg");
                                    if(saveGameFile != null)
                                    {
                                        int round = 0;
                                        Object pathFromRoot[] = getData().getHistory().getLastNode().getPath();
                                        Object arr$[] = pathFromRoot;
                                        int len$ = arr$.length;
                                        int i$ = 0;
                                        do
                                        {
                                            if(i$ >= len$)
                                                break;
                                            Object pathNode = arr$[i$];
                                            HistoryNode curNode = (HistoryNode)pathNode;
                                            if(curNode instanceof Round)
                                            {
                                                round = ((Round)curNode).getRoundNo();
                                                break;
                                            }
                                            i$++;
                                        } while(true);
                                        m_frame.getGame().saveGame(saveGameFile);
                                        StringBuilder saveGameSb = (new StringBuilder()).append("triplea_")
                                                                                        .append(saveGameMsgr.getGameId())
                                                                                        .append("_")
                                                                                        .append(getCurrentPlayer().getName().substring(0, 1))
                                                                                        .append(round)
                                                                                        .append(".tsvg");
                                        m_poster.setSaveGame(saveGameSb.toString(), new FileInputStream(saveGameFile));
                                    }
                                }
                                catch(Exception e)
                                {
                                    postOk = false;
                                    e.printStackTrace();
                                }
                            }
                            if(turnSummaryMsgr != null)
                                m_poster.setTurnSummary(m_historyLog.toString());
                            try
                            {
                                if(!m_poster.post())
                                    postOk = false;
                            }
                            catch(Exception e)
                            {
                                postOk = false;
                                e.printStackTrace();
                            }
                            String screenshotRef = m_poster.getScreenshotRef();
                            String saveGameRef = m_poster.getSaveGameRef();
                            String turnSummaryRef = m_poster.getTurnSummaryRef();
                            String message = "";
                            if(screenshotRef != null)
                                message = (new StringBuilder()).append(message).append("\nScreenshot: ").append(screenshotRef).toString();
                            if(saveGameRef != null)
                                message = (new StringBuilder()).append(message).append("\nSave Game : ").append(saveGameRef).toString();
                            if(turnSummaryRef != null)
                                message = (new StringBuilder()).append(message).append("\nSummary Text: ").append(turnSummaryRef).toString();
                            m_historyLog.getWriter().println(message);
                            if(m_historyLog.isVisible())
                                m_historyLog.setVisible(true);
                            try
                            {
                                if(screenshotFile != null && !screenshotFile.delete())
                                    System.err.println((new StringBuilder()).append("couldn't delete ").append(screenshotFile.getCanonicalPath()).toString());
                                if(saveGameFile != null && !saveGameFile.delete())
                                    System.err.println((new StringBuilder()).append("couldn't delete ").append(saveGameFile.getCanonicalPath()).toString());
                            }
                            catch(IOException ioe)
                            {
                                ioe.printStackTrace();
                            }
                            progressWindow.setVisible(false);
                            progressWindow.removeAll();
                            progressWindow.dispose();
                            delegate.setHasPostedTurnSummary(postOk);
                            m_postButton.setEnabled(!postOk);
                            if(postOk)
                                JOptionPane.showMessageDialog(m_frame, message, "Turn Summary Posted", 1);
                            else
                                JOptionPane.showMessageDialog(m_frame, message, "Turn Summary Posted", 0);
                        }
                    };
                    (new Thread(t)).start();
                    return;
                }
            }

        };
        DontBother = new AbstractAction("Done") {

            public void actionPerformed(ActionEvent event)
            {
                release();
            }

        };
    }

    public void display(final PlayerID id)
    {
        super.display(id);
        SwingUtilities.invokeLater(new Runnable() {

            public void run()
            {
                removeAll();
                m_actionLabel.setText(id.getName() + " Turn Summary");
                add(m_actionLabel);
                add(new JButton(GetViewAction));
                m_postButton = new JButton(GetPostAction);
                add(m_postButton);
                add(new JButton(DontBother));
            }

        });
    }

    public String toString()
    {
        return "EndTurnPanel";
    }

    public void waitForEndTurn(TripleAFrame frame, IPlayerBridge bridge)
    {
        m_frame = frame;
        m_bridge = bridge;

        // Nothing to do if there are no PBEM messengers
        IAbstractEndTurnDelegate delegate = (IAbstractEndTurnDelegate)m_bridge.getRemote();
        m_poster = delegate.getPBEMMessagePoster();
        if(!m_poster.hasMessengers())
            return;
        m_historyLog = new HistoryLog();
        m_historyLog.setEditable(true);
        m_historyLog.printTurn(getData().getHistory().getLastNode(), false);
        final boolean hasPosted = delegate.getHasPostedTurnSummary();
        SwingUtilities.invokeLater(new Runnable() {

            public void run()
            {
                m_postButton.setEnabled(!hasPosted);
            }

        });
        waitForRelease();
        return;
    }
}
