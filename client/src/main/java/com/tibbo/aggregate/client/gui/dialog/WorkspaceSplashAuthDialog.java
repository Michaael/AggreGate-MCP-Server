package com.tibbo.aggregate.client.gui.dialog;

import java.awt.*;
import javax.swing.*;

public class WorkspaceSplashAuthDialog extends SplashAuthDialog
{
  public WorkspaceSplashAuthDialog(int mode)
  {
    super(mode);
    
    initView();
  }
  
  @Override
  protected void initView(JPanel contentPanel, GridBagConstraints c, JLabel version, JLabel copyright,
      JLabel username, JLabel password, JPanel buttonPanel)
  {
    initFieldsPosition(contentPanel, c, version, copyright,
        username, password, buttonPanel);
  }
  
  private void initFieldsPosition(JPanel contentPanel, GridBagConstraints c, JLabel version, JLabel copyright,
      JLabel username, JLabel password, JPanel buttonPanel)
  {
    initComponentPosition(version, c, contentPanel, 0, 0, 4, 1,
        null, 0, 0, 0, 0, 0, 0, null);
    
    initComponentPosition(copyright, c, contentPanel, 0, 1, 4, 1,
        null, null, null, 0, 0, 0, 0, null);
    
    initComponentPosition(getInviteLogIn(), c, contentPanel, 0, 2, 4, 1,
        null, null, null, TOP_INSET_LARGE, 0, 0, 0, null);
    
    initComponentPosition(username, c, contentPanel, 0, 3, 1, 1,
        null, null, null, TOP_INSET_MEDIUM, 0, 0, 0, null);
    
    initComponentPosition(getUsernameField(), c, contentPanel, 1, 3, 3, 1,
        null, null, null, TOP_INSET_MEDIUM, 0, 0, 0, null);
    
    initComponentPosition(password, c, contentPanel, 0, 4, 1, 1,
        null, null, null, TOP_INSET_REGULAR, 0, 0, 0, null);
    
    initComponentPosition(getPasswordField(), c, contentPanel, 1, 4, 3, 1,
        null, null, null, TOP_INSET_REGULAR, 0, 0, 0, null);
    
    if (getWorkspaces().size() != 0)
      getFirstTimeLabel().setText(" ");
    initComponentPosition(getFirstTimeLabel(), c, contentPanel, 0, 5, 4, 1,
        null, null, null, TOP_INSET_MEDIUM, 0, 0, 0, null);
    
    initComponentPosition(buttonPanel, c, contentPanel, 0, 6, 4, 1,
        null, null, null, TOP_INSET_LARGE, 0, 0, 0, null);
  }
  
  @Override
  public String getIpAddress()
  {
    return null;
  }
  
  @Override
  public String getPort()
  {
    return null;
  }
}
