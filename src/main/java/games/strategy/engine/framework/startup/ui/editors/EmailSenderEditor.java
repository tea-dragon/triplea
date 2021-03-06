package games.strategy.engine.framework.startup.ui.editors;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.startup.ui.editors.validators.EmailValidator;
import games.strategy.engine.framework.startup.ui.editors.validators.IntegerRangeValidator;
import games.strategy.engine.pbem.GenericEmailSender;
import games.strategy.engine.pbem.IEmailSender;
import games.strategy.ui.ProgressWindow;

import java.awt.GridBagConstraints;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * An editor for modifying email senders
 * 
 * @author Klaus Groenbaek
 */
public class EmailSenderEditor extends EditorPanel
{
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	
	private static final long serialVersionUID = -4647781117491269926L;
	private final GenericEmailSender m_bean;
	private final JTextField m_subject = new JTextField();
	
	private final JTextField m_toAddress = new JTextField();
	private final JTextField m_host = new JTextField();
	private final JTextField m_port = new JTextField();
	private final JTextField m_login = new JTextField();
	private final JCheckBox m_useTLS = new JCheckBox("Use TLS encryption");
	private final JTextField m_password = new JPasswordField();
	private final JLabel m_toLabel = new JLabel("To:");
	private final JLabel m_loginLabel = new JLabel("Login:");
	private final JLabel m_passwordLabel = new JLabel("Password:");
	private final JLabel m_hostLabel = new JLabel("Host:");
	private final JLabel m_portLabel = new JLabel("Port:");
	private final JButton m_testEmail = new JButton("Test Email");
	private final JCheckBox m_alsoPostAfterCombatMove = new JCheckBox("Also Post After Combat Move");
	
	// -----------------------------------------------------------------------
	// constructors
	// -----------------------------------------------------------------------
	
	/**
	 * creates a new instance
	 * 
	 * @param bean
	 *            the EmailSender to edit
	 * @param editorConfiguration
	 *            configures which editor fields should be visible
	 */
	public EmailSenderEditor(final GenericEmailSender bean, final EditorConfiguration editorConfiguration)
	{
		super();
		m_bean = bean;
		
		m_subject.setText(m_bean.getSubjectPrefix());
		m_host.setText(m_bean.getHost());
		m_port.setText(String.valueOf(m_bean.getPort()));
		m_toAddress.setText(m_bean.getToAddress());
		m_login.setText(m_bean.getUserName());
		m_password.setText(m_bean.getPassword());
		m_useTLS.setSelected(m_bean.getEncryption() == GenericEmailSender.Encryption.TLS);
		
		final int bottomSpace = 1;
		final int labelSpace = 2;
		int row = 0;
		
		add(new JLabel("Subject:"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
		add(m_subject, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
		
		row++;
		add(m_toLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
		add(m_toAddress, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
		
		row++;
		add(m_loginLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
		add(m_login, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
		
		row++;
		add(m_passwordLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
		add(m_password, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
		
		if (editorConfiguration.showHost)
		{
			row++;
			add(m_hostLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
			add(m_host, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
		}
		
		if (editorConfiguration.showPort)
		{
			row++;
			add(m_portLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
			add(m_port, new GridBagConstraints(1, row, 2, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
		}
		
		if (editorConfiguration.showEncryption)
		{
			row++;
			add(m_useTLS, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, 0), 0, 0));
			// add Test button on the same line as encryption
			add(m_testEmail, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, 0), 0, 0));
			
			row++;
			add(m_alsoPostAfterCombatMove, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, 0), 0, 0));
		}
		else
		{
			row++;
			add(m_alsoPostAfterCombatMove, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, 0), 0, 0));
			add(m_testEmail, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, 0), 0, 0));
			// or on a separate line if no encryption
			// add(m_testEmail, new GridBagConstraints(1, row, 3, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, 0), 0, 0));
		}
		
		setupListeners();
	}
	
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	
	private void setupListeners()
	{
		final EditorChangedFiringDocumentListener listener = new EditorChangedFiringDocumentListener();
		m_host.getDocument().addDocumentListener(listener);
		m_login.getDocument().addDocumentListener(listener);
		m_port.getDocument().addDocumentListener(listener);
		m_password.getDocument().addDocumentListener(listener);
		m_toAddress.getDocument().addDocumentListener(listener);
		m_useTLS.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				fireEditorChanged();
			}
		});
		
		m_testEmail.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				testEmail();
			}
		});
		
	}
	
	/**
	 * Tests the email sender. This must be called from the swing event thread
	 */
	private void testEmail()
	{
		final ProgressWindow progressWindow = new ProgressWindow(MainFrame.getInstance(), "Sending test email...");
		progressWindow.setVisible(true);
		
		final Runnable runnable = new Runnable()
		{
			public void run()
			{
				// initialize variables to error state, override if successful
				String message = "An unknown occurred, report this as a bug on the TripleA dev forum";
				int messageType = JOptionPane.ERROR_MESSAGE;
				try
				{
					final String html = "<html><body><h1>Success</h1><p>This was a test email sent by TripleA<p></body></html>";
					final File dummy = new File(GameRunner2.getUserRootFolder(), "dummySave.txt");
					dummy.deleteOnExit();
					final FileOutputStream fout = new FileOutputStream(dummy);
					fout.write("This file would normally be a save game".getBytes());
					fout.close();
					
					((IEmailSender) getBean()).sendEmail("TripleA Test", html, dummy, "dummy.txt");
					// email was sent, or an exception would have been thrown
					message = "Email sent, it should arrive shortly, otherwise check your spam folder";
					messageType = JOptionPane.INFORMATION_MESSAGE;
					
				} catch (final IOException ioe)
				{
					message = "Unable to send email: " + ioe.getMessage();
					
				} finally
				{
					// now that we have a result, marshall it back unto the swing thread
					final String finalMessage = message;
					final int finalMessageType = messageType;
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							try
							{
								JOptionPane.showMessageDialog(MainFrame.getInstance(), finalMessage, "Email Test", finalMessageType);
							} catch (final HeadlessException e)
							{
								// should never happen in a GUI app
							}
						}
					});
					progressWindow.setVisible(false);
				}
				
			}
		};
		// start a background thread
		final Thread t = new Thread(runnable);
		t.start();
	}
	
	@Override
	public boolean isBeanValid()
	{
		
		final boolean hostValid = validateTextFieldNotEmpty(m_host, m_hostLabel);
		final boolean portValid = validateTextField(m_port, m_portLabel, new IntegerRangeValidator(0, 65635));
		// boolean loginValid = validateTextFieldNotEmpty(m_login, m_loginLabel);
		// boolean passwordValid = validateTextFieldNotEmpty(m_password, m_passwordLabel);
		final boolean addressValid = validateTextField(m_toAddress, m_toLabel, new EmailValidator(false));
		
		final boolean allValid = hostValid && portValid && /*loginValid && passwordValid && */addressValid;
		m_testEmail.setEnabled(allValid);
		return allValid;
	}
	
	@Override
	public IBean getBean()
	{
		m_bean.setEncryption(m_useTLS.isSelected() ? GenericEmailSender.Encryption.TLS : GenericEmailSender.Encryption.NONE);
		m_bean.setSubjectPrefix(m_subject.getText());
		m_bean.setHost(m_host.getText());
		m_bean.setUserName(m_login.getText());
		m_bean.setPassword(m_password.getText());
		int port = 0;
		try
		{
			port = Integer.parseInt(m_port.getText());
		} catch (final NumberFormatException e)
		{
			// ignore
		}
		m_bean.setPort(port);
		m_bean.setToAddress(m_toAddress.getText());
		m_bean.setAlsoPostAfterCombatMove(m_alsoPostAfterCombatMove.isSelected());
		return m_bean;
	}
	
	
	// -----------------------------------------------------------------------
	// inner classes
	// -----------------------------------------------------------------------
	
	/**
	 * class for configuring the editor so some fields can be hidden
	 */
	public static class EditorConfiguration
	{
		public boolean showHost;
		public boolean showPort;
		public boolean showEncryption;
		
		public EditorConfiguration()
		{
		}
		
		public EditorConfiguration(final boolean showHost, final boolean showPort, final boolean showEncryption)
		{
			this.showHost = showHost;
			this.showPort = showPort;
			this.showEncryption = showEncryption;
		}
	}
	
}
