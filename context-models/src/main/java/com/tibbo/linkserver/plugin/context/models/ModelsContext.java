package com.tibbo.linkserver.plugin.context.models;

import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.tibbo.aggregate.common.Cres;
import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.action.ActionResult;
import com.tibbo.aggregate.common.action.ActionUtils;
import com.tibbo.aggregate.common.action.ServerAction;
import com.tibbo.aggregate.common.action.ServerActionCommandProcessor;
import com.tibbo.aggregate.common.action.ServerActionDefinition;
import com.tibbo.aggregate.common.action.ServerActionInput;
import com.tibbo.aggregate.common.binding.DefaultBindingProcessor;
import com.tibbo.aggregate.common.binding.EvaluationOptions;
import com.tibbo.aggregate.common.binding.ExtendedBinding;
import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.context.Contexts;
import com.tibbo.aggregate.common.context.FunctionDefinition;
import com.tibbo.aggregate.common.context.RequestController;
import com.tibbo.aggregate.common.context.VariableDefinition;
import com.tibbo.aggregate.common.context.loader.BaseContextLoader;
import com.tibbo.aggregate.common.data.User;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.FieldFormat;
import com.tibbo.aggregate.common.datatable.SimpleDataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.datatable.field.DataFieldFormat;
import com.tibbo.aggregate.common.datatable.field.StringFieldFormat;
import com.tibbo.aggregate.common.event.EventLevel;
import com.tibbo.aggregate.common.security.ServerPermissionChecker;
import com.tibbo.aggregate.common.util.Docs;
import com.tibbo.aggregate.common.util.Icons;
import com.tibbo.aggregate.common.util.ImportExportHelper;
import com.tibbo.aggregate.common.util.TimeHelper;
import com.tibbo.linkserver.Lsres;
import com.tibbo.linkserver.Server;
import com.tibbo.linkserver.context.DefaultResourceBuilder;
import com.tibbo.linkserver.context.EditableChildContext;
import com.tibbo.linkserver.context.EditableChildrenContext;
import com.tibbo.linkserver.context.RepositoryManager;
import com.tibbo.linkserver.event.EventHelper;
import com.tibbo.linkserver.user.UserContext;
import com.tibbo.linkserver.util.ServerImportExportHelper;

public class ModelsContext extends EditableChildrenContext
{
  private final UserContext userContext;

  public static final String V_DEVICE_IMAGES = "deviceImages";
  private static final String VF_NAME = "name";
  private static final String VF_IMAGE = "image";

  public static final String M_DEVICE_IMAGES = "deviceImages";
  public static final String DEVICE_IMAGES_TABLE = "images.tbl";

  public static final long DEVICE_IMAGES_CACHE_TIME = 10 * TimeHelper.SECOND_IN_MS;

  static
  {
    String pattern = ContextUtils.modelsContextPath(ContextUtils.USERNAME_PATTERN);
    RepositoryManager.get().add(new DefaultResourceBuilder(5, pattern, M_DEVICE_IMAGES, Cres.get().getString("model"), Cres.get().getString("devices"), Cres.get().getString("deviceImages"), true)
    {
      @Override
      public void build(String username) throws ContextException
      {
        ModelManager modelManager = ModelManager.get();
        Model model = new Model(getName(), getDescription());
        model.setType(Model.TYPE_ABSOLUTE);

        ModelContext modelContext = modelManager.create(username, model);
        TableFormat deviceImagesTF = new TableFormat();

        FieldFormat name = FieldFormat.create(VF_NAME, FieldFormat.STRING_FIELD, Cres.get().getString("name"));
        deviceImagesTF.addField(name);

        FieldFormat image = FieldFormat.create(VF_IMAGE, FieldFormat.DATA_FIELD, Cres.get().getString("image"));
        image.setEditor(DataFieldFormat.EDITOR_IMAGE);
        final String editorOptions = DataFieldFormat.encodeTextEditorOptions(StringFieldFormat.TEXT_EDITOR_MODE_XML, "", null, Collections.singletonList("svg"));
        image.setEditorOptions(editorOptions);
        deviceImagesTF.addField(image);

        VariableDefinition vDeviceImages = new VariableDefinition(V_DEVICE_IMAGES, deviceImagesTF, true, true, Cres.get().getString("deviceImages"));
        vDeviceImages.setRemoteCacheTime(DEVICE_IMAGES_CACHE_TIME);
        modelManager.addVariable(modelContext, vDeviceImages);

        DataTable data = modelContext.getVariable(V_DEVICE_IMAGES, modelContext.getCallerController());
        ServerImportExportHelper importHelper = new ServerImportExportHelper(new ServerActionCommandProcessor(new ServerAction()
        {

          @Override
          protected ActionResult execute(ServerActionInput parameters) throws ContextException
          {
            return new ActionResult()
            {
              @Override
              public boolean isSuccessful()
              {
                return true;
              }

              @Override
              public DataTable getResult() {
                return null;
              }
            };
          }
        }));

        InputStream inputStream;
        try
        {
          inputStream = this.getClass().getResourceAsStream(DEVICE_IMAGES_TABLE);
          BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

          String readLine;
          String line = "";
          while ((readLine = in.readLine()) != null)
          {
            line += readLine + "\n";
          }

          data = importHelper.importDataTable(new SimpleDataTable(deviceImagesTF), modelContext.getContextManager(), ImportExportHelper.TYPE_NATIVE, line.getBytes(StandardCharsets.UTF_8));
          inputStream.close();
        }
        catch (Exception ex)
        {
          Log.MODELS.warn(ex.getMessage(), ex);
        }
        modelContext.setVariable(V_DEVICE_IMAGES, modelContext.getCallerController(), data);

      }
    });
  }

  public ModelsContext(UserContext userContext)
  {
    super(Contexts.CTX_MODELS, Cres.get().getString("model"), ModelContext.class, Model.FORMAT);
    this.userContext = userContext;
    setDescription(Lres.get().getString("models"));
  }

  @Override
  public void setupMyself() throws ContextException
  {
    super.setupMyself();

    addCreateAction(true, null, Docs.LS_MODELS_PROPERTIES);

    allowGrouping(Lres.get().getString("modelGroups"));

    setIconId(Icons.ST_MODELS);

    addHelpAction(Docs.LS_MODELS);

    removeActionDefinition(A_CREATE_FROM_TEMPLATE);
    ServerActionDefinition createFromTemplate = new ServerActionDefinition(A_CREATE_FROM_TEMPLATE, CreateFromModelTemplateAction.class);
    createFromTemplate.setIconId(Icons.CM_MAKE_COPY);
    createFromTemplate.setDescription(Lsres.get().getString("ecCreateFromTemplate"));
    createFromTemplate.setAcceptedContextTypes(getChildType());
    createFromTemplate.setIndex(ActionUtils.INDEX_LOW);
    createFromTemplate.setPermissions(ServerPermissionChecker.getManagerPermissions());
    addActionDefinition(createFromTemplate);
  }

  @Override
  protected EditableChildContext buildChild(String cname, boolean readOnly, String type)
  {
    return new ModelContext(userContext, cname);
  }

  @Override
  public DataTable callFmakeCopy(FunctionDefinition def, CallerController caller, RequestController request, DataTable parameters) throws ContextException
  {
    String cname = parameters.rec().getString(FIF_MAKE_COPY_CONTEXT);
    ModelContext source = (ModelContext) getRoot().get(cname, caller);
    if (source == null)
    {
      throw new ContextException(Cres.get().getString("conNotAvail") + cname);
    }

    if (!ContextUtils.isDerivedFrom(source.getType(), getChildType()))
    {
      throw new ContextException("Type mismatch: " + getChildType() + " and " + source.getType());
    }

    Model oldModel = source.getModelFromVariable();

    if (oldModel.getType() != Model.TYPE_INSTANTIABLE)
    {
      return super.callFmakeCopy(def, caller, request, parameters);
    }
    String newName = parameters.rec().getString(FIF_MAKE_COPY_NAME);
    String newDescription = parameters.rec().getString(FIF_MAKE_COPY_DESCRIPTION);
    String newContainerType = parameters.rec().getString(FIF_MAKE_COPY_CONTAINER_TYPE);
    String newContainerTypeDescription = parameters.rec().getString(FIF_MAKE_COPY_CONTAINER_TYPE_DESCRIPTION);
    String newContainerName = parameters.rec().getString(FIF_MAKE_COPY_CONTAINER_NAME);
    Boolean enabled = parameters.rec().getBoolean(FIF_MAKE_COPY_ENABLED);

    Set<Context> targetContexts = source.getTargetContexts(oldModel);
    for (Context context : targetContexts)
    {
      String containerPathPrefix = context.getPath().isEmpty() ? "" : context.getPath() + ".";
      Context containerContext = get(containerPathPrefix + newContainerName, getContextManager().getCallerController());
      if (containerContext != null)
      {
        throw new ContextException(MessageFormat.format(Cres.get().getString("conChildExists"), newContainerName, containerContext.getParent()));
      }
    }

    EditableChildContext clone = makeCopy(source, newName, newDescription, newContainerType, newContainerTypeDescription, newContainerName, caller, enabled);

    DataTable copyData = getMakeCopyData(source, clone, caller, newName, newDescription);

    for (DataRecord rec : copyData)
    {
      if (rec.getString(VF_NAME).equals(EditableChildContext.V_CHILD_INFO))
      {
        DataTable fieldsTable = rec.getDataTable(FOF_COPY_DATA_FIELDS);
        Iterator<DataRecord> iterator = fieldsTable.iterator();
        while (iterator.hasNext())
        {
          String fieldName = iterator.next().getString(VF_NAME);
          if (fieldName.equals(FIF_MAKE_COPY_CONTAINER_NAME) || fieldName.equals(FIF_MAKE_COPY_CONTAINER_TYPE) || fieldName.equals(FIF_MAKE_COPY_CONTAINER_TYPE_DESCRIPTION))
          {
            iterator.remove();
          }
        }
        break;
      }
    }

    DataTable res = clone.callFunction(F_COPY, caller, copyData);

    EventHelper.fireInfoEvent(this, EventLevel.INFO, MessageFormat.format(Lsres.get().getString("ecCreatedCopy"), source.toString()));

    return res;
  }

  /**
   * Initiates the activation of model bindings. This method is intended for manual activation
   * of model bindings and should only be used during debugging mode.
   *
   * @throws ContextException If the context tree is not fully initialized yet.
   * @throws IllegalAccessError If the method is called outside of debug mode.
   */
  @VisibleForTesting
  public void evaluateAllModelBindings() throws ContextException
  {
    if (!Server.isDebugging())
    {
      throw new IllegalAccessError("Method unavailable outside of debug mode");
    }

    if (!getRoot().isSetupComplete())
    {
      throw new ContextException("Models bindings cannot be manually evaluated now, because context tree is not fully initialized yet");
    }

    CallerController caller = getRoot().getContextManager().getCallerController();
    BaseContextLoader baseContextLoader = new BaseContextLoader(getContextManager(), caller);

    List<Context> allContexts = baseContextLoader.load(Contexts.CTX_ROOT).getContexts();

    List<Context> models = allContexts.stream()
            .filter(con -> con.getName().equals(Contexts.CTX_MODELS)
                    && con.getParent().getName().equals(User.DEFAULT_ADMIN_USERNAME))
            .limit(1)
            .collect(toList());

    if (models.isEmpty() || models.get(0).getChildren(caller).isEmpty())
    {
      Log.MODELS.warn("No Models contexts found for testing");

      return;
    }

    List<Context> modelsChildren = models.get(0).getChildren();

    Log.MODELS.debug("Starting evaluation Models bindings");

    for (Context child : modelsChildren)
    {
      ModelProcessor modelProcessor = ((ModelContext) child).getProcessor(child.getPath());
      DefaultBindingProcessor bindingProcessor = modelProcessor.getBindingProcessor();

      List<ExtendedBinding> bindings = ((ModelBindingProvider)bindingProcessor.getProvider()).getBindings();

      if (bindings.isEmpty())
      {
        continue;
      }

      Log.MODELS.debug("Starting evaluation Model(" + child.getName() + ") bindings");

      for (ExtendedBinding binding : bindings)
      {
        try
        {
          modelProcessor.getBindingProcessor().evaluateBindingExpression(
                  EvaluationOptions.TEST,
                  binding.getBinding(),
                  binding.getEvaluationOptions());
        }
        catch (Exception ex)
        {
          Log.MODELS.warn("Failed to manually evaluate model("
                  + child.getName()
                  + ") binding("
                  + binding.getBinding()
                  + ")", ex);
        }
      }

      Log.MODELS.debug("Finished evaluating Model(" + child.getName() + ")  bindings");
    }

    Log.MODELS.debug("All Model bindings evaluated successfully for multiple models");
  }
}