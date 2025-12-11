import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.nio.file.*;

import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.linkserver.module.*;
import com.tibbo.linkserver.util.ZipUtils;

public class %ScriptClassNamePattern% implements ModuleScript
{
  @Override
  public DataTable execute(ModuleScriptParameters parameters) throws Exception
  {
    ZipUtils.unzip(Arrays.asList("./macro/macro.zip"),true);
    return null;
  }
}
