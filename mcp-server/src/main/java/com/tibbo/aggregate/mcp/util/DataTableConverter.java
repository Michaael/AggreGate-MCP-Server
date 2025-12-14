package com.tibbo.aggregate.mcp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.FieldFormat;
import com.tibbo.aggregate.common.datatable.SimpleDataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * Utility class for converting between DataTable and JSON format
 */
public class DataTableConverter {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert DataTable to JSON representation
     */
    public static JsonNode toJson(DataTable dataTable) {
        if (dataTable == null) {
            return objectMapper.nullNode();
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("recordCount", dataTable.getRecordCount());
        
        TableFormat format = dataTable.getFormat();
        if (format != null) {
            result.set("format", formatToJson(format));
        }

        ArrayNode records = objectMapper.createArrayNode();
        for (int i = 0; i < dataTable.getRecordCount(); i++) {
            DataRecord record = dataTable.getRecord(i);
            records.add(recordToJson(record, format));
        }
        result.set("records", records);

        return result;
    }

    /**
     * Convert JSON to DataTable
     */
    public static DataTable fromJson(JsonNode json) {
        if (json == null || json.isNull()) {
            return null;
        }

        if (!json.isObject()) {
            throw new IllegalArgumentException("Expected object node for DataTable");
        }

        ObjectNode obj = (ObjectNode) json;
        TableFormat format = null;
        
        if (obj.has("format")) {
            format = formatFromJson(obj.get("format"));
        }

        DataTable dataTable = new SimpleDataTable(format, true);
        
        if (obj.has("records") && obj.get("records").isArray()) {
            ArrayNode records = (ArrayNode) obj.get("records");
            for (JsonNode recordNode : records) {
                DataRecord record = recordFromJson(recordNode, format);
                if (record != null) {
                    dataTable.addRecord(record);
                }
            }
        }

        return dataTable;
    }

    public static ObjectNode formatToJson(TableFormat format) {
        ObjectNode formatJson = objectMapper.createObjectNode();
        formatJson.put("minRecords", format.getMinRecords());
        formatJson.put("maxRecords", format.getMaxRecords());
        
        ArrayNode fields = objectMapper.createArrayNode();
        for (int i = 0; i < format.getFieldCount(); i++) {
            FieldFormat fieldFormat = format.getField(i);
            ObjectNode field = objectMapper.createObjectNode();
            field.put("name", fieldFormat.getName());
            field.put("type", String.valueOf(fieldFormat.getType()));
            field.put("description", fieldFormat.getDescription());
            fields.add(field);
        }
        formatJson.set("fields", fields);
        
        return formatJson;
    }

    private static TableFormat formatFromJson(JsonNode formatJson) {
        if (formatJson == null || !formatJson.isObject()) {
            return null;
        }

        ObjectNode obj = (ObjectNode) formatJson;
        int minRecords = obj.has("minRecords") ? obj.get("minRecords").asInt() : 0;
        int maxRecords = obj.has("maxRecords") ? obj.get("maxRecords").asInt() : Integer.MAX_VALUE;
        
        TableFormat format = new TableFormat(minRecords, maxRecords);
        
        if (obj.has("fields") && obj.get("fields").isArray()) {
            ArrayNode fields = (ArrayNode) obj.get("fields");
            for (JsonNode fieldNode : fields) {
                String fieldDef = buildFieldDefinition(fieldNode);
                format.addField(fieldDef);
            }
        }
        
        return format;
    }

    private static String buildFieldDefinition(JsonNode fieldNode) {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(fieldNode.get("name").asText()).append(">");
        
        String type = fieldNode.get("type").asText();
        String typeChar = String.valueOf(type.charAt(0));
        sb.append("<").append(typeChar).append(">");
        
        if (fieldNode.has("description")) {
            sb.append("<D=").append(fieldNode.get("description").asText()).append(">");
        }
        
        return sb.toString();
    }

    private static JsonNode recordToJson(DataRecord record, TableFormat format) {
        if (record == null) {
            return objectMapper.nullNode();
        }

        ObjectNode recordJson = objectMapper.createObjectNode();
        
        if (format != null) {
            for (int i = 0; i < format.getFieldCount(); i++) {
                FieldFormat fieldFormat = format.getField(i);
                String fieldName = fieldFormat.getName();
                Object value = getFieldValue(record, fieldFormat);
                recordJson.set(fieldName, valueToJson(value));
            }
        } else {
            // If no format, try to iterate over available fields
            // This is a fallback approach
            for (int i = 0; i < record.getFieldCount(); i++) {
                try {
                    FieldFormat fieldFormat = record.getFormat().getField(i);
                    String fieldName = fieldFormat.getName();
                    Object value = getFieldValue(record, fieldFormat);
                    recordJson.set(fieldName, valueToJson(value));
                } catch (Exception e) {
                    // Skip fields that can't be accessed
                }
            }
        }

        return recordJson;
    }

    private static DataRecord recordFromJson(JsonNode recordNode, TableFormat format) {
        if (recordNode == null || !recordNode.isObject()) {
            return null;
        }

        if (format == null) {
            throw new IllegalArgumentException("TableFormat is required to create DataRecord from JSON");
        }

        DataRecord record = new DataRecord(format);
        if (!recordNode.isObject()) {
            return null;
        }
        ObjectNode obj = (ObjectNode) recordNode;

        Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String fieldName = entry.getKey();
            JsonNode valueNode = entry.getValue();
            
            try {
                FieldFormat fieldFormat = format.getField(fieldName);
                Object value = valueFromJson(valueNode, fieldFormat);
                record.setValue(fieldName, value);
            } catch (Exception e) {
                // Skip fields that don't exist in format or can't be set
            }
        }

        return record;
    }

    private static Object getFieldValue(DataRecord record, FieldFormat fieldFormat) {
        try {
            String fieldName = fieldFormat.getName();
            char fieldType = fieldFormat.getType();
            switch (fieldType) {
                case FieldFormat.STRING_FIELD:
                    return record.getString(fieldName);
                case FieldFormat.INTEGER_FIELD:
                    return record.getInt(fieldName);
                case FieldFormat.LONG_FIELD:
                    return record.getLong(fieldName);
                case FieldFormat.FLOAT_FIELD:
                    return record.getFloat(fieldName);
                case FieldFormat.DOUBLE_FIELD:
                    return record.getDouble(fieldName);
                case FieldFormat.BOOLEAN_FIELD:
                    return record.getBoolean(fieldName);
                case FieldFormat.DATE_FIELD:
                    return record.getDate(fieldName);
                case FieldFormat.DATATABLE_FIELD:
                    return toJson(record.getDataTable(fieldName));
                default:
                    return record.getString(fieldName);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonNode valueToJson(Object value) {
        if (value == null) {
            return objectMapper.nullNode();
        }
        
        if (value instanceof String) {
            return objectMapper.valueToTree(value);
        } else if (value instanceof Number) {
            return objectMapper.valueToTree(value);
        } else if (value instanceof Boolean) {
            return objectMapper.valueToTree(value);
        } else if (value instanceof Date) {
            return objectMapper.valueToTree(((Date) value).getTime());
        } else if (value instanceof DataTable) {
            return toJson((DataTable) value);
        } else {
            return objectMapper.valueToTree(value.toString());
        }
    }

    private static Object valueFromJson(JsonNode valueNode, FieldFormat fieldFormat) {
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }

        char fieldType = fieldFormat.getType();
        switch (fieldType) {
            case FieldFormat.STRING_FIELD:
                return valueNode.asText();
            case FieldFormat.INTEGER_FIELD:
                return valueNode.asInt();
            case FieldFormat.LONG_FIELD:
                return valueNode.asLong();
            case FieldFormat.FLOAT_FIELD:
                return (float) valueNode.asDouble();
            case FieldFormat.DOUBLE_FIELD:
                return valueNode.asDouble();
            case FieldFormat.BOOLEAN_FIELD:
                return valueNode.asBoolean();
            case FieldFormat.DATE_FIELD:
                return new Date(valueNode.asLong());
            case FieldFormat.DATATABLE_FIELD:
                return fromJson(valueNode);
            default:
                return valueNode.asText();
        }
    }
}

