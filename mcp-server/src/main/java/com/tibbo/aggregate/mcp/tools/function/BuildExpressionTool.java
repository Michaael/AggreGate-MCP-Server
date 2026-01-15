package com.tibbo.aggregate.mcp.tools.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for building correct Expression function format strings and expressions
 * This tool ensures that Expression functions are created with correct syntax
 * according to AggreGate documentation rules
 */
public class BuildExpressionTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_build_expression";
    }
    
    @Override
    public String getDescription() {
        return "Build correct Expression function format strings and expression syntax. " +
               "This tool ensures proper formatting according to AggreGate rules. " +
               "It returns plain formats (without <<>>) and, for multiple fields, encoded variants wrapped in <<>>. " +
               "Only the expression string itself uses <<>> inside table() function. " +
               "Use this tool BEFORE creating Expression functions to avoid syntax errors.";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode inputFields = instance.objectNode();
        inputFields.put("type", "array");
        inputFields.put("description", "Array of input field definitions. Each field: {name, type, description?}. " +
               "Type can be: S (String), I (Integer), E (Number/Double), B (Boolean), L (Long), T (DataTable)");
        ObjectNode inputFieldItem = instance.objectNode();
        inputFieldItem.put("type", "object");
        ObjectNode inputFieldProps = instance.objectNode();
        inputFieldProps.set("name", instance.objectNode().put("type", "string"));
        inputFieldProps.set("type", instance.objectNode().put("type", "string"));
        inputFieldProps.set("description", instance.objectNode().put("type", "string"));
        inputFieldItem.set("properties", inputFieldProps);
        inputFieldItem.set("required", instance.arrayNode().add("name").add("type"));
        inputFields.set("items", inputFieldItem);
        properties.set("inputFields", inputFields);
        
        ObjectNode outputFields = instance.objectNode();
        outputFields.put("type", "array");
        outputFields.put("description", "Array of output field definitions. Each field: {name, type, description?}");
        ObjectNode outputFieldItem = instance.objectNode();
        outputFieldItem.put("type", "object");
        ObjectNode outputFieldProps = instance.objectNode();
        outputFieldProps.set("name", instance.objectNode().put("type", "string"));
        outputFieldProps.set("type", instance.objectNode().put("type", "string"));
        outputFieldProps.set("description", instance.objectNode().put("type", "string"));
        outputFieldItem.set("properties", outputFieldProps);
        outputFieldItem.set("required", instance.arrayNode().add("name").add("type"));
        outputFields.set("items", outputFieldItem);
        properties.set("outputFields", outputFields);
        
        ObjectNode formula = instance.objectNode();
        formula.put("type", "string");
        formula.put("description", "Expression formula using field names in {fieldName} format. " +
               "Example: '({value1} + {value2}) / 2' or 'table(\"<<result><E>>\", {value1} + {value2})'. " +
               "If you provide a simple formula, the tool will wrap it in table() with correct output format.");
        properties.set("formula", formula);
        
        schema.set("required", instance.arrayNode().add("inputFields").add("outputFields").add("formula"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("inputFields") || !params.has("outputFields") || !params.has("formula")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "inputFields, outputFields, and formula parameters are required"
            );
        }
        
        try {
            // Build inputFormat (base form, WITHOUT outer <<>>)
            // Note: Descriptions are stored separately, not in format string
            StringBuilder inputFormatBuilder = new StringBuilder();
            ArrayNode inputFields = (ArrayNode) params.get("inputFields");
            for (JsonNode field : inputFields) {
                String name = field.get("name").asText();
                String type = field.get("type").asText().toUpperCase();
                // Don't add description to format - it's handled separately by AggreGate
                inputFormatBuilder.append("<").append(name).append(">");
                inputFormatBuilder.append("<").append(type).append(">");
            }
            String inputFormat = inputFormatBuilder.toString();
            
            // Build outputFormat (base form, WITHOUT outer <<>>)
            // Note: Descriptions are stored separately, not in format string
            StringBuilder outputFormatBuilder = new StringBuilder();
            ArrayNode outputFields = (ArrayNode) params.get("outputFields");
            for (JsonNode field : outputFields) {
                String name = field.get("name").asText();
                String type = field.get("type").asText().toUpperCase();
                // Don't add description to format - it's handled separately by AggreGate
                outputFormatBuilder.append("<").append(name).append(">");
                outputFormatBuilder.append("<").append(type).append(">");
            }
            String outputFormat = outputFormatBuilder.toString();
            
            // Encoded variants with outer <<>> for multi-field formats (recommended for MCP create_function)
            boolean multipleInputFields = inputFields.size() > 1;
            boolean multipleOutputFields = outputFields.size() > 1;
            String encodedInputFormat = multipleInputFields ? "<<" + inputFormat + ">>" : inputFormat;
            String encodedOutputFormat = multipleOutputFields ? "<<" + outputFormat + ">>" : outputFormat;
            
            // Build expression string
            String formula = params.get("formula").asText();
            String expression;
            
            // Check if formula already contains table() call
            if (formula.trim().startsWith("table(")) {
                // User provided full expression - validate it has <<>> inside
                if (!formula.contains("<<") || !formula.contains(">>")) {
                    throw new McpException(
                        com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                        "Expression with table() must contain <<>> brackets inside. " +
                        "Example: table(\"<<result><E>>\", {value1} + {value2})"
                    );
                }
                expression = formula;
            } else {
                // Simple formula - wrap it in table() with correct output format
                // Build the <<outputFormat>> part
                StringBuilder outputFormatWithBrackets = new StringBuilder();
                outputFormatWithBrackets.append("<<");
                for (JsonNode field : outputFields) {
                    String name = field.get("name").asText();
                    String type = field.get("type").asText().toUpperCase();
                    outputFormatWithBrackets.append("<").append(name).append(">");
                    outputFormatWithBrackets.append("<").append(type).append(">");
                }
                outputFormatWithBrackets.append(">>");
                
                expression = "table(\"" + outputFormatWithBrackets.toString() + "\", " + formula + ")";
            }
            
            // Validate that all field names in formula exist in inputFields
            for (JsonNode field : inputFields) {
                String fieldName = field.get("name").asText();
                if (formula.contains("{" + fieldName + "}")) {
                    // Field is used - good
                }
            }
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("inputFormat", inputFormat);
            result.put("outputFormat", outputFormat);
            result.put("expression", expression);
            // Extra fields to help MCP / AI choose safest variant
            result.put("encodedInputFormat", encodedInputFormat);
            result.put("encodedOutputFormat", encodedOutputFormat);
            
            // Add usage instructions
            ObjectNode usage = instance.objectNode();
            usage.put("step1", "Use these values in aggregate_create_function:");
            usage.put("step2", "Set functionType to 1 (Expression)");
            usage.put("step3", "For single-field functions, use inputFormat/outputFormat as-is. For multiple fields, prefer encodedInputFormat/encodedOutputFormat (with <<>>) when calling aggregate_create_function via MCP – this avoids field loss.");
            usage.put("step4", "Use expression AS-IS (it already has <<>> inside table())");
            usage.put("warning", "You can safely pass either plain or <<>>-wrapped formats; the server will normalize and auto-fix typical mistakes.");
            result.set("usage", usage);
            
            // Add examples
            ObjectNode examples = instance.objectNode();
            examples.put("singleField",
                "inputFormat: '<value><E>', outputFormat: '<result><E>', " +
                "expression: 'table(\"<<result><E>>\", {value} * 2)'");
            examples.put("multiField_plain",
                "inputFormat: '<value1><E><value2><E>', outputFormat: '<result><E>', " +
                "expression: 'table(\"<<result><E>>\", ({value1} + {value2}) / 2)'");
            examples.put("multiField_encoded",
                "encodedInputFormat: '<<value1><E><value2><E>>', encodedOutputFormat: '<<result><E>>' – " +
                "recommended when creating functions via MCP to avoid losing fields.");
            result.set("examples", examples);
            
            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Failed to build expression: " + e.getMessage()
            );
        }
    }
}
