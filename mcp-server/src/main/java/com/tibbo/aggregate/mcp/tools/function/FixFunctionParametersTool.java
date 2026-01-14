package com.tibbo.aggregate.mcp.tools.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for fixing function parameter issues based on error messages.
 * Analyzes errors and provides corrected parameters.
 */
public class FixFunctionParametersTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_fix_function_parameters";
    }
    
    @Override
    public String getDescription() {
        return "Analyze function parameter errors and provide corrected parameters. " +
               "Use this when aggregate_test_function fails with 'Field not found' errors. " +
               "This tool automatically fixes parameter mismatches based on function's inputFormat.";
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
        
        ObjectNode errorMessage = instance.objectNode();
        errorMessage.put("type", "string");
        errorMessage.put("description", "Error message from aggregate_test_function");
        properties.set("errorMessage", errorMessage);
        
        ObjectNode providedParameters = instance.objectNode();
        providedParameters.put("type", "object");
        providedParameters.put("description", "Parameters that were provided (optional)");
        properties.set("providedParameters", providedParameters);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("functionName").add("errorMessage"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("functionName") || !params.has("errorMessage")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path, functionName, and errorMessage are required"
            );
        }
        
        String path = params.get("path").asText();
        String functionName = params.get("functionName").asText();
        String errorMessage = params.get("errorMessage").asText();
        String connectionKey = params.has("connectionKey") ? params.get("connectionKey").asText() : null;
        
        // Get function definition to understand inputFormat
        com.tibbo.aggregate.mcp.tools.function.GetFunctionTool getFunctionTool = 
            new com.tibbo.aggregate.mcp.tools.function.GetFunctionTool();
        ObjectNode getParams = instance.objectNode();
        getParams.put("path", path);
        getParams.put("functionName", functionName);
        if (connectionKey != null) {
            getParams.put("connectionKey", connectionKey);
        }
        
        JsonNode functionInfo;
        try {
            functionInfo = getFunctionTool.execute(getParams, connectionManager);
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to get function definition: " + e.getMessage()
            );
        }
        
        ObjectNode result = instance.objectNode();
        result.put("path", path);
        result.put("functionName", functionName);
        
        // Extract missing field from error
        String missingField = extractMissingField(errorMessage);
        String foundField = extractFoundField(errorMessage);
        
        if (missingField != null) {
            result.put("missingField", missingField);
        }
        if (foundField != null) {
            result.put("foundField", foundField);
        }
        
            // Get inputFormat from function info
        // Try inputFields first (more reliable), then inputFormat.fields
        ArrayNode requiredFields = instance.arrayNode();
        ArrayNode fieldInfo = instance.arrayNode();
        
        if (functionInfo != null && functionInfo.has("inputFields") && functionInfo.get("inputFields").isArray()) {
            // Use inputFields if available (more reliable)
            ArrayNode inputFields = (ArrayNode) functionInfo.get("inputFields");
            for (JsonNode field : inputFields) {
                if (field.has("name")) {
                    String fieldName = field.get("name").asText();
                    requiredFields.add(fieldName);
                    
                    ObjectNode fieldInfoObj = instance.objectNode();
                    fieldInfoObj.put("name", fieldName);
                    if (field.has("type")) {
                        fieldInfoObj.put("type", field.get("type").asText());
                    }
                    fieldInfo.add(fieldInfoObj);
                }
            }
        } else if (functionInfo != null && functionInfo.has("inputFormat")) {
            JsonNode inputFormatJson = functionInfo.get("inputFormat");
            
            if (inputFormatJson.has("fields") && inputFormatJson.get("fields").isArray()) {
                ArrayNode fields = (ArrayNode) inputFormatJson.get("fields");
                for (JsonNode field : fields) {
                    if (field.has("name")) {
                        String fieldName = field.get("name").asText();
                        requiredFields.add(fieldName);
                        
                        ObjectNode fieldInfoObj = instance.objectNode();
                        fieldInfoObj.put("name", fieldName);
                        if (field.has("type")) {
                            fieldInfoObj.put("type", field.get("type").asText());
                        }
                        fieldInfo.add(fieldInfoObj);
                    }
                }
            }
        }
        
        if (functionInfo != null) {
            
            result.set("requiredFields", requiredFields);
            result.set("fieldInfo", fieldInfo);
            
            // Build corrected parameters
            ObjectNode correctedParameters = instance.objectNode();
            JsonNode providedParams = params.has("providedParameters") ? params.get("providedParameters") : null;
            
            // Add all required fields
            for (JsonNode field : fieldInfo) {
                String fieldName = field.get("name").asText();
                String fieldType = field.has("type") ? field.get("type").asText() : "E";
                
                if (providedParams != null && providedParams.has(fieldName)) {
                    // Use provided value
                    correctedParameters.set(fieldName, providedParams.get(fieldName));
                } else {
                    // Use default value based on type
                    Object defaultValue = getDefaultValueForType(fieldType);
                    if (defaultValue instanceof String) {
                        correctedParameters.put(fieldName, (String) defaultValue);
                    } else if (defaultValue instanceof Number) {
                        if (defaultValue instanceof Integer) {
                            correctedParameters.put(fieldName, (Integer) defaultValue);
                        } else {
                            correctedParameters.put(fieldName, ((Number) defaultValue).doubleValue());
                        }
                    } else if (defaultValue instanceof Boolean) {
                        correctedParameters.put(fieldName, (Boolean) defaultValue);
                    }
                }
            }
            
            result.set("correctedParameters", correctedParameters);
            
            // Add usage instructions
            ObjectNode usage = instance.objectNode();
            usage.put("step1", "Use correctedParameters in aggregate_test_function:");
            usage.put("step2", "aggregate_test_function with parameters = correctedParameters");
            usage.put("step3", "All required fields are now included");
            result.set("usage", usage);
            
            // Add explanation
            if (missingField != null) {
                result.put("explanation",
                    "Функция требует поле '" + missingField + "', но оно отсутствовало в параметрах. " +
                    "Исправленные параметры включают все обязательные поля из inputFormat.");
            } else {
                result.put("explanation",
                    "Параметры исправлены для соответствия inputFormat функции. " +
                    "Все обязательные поля включены.");
            }
        } else {
            result.put("error", "Could not determine inputFormat from function definition");
        }
        
        return result;
    }
    
    private String extractMissingField(String errorMessage) {
        // Pattern: Field 'fieldName' not found
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Field\\s+['\"]([^'\"]+)['\"]\\s+not\\s+found");
        java.util.regex.Matcher matcher = pattern.matcher(errorMessage);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private String extractFoundField(String errorMessage) {
        // Pattern: data record: fieldName
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("data record:\\s*([^\\s:]+)");
        java.util.regex.Matcher matcher = pattern.matcher(errorMessage);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private Object getDefaultValueForType(String type) {
        if (type == null) {
            return 0.0;
        }
        type = type.toUpperCase();
        if (type.equals("S") || type.equals("STRING")) {
            return "";
        } else if (type.equals("I") || type.equals("INTEGER")) {
            return 0;
        } else if (type.equals("L") || type.equals("LONG")) {
            return 0L;
        } else if (type.equals("E") || type.equals("DOUBLE") || type.equals("FLOAT") || type.equals("NUMBER")) {
            return 0.0;
        } else if (type.equals("B") || type.equals("BOOLEAN")) {
            return false;
        } else {
            return 0.0; // Default to number
        }
    }
}
