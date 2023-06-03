package co.restursoap.restyoursoap.antlr.translator;

import co.restursoap.restyoursoap.antlr.gen.XMLParser;
import co.restursoap.restyoursoap.antlr.gen.XMLParserBaseListener;
import org.json.JSONObject;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

public class WSDLToOpenAPI extends XMLParserBaseListener {
    public String getOutput(boolean inYAML) {

        LinkedHashMap<String, Object> openApiDefinition = new LinkedHashMap<>();
        openApiDefinition.put("openapi", "3.0.0");
         /*
            Information
         */

        LinkedHashMap<String, Object> info = new LinkedHashMap<>();
        info.put("title", apiDefinition.get("title"));
        info.put("version", "0.0.1");

        openApiDefinition.put("info", info);

         /*
            Servers
         */
        openApiDefinition.put("servers", new ArrayList<>((HashSet<?>) apiDefinition.get("servers")));

         /*
            Components
         */
        HashMap<String, Object> components = new HashMap<>();
        components.put("schemas", apiDefinition.get("elements"));
        openApiDefinition.put("components", components);

         /*
            Paths
         */
        openApiDefinition.put("paths", apiDefinition.get("paths"));
        if (inYAML) {
            DumperOptions options = new DumperOptions();
            options.setIndent(2);
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);

            return yaml.dump(openApiDefinition);
        } else {
            JSONObject jsonDef = new JSONObject(openApiDefinition);
            return jsonDef.toString();

        }
    }

    public String getPackageJSON() {
        LinkedHashMap<String, Object> pMap = new LinkedHashMap<>();
        pMap.put("name", apiDefinition.get("title"));
        pMap.put("version", "0.0.1");
        pMap.put("description", "");
        pMap.put("main", "index.js");
         /*
            Scripts
         */
        LinkedHashMap<String, String> scripts = new LinkedHashMap<>();
        scripts.put("test", "echo \"Error: no test specified\" && exit 1");
        scripts.put("start", "node index.js");
        pMap.put("scripts", scripts);

        pMap.put("author", "");
        pMap.put("license", "ISC");

         /*
            Depedencies
         */
        LinkedHashMap<String, String> dependencies = new LinkedHashMap<>();
        dependencies.put("express", "^4.18.2");
        pMap.put("dependencies", dependencies);

        // Generate Output JSOn
        JSONObject pjson = new JSONObject(pMap);
        return pjson.toString(2);
    }

    public String getServiceName() {
        return (String) apiDefinition.get("title");
    }

    /* Data structures for store translation */
    HashMap<String, Object> apiDefinition = new HashMap<>();
    // Flags
    private Boolean insideDefinitions = false;
    private Boolean insideService = false;
    private Boolean insideType = false;
    private Boolean insideSchema = false;
    private Boolean insideElement = false;
    private Boolean insidePortType = false;
    private Boolean insideOperation = false;
    private String mainElement = "";
    private String operationName = "";
    private LinkedHashMap<String, Object> messagesMap = new LinkedHashMap<>();
    private String message = "";

    private String getTagType(String tag) {
        List<String> tagTypes = Arrays.asList("types", "message", "portType", "binding", "definitions",
                "address", "service", "element", "schema", "complexType", "part", "operation",
                "input", "output");

        for (String tagType : tagTypes) {
            if (tag.contains(tagType)) return tagType;
        }

        return "other";
    }

    private HashMap<String, Object> mapAttributes(List<XMLParser.AttributeContext> attributes) {
        HashMap<String, Object> attributesMap = new HashMap<>();
        for (XMLParser.AttributeContext attribute : attributes) {
            String[] spAtt = attribute.getText().split("=");
            attributesMap.put(spAtt[0], spAtt[1].replace("\"", ""));
        }
        return attributesMap;
    }

    private void SaveMainElement(XMLParser.ElementContext ctx) {
        insideElement = true;
        String name = (String) mapAttributes(ctx.attribute()).get("name");
        mainElement = name;
        if (apiDefinition.containsKey("elements")) {
            ((HashMap<String, Object>) apiDefinition.get("elements")).put(name, new HashMap<>());
        } else {
            HashMap<String, Object> elements = new HashMap<>();
            elements.put(name, new HashMap<>());
            apiDefinition.put("elements", elements);
        }
    }

    private String reformatAttributes(String attributeName, String attributeContent) {
        String returnValue = "";
        switch (attributeName) {
            case "minOccurs":
                if (!Objects.equals(attributeContent, "unbounded")) {
                    returnValue = "minimum: " + attributeContent;
                }
                break;
            case "maxOccurs":
                if (!Objects.equals(attributeContent, "unbounded")) {
                    returnValue = "maximum: " + attributeContent;
                }
                break;
            case "type":
                String[] spType = attributeContent.split(":");
                if (Objects.equals(spType[0], "s")) {
                    String newContent = "";
                    String format = "";
                    switch (spType[1]) {
                        case "int":
                            newContent = "integer";
                            break;
                        case "string":
                            newContent = "string";
                            break;
                        case "float":
                            newContent = "number";
                            format = "\nformat: float";
                            break;
                        case "boolean":
                            newContent = "integer";
                            break;
                        case "date":
                            newContent = "string";
                            break;
                    }
                    returnValue = "type: " + newContent + format;
                }
                break;
        }
        return returnValue;
    }

    @Override
    public void enterElement(XMLParser.ElementContext ctx) {
        String elementTag = ctx.Name(0).toString();
        switch (getTagType(elementTag)) {
            case "definitions":
                insideDefinitions = true;
                break;
            case "types":
                //System.out.println("Has a type declaration");
                insideType = true;
                break;
            case "message":
                message = (String) mapAttributes(ctx.attribute()).get("name");
                HashMap<String, Object> stringContent = new HashMap<>();
                stringContent.put("application/json", new HashMap<>());
                HashMap<String, Object> content = new HashMap<>();
                content.put("content", stringContent);
                messagesMap.put(message, content);
                break;
            case "part":
                String elementsName = (String) mapAttributes(ctx.attribute()).get("element");
                String[] spPart = elementsName.split(":");
                HashMap<String, Object> schemaRoute = new HashMap<>();
                schemaRoute.put("$ref", "#/components/schemas/" + spPart[1]);
                ((HashMap<String, Object>) ((HashMap<String, Object>) ((HashMap<String, Object>) messagesMap.get(message)).get("content")).get("application/json")).put("schema", schemaRoute);
                break;
            case "portType":
                insidePortType = true;
                break;
            case "operation":
                if (insidePortType) {
                    insideOperation = true;
                    operationName = (String) mapAttributes(ctx.attribute()).get("name");
                    if (apiDefinition.containsKey("paths")) {
                        ((HashMap<String, Object>) apiDefinition.get("paths")).put(operationName, new HashMap<>());
                    } else {
                        HashMap<String, Object> paths = new HashMap<>();
                        paths.put(operationName, new HashMap<>());
                        apiDefinition.put("paths", paths);
                    }
                }
                break;
            case "input":
                if (insideOperation) {
                    String inputMessage = (String) mapAttributes(ctx.attribute()).get("message");
                    String[] spMessage = inputMessage.split(":");
                    HashMap<String, Object> post = new HashMap<>();
                    post.put("requestBody", messagesMap.get(spMessage[1]));
                    ((HashMap<String, Object>) ((HashMap<String, Object>) apiDefinition.get("paths")).get(operationName)).put("post", post);
                }
                break;
            case "output":
                if (insideOperation) {
                    String outputMessage = (String) mapAttributes(ctx.attribute()).get("message");
                    String[] spOutputMessage = outputMessage.split(":");
                    HashMap<String, Object> response = new HashMap<>();
                    response.put("200", messagesMap.get(spOutputMessage[1]));
                    HashMap<String, Object> get = new HashMap<>();
                    get.put("responses", response);
                    if (!((HashMap<String, Object>) ((HashMap<String, Object>) apiDefinition.get("paths")).get(operationName)).containsKey("post")) {
                        ((HashMap<String, Object>) ((HashMap<String, Object>) apiDefinition.get("paths")).get(operationName)).put("get", get);
                    } else {
                        ((HashMap<String, Object>) ((HashMap<String, Object>) ((HashMap<String, Object>) apiDefinition.get("paths")).get(operationName)).get("post")).put("responses", response);
                    }
                }
                break;
            case "service":
                insideService = true;
                apiDefinition.put("title", mapAttributes(ctx.attribute()).get("name"));
                break;
            case "address":
                if (insideService) {
                    String location = (String) mapAttributes(ctx.attribute()).get("location");
                    LinkedHashMap<String, Object> server = new LinkedHashMap<>();
                    server.put("url", location);
                    if (apiDefinition.containsKey("servers")) {
                        ((HashSet<Object>) apiDefinition.get("servers")).add(server);
                    } else {
                        HashSet<Object> servers = new HashSet<>();
                        servers.add(server);
                        apiDefinition.put("servers", servers);
                    }
                }
                break;
            case "schema":
                insideSchema = true;
                break;
            case "complexType":
                if (insideType && insideSchema) {
                    if (mapAttributes(ctx.attribute()).containsKey("name") && !insideElement) {
                        SaveMainElement(ctx);
                    } else if (mapAttributes(ctx.attribute()).containsKey("name") && insideElement) {
                        //TODO: Ver si esto se debe poner o no
                    }
                }
                break;
            case "element":
                if (insideType && insideSchema) {
                    if (!insideElement) {
                        SaveMainElement(ctx);
                    } else {
                        HashMap<String, Object> elementAttributes = mapAttributes(ctx.attribute());
                        StringBuilder inElement = new StringBuilder();
                        for (String attribute : elementAttributes.keySet()) {
                            if (!Objects.equals(attribute, "name")) {
                                inElement.append(reformatAttributes(attribute, (String) elementAttributes.get(attribute))).append("\n");
                            }
                        }
                        ((HashMap<String, Object>) ((HashMap<String, Object>) apiDefinition.get("elements")).get(mainElement)).put((String) elementAttributes.get("name"), inElement);
                    }
                }
                break;
            default:
                //System.out.println(elementTag);
                break;
        }
        //super.enterElement(ctx);
    }

    @Override
    public void exitElement(XMLParser.ElementContext ctx) {
        super.exitElement(ctx);
        String elementTag = ctx.Name(0).toString();
        switch (getTagType(elementTag)) {
            case "element", "complexType":
                String name = (String) mapAttributes(ctx.attribute()).get("name");
                if (((HashMap<String, Object>) apiDefinition.get("elements")).containsKey(name)) {
                    insideElement = false;
                }
                break;
            case "operation":
                insideOperation = false;
                break;
            case "portType":
                insidePortType = false;
                break;
        }
    }
}