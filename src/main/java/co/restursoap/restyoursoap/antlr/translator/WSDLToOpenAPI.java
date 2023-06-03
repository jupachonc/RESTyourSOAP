package co.restursoap.restyoursoap.antlr.translator;

import co.restursoap.restyoursoap.antlr.gen.XMLParser;
import co.restursoap.restyoursoap.antlr.gen.XMLParserBaseListener;

import java.util.*;

public class WSDLToOpenAPI extends XMLParserBaseListener {
     public String getOutput(){

         return "openapi: \"3.0.0\"\n"
                 + "info:\n  title: " + apiDefinition.get("title") + "\n  version: 0.0.1" +
                 "\nservers:\n" + listServers((HashSet<String>) apiDefinition.get("servers")) +
                 "\n components:\n  schemas:\n" + listElements((HashMap<String,Object>) apiDefinition.get("elements"));
     }

     public String getServiceName(){
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
    private String mainElement = "";

    private String listServers(HashSet<String> list){
        StringBuilder out = new StringBuilder();
        for(String url:list){
            out.append("  - url: ").append(url).append("\n");
        }
        return out.toString();
    }
    private String listElements(HashMap<String,Object> list){
        StringBuilder out = new StringBuilder();
        for(String name:list.keySet()){
            out.append("    ").append(name).append(":\n      type: object\n      properties:\n");
            for(String element:((HashMap<String,Object>) list.get(name)).keySet()){
                out.append("        ").append(element).append(": \n        ").append(((HashMap<String,Object>) list.get(name)).get(element)).append("\n");
            }
        }
        return out.toString();
    }
     private String getTagType(String tag){
         List<String> tagTypes = Arrays.asList("types", "message", "portType", "binding", "definitions",
                                                "address", "service", "element", "schema", "complexType");

         for(String tagType:tagTypes){
             if(tag.contains(tagType)) return tagType;
         }

         return "other";
     }

     private HashMap<String, Object> mapAttributes(List<XMLParser.AttributeContext> attributes){
         HashMap<String, Object> attributesMap = new HashMap<>();
         for(XMLParser.AttributeContext attribute:attributes){
             String[] spAtt = attribute.getText().split("=");
             attributesMap.put(spAtt[0], spAtt[1].replace("\"", ""));
         }
         return attributesMap;
     }

    private void SaveMainElement(XMLParser.ElementContext ctx) {
        insideElement = true;
        String name = (String) mapAttributes(ctx.attribute()).get("name");
        mainElement = name;
        if(apiDefinition.containsKey("elements")){
            ((HashMap<String, Object>) apiDefinition.get("elements")).put(name, new HashMap<>());
        } else {
            HashMap<String, Object> elements = new HashMap<>();
            elements.put(name, new HashMap<>());
            apiDefinition.put("elements", elements);
        }
    }

    private String reformatAttributes(String attributeName, String attributeContent){
        String returnValue = "";
        switch (attributeName){
            case "minOccurs":
                if (!Objects.equals(attributeContent, "unbounded")){
                    returnValue =  "minimum: " + attributeContent;
                }
                break;
            case "maxOccurs":
                if (!Objects.equals(attributeContent, "unbounded")){
                    returnValue =  "maximum: " + attributeContent;
                }
                break;
            case "type":
                String[] spType = attributeContent.split(":");
                if (Objects.equals(spType[0], "s")){
                    String newContent = "";
                    String format = "";
                    switch (spType[1]){
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
         switch (getTagType(elementTag)){
             case "definitions":
                 insideDefinitions = true;
                 break;
             case "types":
                 System.out.println("Has a type declaration");
                 insideType = true;
                 break;
             case "message":
                 break;
             case "portType":
                 break;
             case "service":
                 insideService = true;
                 apiDefinition.put("title", mapAttributes(ctx.attribute()).get("name"));
                 break;
             case "address":
                 if(insideService){
                     String location = (String) mapAttributes(ctx.attribute()).get("location");
                     if(apiDefinition.containsKey("servers")){
                         ((HashSet<String>) apiDefinition.get("servers")).add(location);
                     } else {
                         HashSet<String> servers = new HashSet<>();
                         servers.add(location);
                         apiDefinition.put("servers", servers);
                     }
                 }
                 break;
             case "schema":
                 insideSchema = true;
                 break;
             case "complexType":
                 if (insideType && insideSchema){
                     if (mapAttributes(ctx.attribute()).containsKey("name") && !insideElement){
                         SaveMainElement(ctx);
                     }
                     else if(mapAttributes(ctx.attribute()).containsKey("name") && insideElement){
                         //TODO: Ver si esto se debe poner o no
                     }
                 }
                 break;
             case "element":
                 if (insideType && insideSchema){
                     if (!insideElement){
                         SaveMainElement(ctx);
                     }
                     else {
                         HashMap<String,Object> elementAttributes = mapAttributes(ctx.attribute());
                         StringBuilder inElement = new StringBuilder();
                         for (String attribute:elementAttributes.keySet()){
                             if (!Objects.equals(attribute, "name")){
                                 inElement.append(reformatAttributes(attribute, (String) elementAttributes.get(attribute))).append("\n");
                             }
                         }
                         ((HashMap<String, Object>) ((HashMap<String, Object>) apiDefinition.get("elements")).get(mainElement)).put((String)elementAttributes.get("name"), inElement);
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
            case "element", "complexType" -> {
                String name = (String) mapAttributes(ctx.attribute()).get("name");
                if (((HashMap<String, Object>) apiDefinition.get("elements")).containsKey(name)) {
                    insideElement = false;
                }
            }
        }
    }
}