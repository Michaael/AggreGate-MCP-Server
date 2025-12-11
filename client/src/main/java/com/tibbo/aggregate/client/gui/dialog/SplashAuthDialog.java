package com.tibbo.aggregate.client.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.resource.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;

public abstract class SplashAuthDialog extends AbstractDialog
{
  public static final int MODE_WORKSPACE = 0;
  public static final int MODE_SERVER = 1;
  public static final int MODE_REMOTE_SERVER = 2;
  public static final int MODE_KERBEROS = 3;
  
  protected static final int WIDTH_BACKGROUND_IMAGE = 436; // Update it when the background image is changed!
  
  protected static final int MARGIN_HORIZONTAL = 40;
  protected static final int MARGIN_VERTICAL = 70;
  
  protected static final int WIDTH_INNER_PANEL = WIDTH_BACKGROUND_IMAGE - 2 * MARGIN_HORIZONTAL;
  protected static final int WIDTH_LABEL_COLUMN = 100;
  protected static final int WIDTH_WIDE_FIELD = WIDTH_BACKGROUND_IMAGE - WIDTH_LABEL_COLUMN - 2 * MARGIN_HORIZONTAL;
  protected static final int WIDTH_BUTTON = 70;
  private static final int WIDTH_FIRST_TIME_LABEL = WIDTH_BACKGROUND_IMAGE - 2 * MARGIN_HORIZONTAL;
  
  protected static final int HEIGHT_FIELD = 32;
  
  protected static final int TOP_INSET_LARGE = 25;
  protected static final int TOP_INSET_MEDIUM = 15;
  protected static final int TOP_INSET_REGULAR = 10;
  protected static final int TOP_INSET_SMALL = 5;
  
  protected static final String COLOR_PATTERN = "<html><p style=\"color:{0};\">{1}</p>";
  
  protected static final String COLOR_GREY = "#888888";
  protected static final String COLOR_ARSENIC600 = "#4D6378";
  
  private static final String COPYRIGHT_SYMBOL = "\u00A9 ";
  
  private static final String AC_HELP = "help";
  
  private static final Set<Integer> MODES = new HashSet<>();
  
  static
  {
    MODES.add(MODE_WORKSPACE);
    MODES.add(MODE_SERVER);
    MODES.add(MODE_REMOTE_SERVER);
    MODES.add(MODE_KERBEROS);
  }
  
  public static final int OK = 0;
  public static final int CANCEL = 1;
  
  private static final String WORKSPACE_NAME_REGEX = "[^/?*:;{}\\\\]+";
  
  private JLabel inviteLogIn;
  private final JComboBox usernameField = new JComboBox();
  private final JPasswordField passwordField = new JPasswordField();
  private final JButton loginButton = new JButton();
  private final JLabel firstTimeLabel = new JLabel();
  
  private List<String> workspaces;
  
  private int result = CANCEL;
  private final int mode;
  
  protected SplashAuthDialog(int mode)
  {
    super(ComponentHelper.getMainFrame().getFrame(), true);
    
    if (!MODES.contains(mode))
      throw new IllegalArgumentException("Unknown mode: " + mode);
    
    this.mode = mode;
    
    if (OSDetector.isUnix())
    {
      addWindowListener(new WindowAdapter()
      {
        @Override
        public void windowOpened(WindowEvent e)
        {
          linuxPasswordFocusWorkaround();
        }
      });
    }
  }
  
  protected void initView()
  {
    setUndecorated(true);
    
    workspaces = Client.getWorkspaceManager().getWorkspaceNames();
    
    for (String workspaceName : workspaces)
      usernameField.addItem(workspaceName);
    
    final JPanel graphicsPane = new JPanel();
    graphicsPane.setOpaque(false);
    graphicsPane.setLayout(new GridBagLayout());
    
    final GridBagConstraints c = getGridBagConstraints();
    final JLabel background = new JLabel();
    background.setIcon(ResourceManager.getImageIcon(OtherIcons.SA_BACKGROUND));
    if (background.getIcon().getIconWidth() != WIDTH_BACKGROUND_IMAGE)
      Log.CORE.warn("The background image width of the authentication dialog does not match the layout; " +
          "expected " + WIDTH_BACKGROUND_IMAGE + " pixels, given " + background.getIcon().getIconWidth() + " pixels. " +
          "As a result, components can be positioned incorrectly on the dialog.");
    graphicsPane.add(background, c);
    
    final JPanel contentPanel = new JPanel(new GridBagLayout());
    contentPanel.setOpaque(false);
    
    final JLabel version = new JLabel(MessageFormat.format(COLOR_PATTERN, COLOR_GREY,
        (Cres.get().getString("productClient") + " v" + SoftwareVersion.getCurrentVersionAndBuild())));
    final JLabel copyright = new JLabel(MessageFormat.format(COLOR_PATTERN, COLOR_GREY,
        (COPYRIGHT_SYMBOL + Cres.get().getString("copyright"))));
    
    final String inviteLoginText = mode == MODE_WORKSPACE ? Pres.get().getString("mEnterWorkspaceAndPwd")
        : Pres.get().getString("mEnterUsernameAndPwd");
    inviteLogIn = new JLabel(MessageFormat.format(COLOR_PATTERN, COLOR_ARSENIC600, ("<b>" + inviteLoginText + "</b>")));
    
    final JLabel username = new JLabel(mode == MODE_WORKSPACE ? ("<html>" + Pres.get().getString("dlgAuthWorkspace") + "</html>")
        : Pres.get().getString("dlgAuthUsername"));
    setDimensions(username, WIDTH_LABEL_COLUMN, HEIGHT_FIELD);
    final JLabel password = new JLabel(Pres.get().getString("dlgAuthPassword"));
    setDimensions(password, WIDTH_LABEL_COLUMN, HEIGHT_FIELD);
    
    setCommonFields();
    
    final JButton cancelButton = new JButton();
    cancelButton.setBorder(BorderFactory.createEmptyBorder());
    setDimensions(cancelButton, WIDTH_BUTTON, HEIGHT_FIELD);
    cancelButton.setText(Cres.get().getString("cancel"));
    
    final JButton helpButton = new JButton(Pres.get().getString("dlgAuthHelp"));
    helpButton.setBorder(BorderFactory.createEmptyBorder());
    setDimensions(helpButton, WIDTH_BUTTON, HEIGHT_FIELD);
    helpButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    helpButton.setBorderPainted(false);
    helpButton.setOpaque(false);
    
    final KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
    contentPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(saveKeyStroke, AC_HELP);
    contentPanel.getActionMap().put(AC_HELP, new AbstractAction()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        helpButton.doClick();
      }
    });
    
    addListeners();
    
    cancelButton.addActionListener(e -> {
      result = CANCEL;
      dispose();
    });
    
    helpButton.addActionListener(e -> ClientUtils.showHelp(mode == MODE_WORKSPACE ? Docs.CL_STARTUP : Docs.CLIENT));
    
    final JPanel buttonPanel = getButtonPanel(cancelButton, helpButton);
    
    initView(contentPanel, c, version, copyright,
        username, password, buttonPanel);
    
    final JLayeredPane lp = getLayeredPane();
    
    lp.add(graphicsPane, Integer.valueOf(1));
    
    setSize(background.getIcon().getIconWidth(), background.getIcon().getIconHeight());
    setMinimumSize(new Dimension(background.getIcon().getIconWidth(), background.getIcon().getIconHeight()));
    graphicsPane.setBounds(0, 0, background.getIcon().getIconWidth(), background.getIcon().getIconHeight());
    
    contentPanel.setBounds(20, MARGIN_VERTICAL, background.getIcon().getIconWidth() - MARGIN_HORIZONTAL,
        background.getIcon().getIconHeight() - MARGIN_VERTICAL);
    lp.add(contentPanel, Integer.valueOf(2));
    
    validate();
  }
  
  private void addListeners()
  {
    addWindowListener(new WindowAdapter()
    {
      @Override
      public void windowActivated(WindowEvent e)
      {
        if (usernameField.getSelectedIndex() != -1)
        {
          passwordField.requestFocusInWindow();
        }
      }
    });
    
    loginButton.addActionListener(e -> {
      final String userName = getUsername();
      if (userName != null && !userName.matches(WORKSPACE_NAME_REGEX))
      {
        JOptionPane.showMessageDialog(SplashAuthDialog.this, Pres.get().getString("dlgInvalidWorkspaceName"));
        return;
      }
      result = OK;
      dispose();
    });
    
    passwordField.getDocument().addDocumentListener(new DocumentListener()
    {
      @Override
      public void insertUpdate(DocumentEvent e)
      {
        updateDisabled();
      }
      
      @Override
      public void removeUpdate(DocumentEvent e)
      {
        updateDisabled();
      }
      
      @Override
      public void changedUpdate(DocumentEvent e)
      {
        updateDisabled();
      }
      
      private void updateDisabled()
      {
        loginButton.setEnabled(passwordField.getPassword().length > 0);
      }
    });
  }
  
  protected List<String> getWorkspaces()
  {
    return Collections.unmodifiableList(workspaces);
  }
  
  private JPanel getButtonPanel(JButton cancelButton, JButton helpButton)
  {
    final JPanel buttonPanel = new JPanel();
    buttonPanel.setBorder(BorderFactory.createEmptyBorder());
    setDimensions(buttonPanel, WIDTH_INNER_PANEL, HEIGHT_FIELD);
    buttonPanel.setOpaque(false);
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    
    loginButton.setBorder(BorderFactory.createEmptyBorder());
    setDimensions(loginButton, WIDTH_BUTTON, HEIGHT_FIELD);
    buttonPanel.add(loginButton);
    
    buttonPanel.add(Box.createHorizontalStrut(WIDTH_LABEL_COLUMN - WIDTH_BUTTON));
    
    buttonPanel.add(cancelButton);
    
    buttonPanel.add(Box.createHorizontalStrut(WIDTH_WIDE_FIELD - 2 * WIDTH_BUTTON));
    
    buttonPanel.add(helpButton);
    
    getRootPane().setDefaultButton(loginButton);
    
    return buttonPanel;
  }
  
  protected void setDimensions(JComponent component, int width, int height)
  {
    component.setMinimumSize(new Dimension(width, height));
    component.setPreferredSize(new Dimension(width, height));
    component.setMaximumSize(new Dimension(width, height));
  }
  
  private void setCommonFields()
  {
    setDimensions(usernameField, WIDTH_WIDE_FIELD, HEIGHT_FIELD);
    usernameField.setEditable(true);
    
    setDimensions(passwordField, WIDTH_WIDE_FIELD, HEIGHT_FIELD);
    passwordField.setText("");
    
    loginButton.setEnabled(false);
    loginButton.setText(Pres.get().getString("dlgAuthLogIn"));
    
    final String firstTimeLabelText = mode == MODE_WORKSPACE ? Pres.get().getString("wsFirstTime")
        : Pres.get().getString("dlgAuthDefaultCredentials");
    firstTimeLabel.setText(MessageFormat.format(COLOR_PATTERN, COLOR_ARSENIC600, firstTimeLabelText));
    
    setDimensions(firstTimeLabel, WIDTH_FIRST_TIME_LABEL, firstTimeLabel.getPreferredSize().height * 3 / 2);
  }
  
  private GridBagConstraints getGridBagConstraints()
  {
    GridBagConstraints c = new GridBagConstraints();
    
    c.fill = GridBagConstraints.BOTH;
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 1;
    c.weighty = 1;
    
    return c;
  }
  
  protected abstract void initView(JPanel contentPanel, GridBagConstraints c, JLabel version, JLabel copyright,
      JLabel username, JLabel password, JPanel buttonPanel);
  
  protected void initComponentPosition(JComponent component, GridBagConstraints constraints, JPanel contentPanel,
      Integer gridx, Integer gridy, Integer gridwidth, Integer gridheight, Integer fill,
      Integer weightx, Integer weighty, Integer topInset, Integer leftInset,
      Integer bottomInset, Integer rightInset, Integer anchor)
  {
    constraints.gridx = gridx != null ? gridx : constraints.gridx;
    constraints.gridy = gridy != null ? gridy : constraints.gridy;
    constraints.gridwidth = gridwidth != null ? gridwidth : constraints.gridwidth;
    constraints.gridheight = gridheight != null ? gridheight : constraints.gridheight;
    constraints.fill = fill != null ? fill : constraints.fill;
    constraints.weightx = weightx != null ? weightx : constraints.weightx;
    constraints.weighty = weighty != null ? weighty : constraints.weighty;
    constraints.insets = new Insets(topInset, leftInset, bottomInset, rightInset);
    constraints.anchor = anchor != null ? anchor : constraints.anchor;
    contentPanel.add(component, constraints);
  }
  
  private void linuxPasswordFocusWorkaround()
  {
    // HACK: Opens and closes temporary dialog window to fix issue with rejecting to gain focus in dialog's password field on Linux
    final JDialog tmpDialog = new JDialog(this);
    
    tmpDialog.setVisible(true);
    
    final Timer timer = new Timer(50, e -> SwingUtilities.invokeLater(() -> {
      tmpDialog.setVisible(false);
      tmpDialog.dispose();
    }));
    
    timer.setRepeats(false);
    timer.start();
  }
  
  @Override
  public int run()
  {
    super.run();
    return result;
  }
  
  @Override
  protected void close()
  {
    result = CANCEL;
    super.close();
  }
  
  public JLabel getInviteLogIn()
  {
    return inviteLogIn;
  }
  
  public abstract String getIpAddress();
  
  public abstract String getPort();
  
  protected JComboBox getUsernameField()
  {
    return usernameField;
  }
  
  public String getUsername()
  {
    return (String) usernameField.getSelectedItem();
  }
  
  public void setUsername(String username)
  {
    usernameField.setSelectedItem(username);
  }
  
  protected JPasswordField getPasswordField()
  {
    return passwordField;
  }
  
  public String getPassword()
  {
    return String.valueOf(passwordField.getPassword());
  }
  
  protected JButton getLoginButton()
  {
    return loginButton;
  }
  
  protected JLabel getFirstTimeLabel()
  {
    return firstTimeLabel;
  }
}
