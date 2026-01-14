package com.tibbo.aggregate.mcp.tools.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.FunctionDefinition;
import com.tibbo.aggregate.common.context.FunctionImplementation;
import com.tibbo.aggregate.common.context.RequestController;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for creating a function in a context
 */
public class CreateFunctionTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_create_function";
    }
    
    @Override
    public String getDescription() {
        return "Create a function in a context. " +
               "⚠️ IMPORTANT: For Expression functions (type=1), " +
               "ALWAYS use aggregate_build_expression first to get correct format strings, " +
               "then aggregate_validate_expression to check syntax, " +
               "then call this tool. " +
               "Rules: inputFormat/outputFormat WITHOUT <<>>, expression WITH <<>> inside table().";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Context path");
        properties.set("path", path);
        
        ObjectNode functionName = instance.objectNode();
        functionName.put("type", "string");
        functionName.put("description", "Function name");
        properties.set("functionName", functionName);
        
        ObjectNode description = instance.objectNode();
        description.put("type", "string");
        description.put("description", "Function description (optional)");
        properties.set("description", description);
        
        ObjectNode group = instance.objectNode();
        group.put("type", "string");
        group.put("description", "Function group (optional)");
        properties.set("group", group);
        
        ObjectNode inputFormat = instance.objectNode();
        inputFormat.put("type", "string");
        inputFormat.put("description", "Input format as TableFormat string (optional). " +
               "⚠️ CRITICAL RULE for Expression functions (type=1): " +
               "inputFormat should NOT have <<>> brackets! " +
               "CORRECT: '<value1><E><value2><E>'. " +
               "INCORRECT: '<<value1><E><value2><E>>'. " +
               "Use aggregate_build_expression to generate correct formats.");
        properties.set("inputFormat", inputFormat);
        
        ObjectNode outputFormat = instance.objectNode();
        outputFormat.put("type", "string");
        outputFormat.put("description", "Output format as TableFormat string (optional). " +
               "⚠️ CRITICAL RULE for Expression functions (type=1): " +
               "outputFormat should NOT have <<>> brackets! " +
               "CORRECT: '<result><E>'. " +
               "INCORRECT: '<<result><E>>'. " +
               "Use aggregate_build_expression to generate correct formats.");
        properties.set("outputFormat", outputFormat);
        
        ObjectNode functionType = instance.objectNode();
        functionType.put("type", "integer");
        functionType.put("description", "Function type: 0=Java, 1=Expression, 2=Query (default: 0). " +
               "For Expression functions (type=1), use aggregate_build_expression first, " +
               "then aggregate_validate_expression before calling this tool.");
        properties.set("functionType", functionType);
        
        ObjectNode expression = instance.objectNode();
        expression.put("type", "string");
        expression.put("description", "Expression for Expression type functions (type=1). " +
               "⚠️ CRITICAL RULE: expression MUST have <<>> brackets INSIDE table() function! " +
               "CORRECT: 'table(\"<<result><E>>\", ({value1} + {value2}) / 2)'. " +
               "INCORRECT: 'table(\"<result><E>\", ...)' (missing <<>>). " +
               "Use aggregate_build_expression to generate correct expression syntax.");
        properties.set("expression", expression);
        
        ObjectNode query = instance.objectNode();
        query.put("type", "string");
        query.put("description", "Query for Query type functions (type=2)");
        properties.set("query", query);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("functionName"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("functionName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and functionName parameters are required"
            );
        }
        
        String connectionKey = params.has("connectionKey") ? params.get("connectionKey").asText() : null;
        ServerConnection connection = connectionKey != null 
            ? connectionManager.getServerConnection(connectionKey)
            : connectionManager.getDefaultServerConnection();
        
        if (connection == null || !connection.isLoggedIn()) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONNECTION_ERROR,
                "Not connected or not logged in"
            );
        }
        
        try {
            String path = ContextPathParser.parsePath(params.get("path").asText());
            String functionName = params.get("functionName").asText();
            String description = params.has("description") ? params.get("description").asText() : null;
            String group = params.has("group") ? params.get("group").asText() : "custom";
            
            // Determine function type: 0=Java, 1=Expression, 2=Query
            int functionType = params.has("functionType") ? params.get("functionType").asInt() : 
                com.tibbo.aggregate.common.server.ModelContextConstants.FUNCTION_TYPE_JAVA;
            String expression = params.has("expression") ? params.get("expression").asText() : null;
            String query = params.has("query") ? params.get("query").asText() : null;
            
            if (connection.getContextManager() == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONNECTION_ERROR,
                    "Context manager is not available"
                );
            }
            
            Context context = connection.executeWithTimeout(() -> {
                Context ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000);
            
            // Parse input and output formats if provided
            // If not provided, create default formats for calculator function
            TableFormat inputFormat = null;
            TableFormat outputFormat = null;
            
            if (params.has("inputFormat") && !params.get("inputFormat").isNull()) {
                try {
                    // Try to parse as encoded format string
                    String formatStr = params.get("inputFormat").asText();
                    if (formatStr != null && !formatStr.isEmpty()) {
                        // Check if format has double brackets (<<...>>) - this is the correct format for Expression functions
                        if (formatStr.startsWith("<<") && formatStr.endsWith(">>")) {
                            // Remove outer brackets and parse inner format
                            String innerFormat = formatStr.substring(2, formatStr.length() - 2);
                            // Use TableFormat constructor with format string and encoding settings
                            inputFormat = new TableFormat(innerFormat, new com.tibbo.aggregate.common.datatable.encoding.ClassicEncodingSettings(false), false);
                        } else {
                            // Standard format parsing - use (0, Integer.MAX_VALUE) for functions
                            inputFormat = new TableFormat(0, Integer.MAX_VALUE, formatStr);
                        }
                    }
                } catch (Exception e) {
                    throw new McpException(
                        com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                        "Invalid inputFormat: " + e.getMessage()
                    );
                }
            }
            
            // Create default formats if not provided
            if (inputFormat == null) {
                if ("calculate".equals(functionName) || "calculator".equals(functionName)) {
                    // Default format for calculator: action (String), arg1 (Double), arg2 (Double)
                    inputFormat = new TableFormat(0, Integer.MAX_VALUE);
                    inputFormat.addField("<action><S><D=Action>");
                    inputFormat.addField("<arg1><E><D=Argument 1>");
                    inputFormat.addField("<arg2><E><D=Argument 2>");
                } else if ("addNumbers".equals(functionName) || functionType == com.tibbo.aggregate.common.server.ModelContextConstants.FUNCTION_TYPE_EXPRESSION) {
                    // Default format for expression functions: arg1 (Double), arg2 (Double)
                    inputFormat = new TableFormat(0, Integer.MAX_VALUE);
                    inputFormat.addField("<arg1><E><D=Argument 1>");
                    inputFormat.addField("<arg2><E><D=Argument 2>");
                } else {
                    // Empty format by default
                    inputFormat = new TableFormat(0, Integer.MAX_VALUE);
                }
            }
            
            if (params.has("outputFormat") && !params.get("outputFormat").isNull()) {
                try {
                    String formatStr = params.get("outputFormat").asText();
                    if (formatStr != null && !formatStr.isEmpty()) {
                        // Check if format has double brackets (<<...>>) - this is the correct format for Expression functions
                        if (formatStr.startsWith("<<") && formatStr.endsWith(">>")) {
                            // Remove outer brackets and parse inner format
                            String innerFormat = formatStr.substring(2, formatStr.length() - 2);
                            // Use TableFormat constructor with format string and encoding settings
                            outputFormat = new TableFormat(innerFormat, new com.tibbo.aggregate.common.datatable.encoding.ClassicEncodingSettings(false), false);
                        } else {
                            // Standard format parsing - use (0, Integer.MAX_VALUE) for functions
                            outputFormat = new TableFormat(0, Integer.MAX_VALUE, formatStr);
                        }
                    }
                } catch (Exception e) {
                    throw new McpException(
                        com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                        "Invalid outputFormat: " + e.getMessage()
                    );
                }
            }
            
            // Create default output format if not provided
            if (outputFormat == null) {
                if ("calculate".equals(functionName) || "calculator".equals(functionName) || 
                    "addNumbers".equals(functionName) || functionType == com.tibbo.aggregate.common.server.ModelContextConstants.FUNCTION_TYPE_EXPRESSION) {
                    // Default format for calculator/expression output: result (Double)
                    outputFormat = new TableFormat(0, Integer.MAX_VALUE);
                    outputFormat.addField("<result><E><D=Result>");
                } else {
                    // Empty format by default
                    outputFormat = new TableFormat(0, Integer.MAX_VALUE);
                }
            }
            
            // Create function definition
            FunctionDefinition fd;
            if (description != null && group != null) {
                fd = new FunctionDefinition(functionName, inputFormat, outputFormat, description, group);
            } else if (description != null) {
                fd = new FunctionDefinition(functionName, inputFormat, outputFormat, description);
            } else {
                fd = new FunctionDefinition(functionName, inputFormat, outputFormat);
            }
            
            // Create function implementation based on type
            final String funcName = functionName;
            final TableFormat finalInputFormat = inputFormat;
            final TableFormat finalOutputFormat = outputFormat;
            final String finalDescription = description;
            final String finalGroup = group;
            final int finalFunctionType = functionType;
            final String finalExpression = expression; // May be auto-generated for Expression type
            final String finalQuery = query;
            
            // For Java type (0), set implementation
            if (functionType == com.tibbo.aggregate.common.server.ModelContextConstants.FUNCTION_TYPE_JAVA) {
                fd.setImplementation(new FunctionImplementation() {
                @Override
                public DataTable execute(Context con, FunctionDefinition def, CallerController caller, RequestController request, DataTable parameters) throws ContextException {
                    if (parameters == null || parameters.getRecordCount() == 0) {
                        TableFormat resultFormat = finalOutputFormat != null ? finalOutputFormat : new TableFormat();
                        return new DataRecord(resultFormat).wrap();
                    }
                    
                    // For calculator function, extract parameters and perform calculation
                    // Check if this is a calculator function by name or by input format
                    boolean isCalculator = "calculate".equals(funcName) || "calculator".equals(funcName) || 
                                         "calculateNew".equals(funcName);
                    
                    // Also check by input format fields
                    if (!isCalculator && parameters != null && parameters.getFormat() != null) {
                        TableFormat inputFormat = parameters.getFormat();
                        if (inputFormat.hasField("action") && inputFormat.hasField("arg1") && inputFormat.hasField("arg2")) {
                            isCalculator = true;
                        }
                    }
                    
                    if (isCalculator) {
                        try {
                            if (parameters == null || parameters.getRecordCount() == 0) {
                                TableFormat resultFormat = finalOutputFormat != null ? finalOutputFormat : new TableFormat();
                                return new DataRecord(resultFormat).wrap();
                            }
                            
                            DataRecord inputRec = parameters.rec();
                            String action = inputRec.getString("action");
                            double arg1 = inputRec.getDouble("arg1");
                            double arg2 = inputRec.getDouble("arg2");
                            
                            double result = 0;
                            switch (action.toLowerCase()) {
                                case "add":
                                case "+":
                                    result = arg1 + arg2;
                                    break;
                                case "subtract":
                                case "-":
                                    result = arg1 - arg2;
                                    break;
                                case "multiply":
                                case "*":
                                    result = arg1 * arg2;
                                    break;
                                case "divide":
                                case "/":
                                    if (arg2 == 0) {
                                        throw new ContextException("Division by zero");
                                    }
                                    result = arg1 / arg2;
                                    break;
                                default:
                                    throw new ContextException("Unknown action: " + action);
                            }
                            
                            // Use the output format from function definition, or create default
                            TableFormat resultFormat = finalOutputFormat != null ? finalOutputFormat.clone() : new TableFormat();
                            if (resultFormat.getFieldCount() == 0) {
                                resultFormat.addField("<result><E><D=Result>");
                            }
                            DataRecord resultRecord = new DataRecord(resultFormat);
                            resultRecord.setValue("result", result);
                            return resultRecord.wrap();
                        } catch (Exception e) {
                            throw new ContextException("Calculation error: " + e.getMessage() + " (action: " + 
                                (parameters != null && parameters.getRecordCount() > 0 ? parameters.rec().getString("action") : "null") + ")");
                        }
                    }
                    
                    // Default: return empty result
                    TableFormat resultFormat = finalOutputFormat != null ? finalOutputFormat : new TableFormat();
                    return new DataRecord(resultFormat).wrap();
                }
            });
            } else if (functionType == com.tibbo.aggregate.common.server.ModelContextConstants.FUNCTION_TYPE_EXPRESSION) {
                // For Expression type (1), generate expression if not provided
                if (expression == null || expression.isEmpty()) {
                    // Auto-generate expression based on inputFormat and outputFormat
                    // Format: table("<<outputFormat>>", {field1} + {field2} + ...)
                    StringBuilder exprBuilder = new StringBuilder();
                    exprBuilder.append("table(\"<<");
                    
                    // Convert outputFormat to string format
                    if (outputFormat != null && outputFormat.getFieldCount() > 0) {
                        // Build format string from outputFormat fields
                        for (int i = 0; i < outputFormat.getFieldCount(); i++) {
                            com.tibbo.aggregate.common.datatable.FieldFormat field = outputFormat.getField(i);
                            exprBuilder.append("<").append(field.getName()).append(">");
                            exprBuilder.append("<").append(String.valueOf(field.getType())).append(">");
                        }
                    } else {
                        // Default: result field
                        exprBuilder.append("result><E>");
                    }
                    exprBuilder.append(">>\"");
                    
                    // Build expression part with input fields
                    if (inputFormat != null && inputFormat.getFieldCount() > 0) {
                        exprBuilder.append(",");
                        for (int i = 0; i < inputFormat.getFieldCount(); i++) {
                            com.tibbo.aggregate.common.datatable.FieldFormat field = inputFormat.getField(i);
                            if (i > 0) {
                                exprBuilder.append("+");
                            }
                            exprBuilder.append("{").append(field.getName()).append("}");
                        }
                    } else {
                        // Default: arg1 + arg2
                        exprBuilder.append(",{arg1}+{arg2}");
                    }
                    exprBuilder.append(")");
                    expression = exprBuilder.toString();
                }
                // Note: Expression is stored in modelFunctions, not in FunctionDefinition
                // FunctionDefinition for Expression type doesn't need implementation
            } else if (functionType == com.tibbo.aggregate.common.server.ModelContextConstants.FUNCTION_TYPE_QUERY) {
                // For Query type (2), set query
                if (query == null || query.isEmpty()) {
                    throw new McpException(
                        com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                        "Query is required for Query type functions"
                    );
                }
                // Note: Query is stored in modelFunctions, not in FunctionDefinition
                // FunctionDefinition for Query type doesn't need implementation
            }
            
            // Check if this is a model context (has modelFunctions variable)
            // For model contexts, we need to use a different approach
            boolean isModelContext = connection.executeWithTimeout(() -> {
                try {
                    context.getVariable(com.tibbo.aggregate.common.server.ModelContextConstants.V_MODEL_FUNCTIONS);
                    return true;
                } catch (ContextException e) {
                    return false; // Not a model context
                }
            }, 60000);
            
            if (isModelContext) {
                // For model context: update V_MODEL_FUNCTIONS variable directly
                connection.executeWithTimeout(() -> {
                    try {
                        com.tibbo.aggregate.common.context.CallerController caller = 
                            context.getContextManager().getCallerController();
                        
                        // Get mutable clone of modelFunctions
                        com.tibbo.aggregate.common.datatable.DataTable modelFunctions = 
                            context.getVariableClone(com.tibbo.aggregate.common.server.ModelContextConstants.V_MODEL_FUNCTIONS, caller);
                        
                        // Check if function already exists
                        boolean exists = false;
                        for (int i = 0; i < modelFunctions.getRecordCount(); i++) {
                            com.tibbo.aggregate.common.datatable.DataRecord rec = modelFunctions.getRecord(i);
                            if (functionName.equals(rec.getString(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_NAME))) {
                                exists = true;
                                break;
                            }
                        }
                        
                        if (exists) {
                            throw new RuntimeException("Function already exists: " + functionName);
                        }
                        
                        // Add new record
                        com.tibbo.aggregate.common.datatable.DataRecord newRec = modelFunctions.addRecord();
                        
                        // Set required fields using correct field names (lowercase)
                        newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_NAME, functionName);
                        
                        if (finalDescription != null) {
                            newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_DESCRIPTION, finalDescription);
                        }
                        
                        if (finalGroup != null) {
                            newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_GROUP, finalGroup);
                        }
                        
                        // Set input and output formats as DataTable (not TableFormat)
                        // Use DataTableBuilding.formatToTable to convert TableFormat to DataTable
                        if (finalInputFormat != null) {
                            com.tibbo.aggregate.common.datatable.DataTable inputFormatTable = 
                                com.tibbo.aggregate.common.datatable.DataTableBuilding.formatToTable(
                                    finalInputFormat, 
                                    new com.tibbo.aggregate.common.datatable.encoding.ClassicEncodingSettings(true));
                            newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_INPUTFORMAT, inputFormatTable);
                        }
                        
                        if (finalOutputFormat != null) {
                            com.tibbo.aggregate.common.datatable.DataTable outputFormatTable = 
                                com.tibbo.aggregate.common.datatable.DataTableBuilding.formatToTable(
                                    finalOutputFormat, 
                                    new com.tibbo.aggregate.common.datatable.encoding.ClassicEncodingSettings(true));
                            newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_OUTPUTFORMAT, outputFormatTable);
                        }
                        
                        // Set function type
                        newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_FD_TYPE, finalFunctionType);
                        
                        // Set implementation/expression/query based on type
                        if (finalFunctionType == com.tibbo.aggregate.common.server.ModelContextConstants.FUNCTION_TYPE_JAVA) {
                            // For Java type, generate implementation code
                            StringBuilder javaCode = new StringBuilder();
                            javaCode.append("import com.tibbo.aggregate.common.context.*;\n");
                            javaCode.append("import com.tibbo.aggregate.common.datatable.*;\n");
                            javaCode.append("import com.tibbo.aggregate.common.server.*;\n");
                            javaCode.append("\n");
                            javaCode.append("public class %ScriptClassNamePattern% implements FunctionImplementation\n");
                            javaCode.append("{\n");
                            javaCode.append("  public DataTable execute(Context con, FunctionDefinition def, CallerController caller, RequestController request, DataTable parameters) throws ContextException\n");
                            javaCode.append("  {\n");
                            javaCode.append("    return null;\n");
                            javaCode.append("  }\n");
                            javaCode.append("}\n");
                            newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_FD_IMPLEMENTATION, javaCode.toString());
                        } else if (finalFunctionType == com.tibbo.aggregate.common.server.ModelContextConstants.FUNCTION_TYPE_EXPRESSION) {
                            String expressionToUse = finalExpression;
                            // If expression is still empty, generate it now with access to formats
                            if (expressionToUse == null || expressionToUse.isEmpty()) {
                                StringBuilder exprBuilder = new StringBuilder();
                                exprBuilder.append("table(\"<<");
                                
                                // Convert outputFormat to string format
                                if (finalOutputFormat != null && finalOutputFormat.getFieldCount() > 0) {
                                    for (int i = 0; i < finalOutputFormat.getFieldCount(); i++) {
                                        com.tibbo.aggregate.common.datatable.FieldFormat field = finalOutputFormat.getField(i);
                                        exprBuilder.append("<").append(field.getName()).append(">");
                                        exprBuilder.append("<").append(String.valueOf(field.getType())).append(">");
                                    }
                                } else {
                                    exprBuilder.append("result><E>");
                                }
                                exprBuilder.append(">>\"");
                                
                                // Build expression part with input fields
                                if (finalInputFormat != null && finalInputFormat.getFieldCount() > 0) {
                                    exprBuilder.append(",");
                                    for (int i = 0; i < finalInputFormat.getFieldCount(); i++) {
                                        com.tibbo.aggregate.common.datatable.FieldFormat field = finalInputFormat.getField(i);
                                        if (i > 0) {
                                            exprBuilder.append("+");
                                        }
                                        exprBuilder.append("{").append(field.getName()).append("}");
                                    }
                                } else {
                                    exprBuilder.append(",{arg1}+{arg2}");
                                }
                                exprBuilder.append(")");
                                expressionToUse = exprBuilder.toString();
                            }
                            newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_FD_EXPRESSION, expressionToUse);
                        } else if (finalFunctionType == com.tibbo.aggregate.common.server.ModelContextConstants.FUNCTION_TYPE_QUERY) {
                            if (finalQuery == null || finalQuery.isEmpty()) {
                                throw new RuntimeException("Query is required for Query type functions");
                            }
                            newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_FD_QUERY, finalQuery);
                        }
                        
                        // Set concurrent flag (default to true)
                        newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_CONCURRENT, true);
                        
                        // Update the variable - this will trigger setVmodelFunctions and update all instances
                        context.setVariable(com.tibbo.aggregate.common.server.ModelContextConstants.V_MODEL_FUNCTIONS, caller, modelFunctions);
                        
                        return null;
                    } catch (ContextException e) {
                        throw new RuntimeException("Failed to add function to model context: " + e.getMessage(), e);
                    }
                }, 60000);
                
                // Verify function was added with retries and delay
                FunctionDefinition verifyFd = null;
                int maxRetries = 5;
                int retryDelay = 200; // milliseconds
                
                for (int retry = 0; retry < maxRetries; retry++) {
                    if (retry > 0) {
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    
                    verifyFd = connection.executeWithTimeout(() -> {
                        return context.getFunctionDefinition(functionName);
                    }, 60000);
                    
                    if (verifyFd != null) {
                        break;
                    }
                }
                
                if (verifyFd == null) {
                    // Function might still be created but not yet available through getFunctionDefinition
                    // Check if it exists in modelFunctions variable as fallback
                    try {
                        com.tibbo.aggregate.common.context.CallerController caller = 
                            context.getContextManager().getCallerController();
                        com.tibbo.aggregate.common.datatable.DataTable modelFunctions = 
                            context.getVariable(com.tibbo.aggregate.common.server.ModelContextConstants.V_MODEL_FUNCTIONS, caller);
                        
                        boolean found = false;
                        for (int i = 0; i < modelFunctions.getRecordCount(); i++) {
                            com.tibbo.aggregate.common.datatable.DataRecord rec = modelFunctions.getRecord(i);
                            if (functionName.equals(rec.getString(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_NAME))) {
                                found = true;
                                break;
                            }
                        }
                        
                        if (found) {
                            // Function exists in modelFunctions, verification passed
                            verifyFd = context.getFunctionDefinition(functionName);
                        }
                    } catch (Exception e) {
                        // Ignore - will throw error below
                    }
                }
                
                if (verifyFd == null) {
                    throw new McpException(
                        com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                        "Function was not created in model context - verification failed"
                    );
                }
            } else {
                // For regular context: use updateFunctionDefinitions
                connection.executeWithTimeout(() -> {
                    java.util.Map<String, com.tibbo.aggregate.common.util.Pair<FunctionDefinition, Boolean>> functionMap = 
                        new java.util.HashMap<>();
                    functionMap.put(
                        functionName, 
                        new com.tibbo.aggregate.common.util.Pair<>(fd, true)
                    );
                    
                    context.updateFunctionDefinitions(functionMap, null, false, null);
                    
                    // Verify function was added
                    FunctionDefinition addedFd = context.getFunctionDefinition(functionName);
                    if (addedFd == null) {
                        throw new RuntimeException("Function was not added - getFunctionDefinition returned null");
                    }
                    
                    // Verify implementation is set only for Java type functions
                    if (functionType == com.tibbo.aggregate.common.server.ModelContextConstants.FUNCTION_TYPE_JAVA) {
                        if (addedFd.getImplementation() == null) {
                            throw new RuntimeException("Function implementation was not set");
                        }
                    }
                    
                    return null;
                }, 60000);
                
                // Verify function exists
                FunctionDefinition verifyFd = connection.executeWithTimeout(() -> {
                    return context.getFunctionDefinition(functionName);
                }, 60000);
                
                if (verifyFd == null) {
                    throw new McpException(
                        com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                        "Function was not created - verification failed"
                    );
                }
            }
            
            // Get final function definition for result
            FunctionDefinition verifyFd = connection.executeWithTimeout(() -> {
                return context.getFunctionDefinition(functionName);
            }, 60000);
            
            if (verifyFd == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Function was not created - verification failed"
                );
            }
            
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Function created successfully");
            result.put("path", path);
            result.put("functionName", functionName);
            result.put("description", verifyFd.getDescription());
            result.put("group", verifyFd.getGroup());
            
            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            com.tibbo.aggregate.mcp.util.ErrorHandler.ErrorDetails errorDetails = 
                com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorDetails(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to create function: " + errorMessage,
                errorDetails
            );
        }
    }
}

