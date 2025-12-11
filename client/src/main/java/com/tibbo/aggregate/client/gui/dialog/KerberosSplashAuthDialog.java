package com.tibbo.aggregate.client.gui.dialog;

import java.awt.*;
import java.text.*;
import javax.swing.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.common.util.*;

public class KerberosSplashAuthDialog extends RemoteServerSplashAuthDialog
{
  private final JCheckBox ssoCheckbox = new JCheckBox();
  
  public KerberosSplashAuthDialog(int mode, String address, Integer port)
  {
    super(mode);
    
    setServerAddressAndPort(address, port);
    
    initView();
  }
  
  @Override
  protected void initView(JPanel contentPanel, GridBagConstraints c, JLabel version, JLabel copyright,
      JLabel username, JLabel password, JPanel buttonPanel)
  {
    final JLabel sso = new JLabel(Pres.get().getString("dlgAuthKerberosSso"));
    setDimensions(sso, WIDTH_LABEL_COLUMN, HEIGHT_FIELD);
    
    setFields();
    
    addListenerToKerberosSsoCheckbox();
    
    initFieldsPosition(contentPanel, c, version, copyright,
        sso, username, password, buttonPanel);
  }
  
  private void setFields()
  {
    getInviteLogIn().setText(" ");
    
    getLoginButton().setEnabled(true);
    ssoCheckbox.setSelected(true);
    ssoCheckbox.setBorder(BorderFactory.createEmptyBorder());
    ssoCheckbox.setBorderPainted(false);
    
    getUsernameField().setSelectedIndex(-1);
    getUsernameField().setEnabled(false);
    getPasswordField().setEnabled(false);
    
    getFirstTimeLabel().setVisible(false);
  }
  
  private void addListenerToKerberosSsoCheckbox()
  {
    ssoCheckbox.addChangeListener(e -> {
      if (ssoCheckbox.isSelected())
      {
        getInviteLogIn().setText(" ");
        
        getUsernameField().setSelectedIndex(-1);
        getUsernameField().setEnabled(false);
        getPasswordField().setEnabled(false);
        getLoginButton().setEnabled(true);
      }
      else
      {
        getInviteLogIn().setText(MessageFormat.format(COLOR_PATTERN, COLOR_ARSENIC600,
            ("<b>" + Pres.get().getString("mEnterUsernameAndPwd") + "</b>")));
        
        getUsernameField().setSelectedIndex(0);
        getUsernameField().setEnabled(true);
        getPasswordField().setEnabled(true);
        getLoginButton().setEnabled(areAllRequiredFieldsFilled());
      }
    });
  }
  
  private boolean areAllRequiredFieldsFilled()
  {
    return !StringUtils.isEmpty(getUsername())
        && getPasswordField().getPassword().length != 0;
  }
  
  private void initFieldsPosition(JPanel contentPanel, GridBagConstraints c, JLabel version, JLabel copyright,
      JLabel kerberosSso, JLabel username, JLabel password, JPanel buttonPanel)
  {
    initHeaderFieldsPosition(contentPanel, c, version, copyright);
    
    initComponentPosition(kerberosSso, c, contentPanel, 0, 4, 1, 1,
        1, null, null, TOP_INSET_SMALL, 0, 0, 0, null);
    
    initComponentPosition(ssoCheckbox, c, contentPanel, 1, 4, 1, 1,
        null, null, null, TOP_INSET_SMALL, 0, 0, 0, null);
    
    initComponentPosition(username, c, contentPanel, 0, 5, 1, 1,
        null, null, null, TOP_INSET_SMALL, 0, 0, 0, null);
    
    initComponentPosition(getUsernameField(), c, contentPanel, 1, 5, 3, 1,
        null, null, null, TOP_INSET_SMALL, 0, 0, 0, null);
    
    initComponentPosition(password, c, contentPanel, 0, 6, 1, 1,
        null, null, null, TOP_INSET_REGULAR, 0, 0, 0, null);
    
    initComponentPosition(getPasswordField(), c, contentPanel, 1, 6, 3, 1,
        null, null, null, TOP_INSET_REGULAR, 0, 0, 0, null);
    
    initComponentPosition(buttonPanel, c, contentPanel, 0, 7, 4, 1,
        null, null, null, TOP_INSET_LARGE, 0, 0, 0, null);
  }
  
  public boolean isSso()
  {
    return ssoCheckbox.isSelected();
  }
}
