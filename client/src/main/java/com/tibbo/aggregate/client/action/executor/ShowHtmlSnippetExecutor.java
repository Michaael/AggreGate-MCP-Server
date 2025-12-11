package com.tibbo.aggregate.client.action.executor;

import static com.tibbo.aggregate.common.context.Contexts.CTX_UTILITIES;
import static com.tibbo.aggregate.common.server.UtilitiesContextConstants.F_ACTION_EXECUTION_PARAMETERS;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.Element;
import javax.xml.parsers.*;

import com.tibbo.aggregate.client.gui.frame.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.*;
import org.xml.sax.*;

import com.tibbo.aggregate.client.action.*;
import com.tibbo.aggregate.client.operation.*;
import com.tibbo.aggregate.client.util.*;
import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.action.*;
import com.tibbo.aggregate.common.action.command.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.expression.*;
import com.tibbo.aggregate.common.util.*;
import com.tibbo.aggregate.component.*;

public class ShowHtmlSnippetExecutor extends AbstractCommandExecutor
{
  private static final String FRAME_PREFIX = "snippet_";
  private static final String EDITOR_PANE_TYPE = "text/html";
  
  public static final Map<String, HyperlinkEvent.EventType> JAVASCRIPT_EVENTS_TO_SWING = new HashMap<>();
  static
  {
    JAVASCRIPT_EVENTS_TO_SWING.put("onclick", HyperlinkEvent.EventType.ACTIVATED);
    JAVASCRIPT_EVENTS_TO_SWING.put("onmouseenter", HyperlinkEvent.EventType.ENTERED);
    JAVASCRIPT_EVENTS_TO_SWING.put("onmouseleave", HyperlinkEvent.EventType.EXITED);
  }
  
  private InternalFrame frame;
  private Evaluator evaluator;
  
  public ShowHtmlSnippetExecutor()
  {
    super(ActionUtils.CMD_SHOW_HTML_SNIPPET);
  }
  
  @Override
  public GenericActionResponse execute(Operation originator, final GenericActionCommand cmd)
  {
    final InvokeActionOperation iop = (originator instanceof InvokeActionOperation) ? (InvokeActionOperation) originator : null;
    
    final DataRecord rec = cmd.getParameters().rec();
    
    DataTable locationData = rec.getDataTable(ShowHtmlSnippet.CF_LOCATION);
    final WindowLocation location = locationData != null ? new WindowLocation(locationData.rec()) : null;
    
    DataTable dashboardData = rec.hasField(ShowHtmlSnippet.CF_DASHBOARD) ? rec.getDataTable(ShowHtmlSnippet.CF_DASHBOARD) : null;
    final DashboardProperties dashboard = dashboardData != null ? new DashboardProperties(dashboardData.rec()) : null;
    
    DataTable dhInfoData = rec.hasField(ShowHtmlSnippet.CF_DASHBOARDS_HIERARCHY_INFO) ? rec.getDataTable(ShowHtmlSnippet.CF_DASHBOARDS_HIERARCHY_INFO) : null;
    final DashboardsHierarchyInfo dhInfo = dhInfoData != null ? new DashboardsHierarchyInfo(dhInfoData.rec()) : null;
    
    String key = rec.getFormat().hasField(ShowHtmlSnippet.CF_KEY) ? rec.getString(ShowHtmlSnippet.CF_KEY) : null;
    final String frameKey = (key != null && !key.isEmpty()) ? key : ExecutionHelper.createFrameKey(location, FRAME_PREFIX + Util.descriptionToName(cmd.getTitle()));
    
    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        JComponent contents;
        try
        {
          int snippetType = rec.getInt(ShowHtmlSnippet.CF_TYPE);
          
          if (iop != null && iop.getConnector() != null)
          {
            final SnippetReferenceResolver snippetReferenceResolver = new SnippetReferenceResolver(iop.getConnector().getContextManager(), iop.getConnector().getCallerController());
            evaluator = new Evaluator(snippetReferenceResolver);
          }
          
          JEditorPane editorPane = null;
          if (snippetType == ShowHtmlSnippet.TYPE_FRAME)
          {
            editorPane = new JEditorPane(rec.getString(ShowHtmlSnippet.CF_URL));
          }
          else if (snippetType == ShowHtmlSnippet.TYPE_HTML || snippetType == ShowHtmlSnippet.TYPE_EXPRESSION)
          {
            String html = rec.getString(ShowHtmlSnippet.CF_HTML);

            Boolean checkHtmlValidity = rec.hasField(ShowHtmlSnippet.CF_CHECK_HTML_VALIDITY) ? rec.getBoolean(ShowHtmlSnippet.CF_CHECK_HTML_VALIDITY) : Boolean.TRUE;

            if (checkHtmlValidity)
              validateHtmlDocument(html);
            
            editorPane = new JEditorPane(EDITOR_PANE_TYPE, html);
            
            editorPane.addHyperlinkListener(new ExpressionsHyperlinkListener());
            editorPane.setEditable(false);
            
          }
          contents = new JScrollPane(editorPane);
        }
        catch (IOException e)
        {
          contents = new JLabel(e.getMessage());
        }
        
        // TODO helpId
        frame = InternalFrame.create(frameKey, cmd.getTitle(), new DefaultDmfComponent(contents), true, false, location, dashboard, Docs.CL_SYSTEM_TREE, null, dhInfo);
      }
    });
    
    return new GenericActionResponse(null, false, cmd.getRequestId());
  }
  
  @Override
  public void cancel()
  {
    if (frame != null)
    {
      ClientUtils.removeFrame(frame);
    }
  }
  
  private static class SnippetReferenceResolver extends DefaultReferenceResolver
  {
    public SnippetReferenceResolver(ContextManager contextManager, CallerController caller)
    {
      super();
      setContextManager(contextManager);
      setCallerController(caller);
    }
    
    @Override
    protected DataTable resolveEntity(Reference ref, Context con, EvaluationEnvironment environment) throws ContextException, SyntaxErrorException, EvaluationException
    {
      if (ref.getEntityType() == ContextUtils.ENTITY_ACTION)
      {
        DataTable parameters = new SimpleDataTable();
        
        if (!ref.getParameters().isEmpty())
        {
          final Context utilities = getContextManager().get(CTX_UTILITIES, getContextManager().getCallerController());
          final DataTable actionParameters = utilities.callFunction(F_ACTION_EXECUTION_PARAMETERS, con.getPath(), ref.getEntity());
          
          parameters = DataTableConstruction.constructTable(ref.getParameters(), actionParameters.getFormat(), getEvaluator(), environment);
        }
        
        InvokeActionOperation.invoke(ref.getEntity(), con, (RemoteConnector) null, parameters, null);
        
        return parameters;
      }
      
      return super.resolveEntity(ref, con, environment);
    }
  }
  
  private class ExpressionsHyperlinkListener implements HyperlinkListener
  {
    
    public final Map<Element, Map<HyperlinkEvent.EventType, String>> hyperLinksToActions = new HashMap<>();
    
    @Override
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
      if (ShowHtmlSnippetExecutor.this.evaluator == null)
        return;
      
      final Element hyperLink = e.getSourceElement();
      
      Map<HyperlinkEvent.EventType, String> actionsToExpressions = hyperLinksToActions.get(hyperLink);
      
      if (actionsToExpressions == null)
      {
        actionsToExpressions = createActionsToExpressions(hyperLink);
        hyperLinksToActions.put(hyperLink, actionsToExpressions);
      }
      
      final String expression = actionsToExpressions.get(e.getEventType());
      
      if (expression != null && !expression.isEmpty())
      {
        try
        {
          evaluator.evaluate(new Expression(expression));
        }
        catch (Exception ex)
        {
          Log.DASHBOARDS.warn("Error evaluating expression on hyperlink '" + e.getEventType() + "': " + ex.getMessage(), Log.DASHBOARDS.isDebugEnabled() ? ex : null);
        }
      }
    }
    
    private Map<HyperlinkEvent.EventType, String> createActionsToExpressions(Element hyperLink)
    {
      Map<HyperlinkEvent.EventType, String> actionsToExpressions = new HashMap<>();
      
      for (Enumeration<?> tags = hyperLink.getAttributes().getAttributeNames(); tags.hasMoreElements();)
      {
        final Object tag = tags.nextElement();
        if ("a".equals(tag.toString()))
        {
          
          final Object anchor = hyperLink.getAttributes().getAttribute(tag);
          if (anchor instanceof AttributeSet)
          {
            AttributeSet anchorAttributes = (AttributeSet) anchor;
            final Enumeration<?> anchorAttributeNames = anchorAttributes.getAttributeNames();
            while (anchorAttributeNames.hasMoreElements())
            {
              Object anchorAttribute = anchorAttributeNames.nextElement();
              
              final HyperlinkEvent.EventType eventType = JAVASCRIPT_EVENTS_TO_SWING.get(Objects.toString(anchorAttribute));
              
              if (eventType != null)
              {
                final Object attribute = anchorAttributes.getAttribute(anchorAttribute);
                
                final String expression = StringUtils.substringBetween(attribute.toString(), "<e>", "</e>");
                
                actionsToExpressions.put(eventType, expression);
              }
            }
          }
        }
      }
      return actionsToExpressions;
    }
  }
  
  private void validateHtmlDocument(String html)
  {
    try
    {
      html = replaceAction(html);
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);
      
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setErrorHandler(new ErrorHandler()
      {
        @Override
        public void warning(SAXParseException exception) throws SAXException
        {
          Log.DASHBOARDS.warn(exception.getMessage(), exception);
        }
        
        @Override
        public void error(SAXParseException exception) throws SAXException
        {
          throw exception;
        }
        
        @Override
        public void fatalError(SAXParseException exception) throws SAXException
        {
          throw exception;
        }
      });
      
      builder.parse(new ByteArrayInputStream(html.getBytes()));
    }
    catch (Exception ex)
    {
      Log.DASHBOARDS.warn(ex.toString(), ex);
      ClientUtils.showError(ComponentHelper.getMainFrame().getFrame(), Level.WARN, Cres.get().getString("error"), ex.toString(), ex);
    }
  }
  
  private String replaceAction(String source)
  {
    final Matcher matcher = ShowHtmlSnippet.EXPRESSION_PATTERN.matcher(source);
    
    StringBuffer stringBuffer = new StringBuffer();
    
    while (matcher.find())
    {
      matcher.appendReplacement(stringBuffer, "");
    }
    matcher.appendTail(stringBuffer);
    
    return stringBuffer.toString();
  }
}
