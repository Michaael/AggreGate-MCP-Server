package com.tibbo.aggregate.client.gui.dialog;

import java.awt.*;
import javax.swing.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.common.protocol.*;
import com.tibbo.aggregate.common.util.*;

public class RemoteServerSplashAuthDialog extends SplashAuthDialog
{
  private static final int WIDTH_IP_ADDRESS_FIELD = 130;
  private static final int WIDTH_PORT_FIELD = WIDTH_BUTTON;
  private static final int WIDTH_PORT_LABEL = 34;
  
  private static final int STRUT_PORT = 10;
  
  private final JTextField ipAddressField = new JTextField();
  private final JTextField portField = new JTextField();
  
  private String serverAddress;
  private Integer serverPort;
  
  protected RemoteServerSplashAuthDialog(int mode)
  {
    super(mode);
  }
  
  public RemoteServerSplashAuthDialog(int mode, String address, Integer port)
  {
    super(mode);
    
    setServerAddressAndPort(address, port);
    
    initView();
  }
  
  @Override
  protected void initView(JPanel contentPanel, GridBagConstraints c, JLabel version, JLabel copyright,
      JLabel username, JLabel password, JPanel buttonPanel)
  {
    initFieldsPosition(contentPanel, c, version, copyright, username, password, buttonPanel);
  }
  
  private JPanel getIpAddressAndPortPanel()
  {
    final JLabel ipAddress = new JLabel(Pres.get().getString("dlgAuthIpAddress"));
    setDimensions(ipAddress, WIDTH_LABEL_COLUMN, HEIGHT_FIELD);
    final JLabel port = new JLabel(Pres.get().getString("dlgAuthPort"));
    setDimensions(port, WIDTH_PORT_LABEL, HEIGHT_FIELD);
    
    setIpAddressAndPortFields();
    
    final JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createEmptyBorder());
    setDimensions(panel, WIDTH_INNER_PANEL, HEIGHT_FIELD);
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    
    panel.add(ipAddress);
    panel.add(ipAddressField);
    panel.add(Box.createHorizontalStrut(WIDTH_INNER_PANEL -
        (WIDTH_LABEL_COLUMN + WIDTH_IP_ADDRESS_FIELD + WIDTH_PORT_LABEL + STRUT_PORT + WIDTH_PORT_FIELD)));
    panel.add(port);
    panel.add(Box.createHorizontalStrut(STRUT_PORT));
    panel.add(portField);
    
    return panel;
  }
  
  private void setIpAddressAndPortFields()
  {
    if (!StringUtils.isEmpty(serverAddress) && serverPort != null)
    {
      if (serverAddress.equals(RemoteServer.DEFAULT_ADDRESS) && serverPort.equals(RemoteServer.DEFAULT_PORT))
      {
        ipAddressField.setEditable(true);
        ipAddressField.setText(serverAddress);
        portField.setEditable(true);
      }
      else
      {
        ipAddressField.setEditable(false);
        ipAddressField.setEnabled(false);
        ipAddressField.setText(serverAddress);
        portField.setEditable(false);
        portField.setEnabled(false);
      }
      portField.setText(Integer.toString(serverPort));
    }
    else
    {
      ipAddressField.setEditable(true);
      portField.setEditable(true);
    }
    
    setDimensions(ipAddressField, WIDTH_IP_ADDRESS_FIELD, HEIGHT_FIELD);
    
    setDimensions(portField, WIDTH_PORT_FIELD, HEIGHT_FIELD);
  }
  
  private void initFieldsPosition(JPanel contentPanel, GridBagConstraints c, JLabel version, JLabel copyright,
      JLabel username, JLabel password, JPanel buttonPanel)
  {
    initHeaderFieldsPosition(contentPanel, c, version, copyright);
    
    initComponentPosition(username, c, contentPanel, 0, 4, 1, 1,
        null, null, null, TOP_INSET_REGULAR, 0, 0, 0, null);
    
    initComponentPosition(getUsernameField(), c, contentPanel, 1, 4, 3, 1,
        null, null, null, TOP_INSET_REGULAR, 0, 0, 0, null);
    
    initComponentPosition(password, c, contentPanel, 0, 5, 1, 1,
        null, null, null, TOP_INSET_REGULAR, 0, 0, 0, null);
    
    initComponentPosition(getPasswordField(), c, contentPanel, 1, 5, 3, 1,
        null, null, null, TOP_INSET_REGULAR, 0, 0, 0, null);
    
    initComponentPosition(getFirstTimeLabel(), c, contentPanel, 0, 6, 4, 1,
        null, null, null, TOP_INSET_MEDIUM, 0, 0, 0, null);
    
    initComponentPosition(buttonPanel, c, contentPanel, 0, 7, 4, 1,
        null, null, null, TOP_INSET_LARGE, 0, 0, 0, null);
  }
  
  protected void initHeaderFieldsPosition(JPanel contentPanel, GridBagConstraints c, JLabel version, JLabel copyright)
  {
    initComponentPosition(version, c, contentPanel, 0, 0, 4, 1,
        null, 0, 0, 0, 0, 0, 0, null);
    
    initComponentPosition(copyright, c, contentPanel, 0, 1, 4, 1,
        null, null, null, 0, 0, 0, 0, null);
    
    initComponentPosition(getInviteLogIn(), c, contentPanel, 0, 2, 4, 1,
        null, null, null, TOP_INSET_MEDIUM, 0, 0, 0, null);
    
    initComponentPosition(getIpAddressAndPortPanel(), c, contentPanel, 0, 3, 4, 1,
        null, null, null, TOP_INSET_MEDIUM, 0, 0, 0, null);
  }
  
  protected void setServerAddressAndPort(String serverAddress, Integer serverPort)
  {
    this.serverAddress = serverAddress;
    this.serverPort = serverPort;
  }
  
  public String getIpAddress()
  {
    return ipAddressField.getText();
  }
  
  public String getPort()
  {
    return portField.getText();
  }
}
