package com.tibbo.aggregate.mcp.tools.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for validating Expression function syntax before creation
 * Checks all AggreGate Expression rules and provides detailed feedback
 */
public class ValidateExpressionTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_validate_expression";
    }
    
    @Override
    public String getDescription() {
        return "Validate Expression function syntax according to AggreGate rules. " +
               "Checks: inputFormat/outputFormat should NOT have <<>>, " +
               "expression must have <<>> inside table(), field names match, etc. " +
               "Use this BEFORE creating functions to catch errors early.";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode inputFormat = instance.objectNode();
        inputFormat.put("type", "string");
        inputFormat.put("description", "Input format string (should NOT have <<>> brackets)");
        properties.set("inputFormat", inputFormat);
        
        ObjectNode outputFormat = instance.objectNode();
        outputFormat.put("type", "string");
        outputFormat.put("description", "Output format string (should NOT have <<>> brackets)");
        properties.set("outputFormat", outputFormat);
        
        ObjectNode expression = instance.objectNode();
        expression.put("type", "string");
        expression.put("description", "Expression string (should have <<>> inside table() function)");
        properties.set("expression", expression);
        
        schema.set("required", instance.arrayNode().add("inputFormat").add("outputFormat").add("expression"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("inputFormat") || !params.has("outputFormat") || !params.has("expression")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "inputFormat, outputFormat, and expression parameters are required"
            );
        }
        
        String inputFormat = params.get("inputFormat").asText();
        String outputFormat = params.get("outputFormat").asText();
        String expression = params.get("expression").asText();
        
        ObjectNode result = instance.objectNode();
        result.put("valid", true);
        ArrayNode errors = instance.arrayNode();
        ArrayNode warnings = instance.arrayNode();
        ArrayNode suggestions = instance.arrayNode();
        
        // Rule 1: inputFormat should NOT have <<>>
        if (inputFormat.contains("<<") || inputFormat.contains(">>")) {
            result.put("valid", false);
            errors.add("inputFormat contains <<>> brackets. " +
                      "CORRECT: '<value1><E><value2><E>'. " +
                      "INCORRECT: '<<value1><E><value2><E>>'. " +
                      "Remove <<>> from inputFormat!");
        }
        
        // Rule 2: outputFormat should NOT have <<>>
        if (outputFormat.contains("<<") || outputFormat.contains(">>")) {
            result.put("valid", false);
            errors.add("outputFormat contains <<>> brackets. " +
                      "CORRECT: '<result><E>'. " +
                      "INCORRECT: '<<result><E>>'. " +
                      "Remove <<>> from outputFormat!");
        }
        
        // Rule 3: expression MUST have <<>> inside table()
        if (!expression.contains("table(")) {
            result.put("valid", false);
            errors.add("expression does not contain table() function. " +
                      "Expression functions must use table() format. " +
                      "Example: table(\"<<result><E>>\", {value1} + {value2})");
        } else {
            // Check that <<>> is inside table()
            if (!expression.contains("<<") || !expression.contains(">>")) {
                result.put("valid", false);
                errors.add("expression contains table() but missing <<>> brackets inside. " +
                          "CORRECT: table(\"<<result><E>>\", {value1} + {value2}). " +
                          "The <<>> must be INSIDE the table() function!");
            } else {
                // Extract the format inside <<>> from expression
                Pattern pattern = Pattern.compile("table\\(\"(&lt;&lt;.*?&gt;&gt;)\"|table\\(\"(<<.*?>>)\"");
                Matcher matcher = pattern.matcher(expression);
                if (matcher.find()) {
                    String formatInExpression = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                    // Remove << and >>
                    String formatInExpressionClean = formatInExpression.replace("<<", "").replace(">>", "");
                    
                    // Compare with outputFormat
                    if (!formatInExpressionClean.equals(outputFormat)) {
                        warnings.add("Format inside expression <<>> does not match outputFormat. " +
                                   "Expression has: " + formatInExpressionClean + ", " +
                                   "outputFormat has: " + outputFormat + ". " +
                                   "They should match!");
                    }
                }
            }
        }
        
        // Rule 4: Extract field names from inputFormat and check they're used in expression
        Set<String> inputFieldNames = extractFieldNames(inputFormat);
        Set<String> expressionFieldNames = extractFieldNamesFromExpression(expression);
        
        for (String fieldName : inputFieldNames) {
            if (!expressionFieldNames.contains(fieldName)) {
                warnings.add("Input field '" + fieldName + "' is defined but not used in expression. " +
                           "Use {fieldName} in expression to reference it.");
            }
        }
        
        for (String fieldName : expressionFieldNames) {
            if (!inputFieldNames.contains(fieldName)) {
                errors.add("Expression references field '" + fieldName + "' but it's not in inputFormat. " +
                          "Add it to inputFormat or remove from expression.");
                result.put("valid", false);
            }
        }
        
        // Rule 5: Check outputFormat field names match expression output
        // This is harder to validate without executing, but we can check basic structure
        // (outputFieldNames extraction is done implicitly in the expression format check above)
        
        // Add suggestions based on common mistakes
        if (inputFormat.trim().isEmpty()) {
            suggestions.add("inputFormat is empty. Even if function has no inputs, consider using empty format: ''");
        }
        
        if (outputFormat.trim().isEmpty()) {
            suggestions.add("outputFormat is empty. Expression functions should have output. " +
                          "Example: '<result><E>'");
        }
        
        // Check for common type mistakes
        if (inputFormat.contains("<S>") && expression.contains("+") && !expression.contains("concat")) {
            suggestions.add("You're using + operator with String fields. " +
                          "For strings, use concat() function: concat({field1}, {field2})");
        }
        
        result.set("errors", errors);
        result.set("warnings", warnings);
        result.set("suggestions", suggestions);
        
        // Add correct example
        if (!result.get("valid").asBoolean()) {
            ObjectNode correctExample = instance.objectNode();
            correctExample.put("inputFormat", "<value1><E><value2><E>");
            correctExample.put("outputFormat", "<result><E>");
            correctExample.put("expression", "table(\"<<result><E>>\", ({value1} + {value2}) / 2)");
            result.set("correctExample", correctExample);
        }
        
        return result;
    }
    
    private Set<String> extractFieldNames(String format) {
        Set<String> fieldNames = new HashSet<>();
        // Pattern: <fieldName><Type> or <fieldName><Type><D=description>
        Pattern pattern = Pattern.compile("<([^<>]+)><[^<>]+>");
        Matcher matcher = pattern.matcher(format);
        while (matcher.find()) {
            fieldNames.add(matcher.group(1));
        }
        return fieldNames;
    }
    
    private Set<String> extractFieldNamesFromExpression(String expression) {
        Set<String> fieldNames = new HashSet<>();
        // Pattern: {fieldName}
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(expression);
        while (matcher.find()) {
            fieldNames.add(matcher.group(1));
        }
        return fieldNames;
    }
}
