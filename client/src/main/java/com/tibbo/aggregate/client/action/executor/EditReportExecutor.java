package com.tibbo.aggregate.client.action.executor;

import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import com.tibbo.aggregate.client.*;
import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.data.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.report.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;
import org.w3c.dom.Element;
import org.w3c.dom.*;

public class EditReportExecutor extends AbstractCommandExecutor
{
  private static final String JASPERSOFT_STUDIO_INI = "Jaspersoft Studio.ini";
  private static final String JASPERSOFT_STUDIO_1251_INI = "Jaspersoft Studio_cp1251.ini";
  private static final String JASPERSOFT_STUDIO_WIN = "Jaspersoft Studio.exe";
  private static final String JASPERSOFT_STUDIO_WIN_LAUNCHER = "ReportLauncher.exe";
  private static final String JASPERSOFT_STUDIO_LINUX = "Jaspersoft Studio";
  
  private static final String REPORTS_FOLDER = JRDataTableDataSourceProvider.REPORTS_FOLDER;
  private static final String REPORT_EXTENSION = ".jrxml";
  private static final String TABLE_EXTENSION = ".tbl";
  private static final String TEMPLATE_FILE = "template.jrxml";
  private static final String DATA_FILE = JRDataTableDataSourceProvider.DATA_FILE;
  
  private static final String UTF_8 = "UTF-8";
  private static final String CP1251 = "CP1251";
  private static final String AGGREGATE_COMMONS_JAR = "aggregate-commons.jar";
  private static final String AGGREGATE_API_JAR = "aggregate-api.jar";
  
  private static final String ARCHITECTURE = System.getProperty("os.arch").endsWith("64") ? "64" : "32";
  private static final String JASPER_STUDIO_FOLDER = "jaspersoftstudio" + File.separator + (OSDetector.isWindows() ? "win_x" : "linux_x") + ARCHITECTURE;
  
  private static final String JASPER_STUDIO_CONFIG_FOLDER = System.getProperty("user.home") + File.separator + ".ag" + File.separator + "reports" + File.separator + "data";
  
  private static boolean running;
  
  public EditReportExecutor()
  {
    super(ActionUtils.CMD_EDIT_REPORT);
  }
  
  @Override
  public GenericActionResponse execute(Operation originator, GenericActionCommand cmd)
  {
    DataTable params = cmd.getParameters();
    String template = params.rec().getString(EditReport.CF_TEMPLATE);
    DataTable data = params.rec().getDataTable(EditReport.CF_DATA);
    
    if (ComponentHelper.isSteadyStateMode())
    {
      Toolkit.getDefaultToolkit().beep();
      return createResponseData(cmd, ActionUtils.RESPONSE_CLOSED, null);
    }
    
    return startReportEditor(cmd, template, data);
  }
  
  public GenericActionResponse startReportEditor(GenericActionCommand cmd, String template, DataTable data)
  {
    File templateFile = new File(getTemplatePath());
    File dataFile = new File(getDataPath());
    
    long initialTimestamp;
    
    if (running)
    {
      JOptionPane.showMessageDialog(ComponentHelper.getMainFrame(), Pres.get().getString("actionReportEditorRunning"), Cres.get().getString("error"), JOptionPane.ERROR_MESSAGE);
      return createResponseData(cmd, ActionUtils.RESPONSE_ERROR, null);
    }
    
    File file = new File(getFolder());
    
    running = true;
    
    DataTable parameters = cmd.getParameters();
    
    file.mkdirs();
    
    try
    {
      if (!file.exists())
      {
        throw new IOException("Can't create forder for reports: " + getFolder(), new Throwable());
      }
      
      try (OutputStream tos = new FileOutputStream(templateFile))
      {
        tos.write(template.getBytes(StringUtils.UTF8_CHARSET));
      }
      
      try (OutputStream dos = new FileOutputStream(dataFile))
      {
        dos.write(data.encode().getBytes(StringUtils.UTF8_CHARSET));
      }
      
      DataTable subreports = parameters.rec().getDataTable(EditReport.CF_SUBREPORTS);
      DataTable resources = parameters.rec().getDataTable(EditReport.CF_RESOURCES);
      
      createSubreportFiles(subreports);
      createResourcesFiles(resources);
      
      initialTimestamp = templateFile.lastModified();
      
      String editorExecutable = JASPER_STUDIO_FOLDER + File.separator + (OSDetector.isWindows() ? JASPERSOFT_STUDIO_WIN : JASPERSOFT_STUDIO_LINUX);
      
      File editorExecutableFile = new File(editorExecutable);
      
      if (!editorExecutableFile.exists())
      {
        JOptionPane.showMessageDialog(ComponentHelper.getMainFrame(), Pres.get().getString("actionReportEditorNotAvail") + editorExecutable, Cres.get().getString("error"), JOptionPane.ERROR_MESSAGE);
        return createResponseData(cmd, ActionUtils.RESPONSE_ERROR, null);
      }
      
      checkAndCorrectSettings();
      
      int exitCode = launchAndWaitReportEditor(editorExecutable);
      
      if (exitCode != 0 && initialTimestamp == templateFile.lastModified())
      {
        return createResponseData(cmd, ActionUtils.RESPONSE_CLOSED, null);
      }
      else
      {
        return createResponseData(cmd, ActionUtils.RESPONSE_SAVED, FileUtils.readTextFile(templateFile.getAbsolutePath(), StringUtils.UTF8_CHARSET));
      }
    }
    catch (Exception e)
    {
      Log.CONTEXT_ACTIONS.warn("Error starting report editor", e);
      
      return createResponseData(cmd, ActionUtils.RESPONSE_ERROR, null);
    }
    finally
    {
      running = false;
      try
      {
        org.apache.commons.io.FileUtils.cleanDirectory(file);
      }
      catch (IOException e)
      {
        Log.REPORTS.warn("Error while cleaning up temp reports folder: " + getFolder(), e);
      }
    }
  }
  
  private int launchAndWaitReportEditor(String editorExecutable) throws IOException, InterruptedException
  {
    String[] command;
    String templatePath = getTemplatePath();
    
    if (OSDetector.isWindows())
    {
      String launcherExecutable = JASPER_STUDIO_FOLDER + File.separator + JASPERSOFT_STUDIO_WIN_LAUNCHER;
      command = new String[] { launcherExecutable, editorExecutable, templatePath };
    }
    else
    {
      command = new String[] { editorExecutable, templatePath };
    }
    
    Process process = Runtime.getRuntime().exec(command);
    
    return process.waitFor();
  }
  
  protected void createSubreportFiles(DataTable subreports) throws IOException
  {
    for (DataRecord subreport : subreports)
    {
      String name = subreport.getString(EditReport.CF_SUBREPORTS_NAME);
      String template = subreport.getString(EditReport.CF_SUBREPORTS_TEMPLATE);
      DataTable data = subreport.getDataTable(EditReport.CF_SUBREPORTS_DATA);
      
      String path = getReportFilePath(name + REPORT_EXTENSION);
      
      try (OutputStream stream = new FileOutputStream(path))
      {
        stream.write(template.getBytes(StringUtils.UTF8_CHARSET));
      }
      
      String dataPath = getReportFilePath(name + TABLE_EXTENSION);
      try (OutputStream stream = new FileOutputStream(dataPath))
      {
        stream.write(data.encode().getBytes(StringUtils.UTF8_CHARSET));
      }
    }
  }
  
  protected void createResourcesFiles(DataTable resources) throws IOException
  {
    for (DataRecord resource : resources)
    {
      Data data = resource.getData(EditReport.CF_RESOURCES_DATA);
      String path = getReportFilePath(data.getName());
      try (OutputStream stream = new FileOutputStream(path))
      {
        stream.write(data.getData());
      }
    }
  }
  
  private String getFolder()
  {
    return ComponentHelper.getConfig().getDataDirectory() + REPORTS_FOLDER;
  }
  
  private String getTemplatePath()
  {
    return getReportFilePath(TEMPLATE_FILE);
  }
  
  private String getDataPath()
  {
    return getReportFilePath(DATA_FILE);
  }
  
  private String getReportFilePath(String fileName)
  {
    return getFolder() + File.separator + fileName;
  }
  
  public static GenericActionResponse createResponseData(GenericActionCommand cmd, String result, String template)
  {
    ActionUtils.checkResponseCode(result);
    
    DataTable resultTable = new SimpleDataTable(EditReport.RFT_EDIT_REPORT);
    DataRecord dr = resultTable.addRecord().addString(result);
    dr.addString(template);
    
    return new GenericActionResponse(resultTable, false, cmd.getRequestId());
  }
  
  private void checkAndCorrectSettings()
  {
    checkAndCorrectIniFile();
    checkAndCorrectClasspath();
  }
  
  private String getClientDir()
  {
    String path = "";
    try
    {
      path = new File(".").getCanonicalPath();
    }
    catch (IOException e)
    {
      Log.REPORTS.warn("Error while getting client directory path", e);
    }
    return path;
  }
  
  private void checkAndCorrectClasspath()
  {
    String classPathFile = JASPER_STUDIO_CONFIG_FOLDER + File.separator + "MyReports" + File.separator + ".classpath";
    String jarPath = getClientDir() + File.separator + JASPER_STUDIO_FOLDER + File.separator + "agg" + File.separator;
    
    try
    {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(new File(classPathFile));
      doc.getDocumentElement().normalize();
      NodeList classpathEntrys = doc.getElementsByTagName("classpathentry");
      ArrayList<Node> nodesToCheck = new ArrayList<>();
      
      for (int i = 0; i < classpathEntrys.getLength(); i++)
      {
        Node classpathEntry = classpathEntrys.item(i);
        Node kindNode = classpathEntry.getAttributes().getNamedItem("kind");
        if ("lib".equals(kindNode.getNodeValue()))
        {
          nodesToCheck.add(classpathEntry);
        }
      }
      
      ArrayList<String> jarsToAdd = new ArrayList<>();
      jarsToAdd.add(AGGREGATE_API_JAR);
      jarsToAdd.add(AGGREGATE_COMMONS_JAR);
      
      for (Node nodeToCheck : nodesToCheck)
      {
        Node pathNode = nodeToCheck.getAttributes().getNamedItem("path");
        String jar = new File(pathNode.getNodeValue()).getName();
        if (jarsToAdd.contains(jar))
        {
          pathNode.setNodeValue(jarPath + jar);
          jarsToAdd.remove(jar);
        }
      }
      
      for (String jarToAdd : jarsToAdd)
      {
        Element classpathentry = doc.createElement("classpathentry");
        classpathentry.setAttribute("kind", "lib");
        classpathentry.setAttribute("path", jarPath + jarToAdd);
        doc.getDocumentElement().appendChild(classpathentry);
      }
      
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(new File(classPathFile));
      transformer.transform(source, result);
    }
    catch (Exception e)
    {
      Log.REPORTS.warn("Error while modifying classpath file: " + classPathFile, e);
    }
  }
  
  private void checkAndCorrectIniFile()
  {
    String loadIniFile = JASPER_STUDIO_FOLDER + File.separator + JASPERSOFT_STUDIO_1251_INI;
    String writeIniFile = JASPER_STUDIO_FOLDER + File.separator + JASPERSOFT_STUDIO_INI;
    {
      if (!new File(loadIniFile).exists())
        loadIniFile = writeIniFile;
    }
    
    ArrayList<String> settings = loadFile(loadIniFile);
    
    modifyIni(settings);
    
    writeFile(writeIniFile, settings);
  }
  
  protected void writeFile(String iniFile, ArrayList<String> settings)
  {
    BufferedWriter bw;
    try
    {
      FileOutputStream fis = new FileOutputStream(iniFile);
      OutputStreamWriter isr = new OutputStreamWriter(fis, getIniFileEncoding());
      bw = new BufferedWriter(isr);
      
      for (String currentLine : settings)
      {
        bw.write(currentLine);
        bw.newLine();
      }
      
      bw.close();
    }
    catch (IOException e)
    {
      Log.REPORTS.warn("Error while writing properties file: " + iniFile, e);
    }
  }
  
  protected ArrayList<String> modifyIni(ArrayList<String> settings)
  {
    boolean dataPathFound = false;
    for (int i = 0; i < settings.size(); i++)
    {
      if (settings.get(i).equals("-data"))
      {
        settings.set(i + 1, JASPER_STUDIO_CONFIG_FOLDER);
      }
      else if (settings.get(i).equals("-vm"))
      {
        settings.set(i + 1, System.getProperty("java.home") + File.separator + "bin");
      }
      else if (settings.get(i).startsWith("-D" + JRDataTableDataSourceProvider.AGG_DATA_PATH))
      {
        settings.set(i, "-D" + JRDataTableDataSourceProvider.AGG_DATA_PATH + "=" + getFolder());
        dataPathFound = true;
      }
    }
    if (!dataPathFound)
    {
      settings.add("-D" + JRDataTableDataSourceProvider.AGG_DATA_PATH + "=" + getFolder());
    }
    
    return settings;
  }
  
  protected ArrayList<String> loadFile(String iniFile)
  {
    ArrayList<String> settings = new ArrayList<>();
    
    FileInputStream fis = null;
    BufferedReader br = null;
    
    try
    {
      fis = new FileInputStream(iniFile);
      br = new BufferedReader(new InputStreamReader(fis, getIniFileEncoding()));
      
      String currentLine;
      while ((currentLine = br.readLine()) != null)
      {
        settings.add(currentLine);
      }
    }
    catch (IOException e)
    {
      Log.REPORTS.warn("Error while reading properties file: " + iniFile, e);
    }
    finally
    {
      try
      {
        if (br != null)
          br.close();
        
        if (fis != null)
          fis.close();
      }
      catch (IOException e)
      {
        Log.REPORTS.warn("Error while closing properties file: " + iniFile, e);
      }
    }
    return settings;
  }
  
  private String getIniFileEncoding()
  {
    return OSDetector.isWindows() ? CP1251 : UTF_8;
  }
}