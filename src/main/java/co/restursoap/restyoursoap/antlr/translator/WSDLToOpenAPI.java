package co.restursoap.restyoursoap.antlr.translator;

import co.restursoap.restyoursoap.antlr.gen.XMLParser;
import co.restursoap.restyoursoap.antlr.gen.XMLParserBaseListener;

import java.util.*;

public class WSDLToOpenAPI extends XMLParserBaseListener {
     public String getOutput(){

         return "openapi: \"3.0.0\"\n"
                 + "info:\n\tname: " + apiDefinition.get("title") +
                 "\nservers:\n" + listServers((HashSet<String>) apiDefinition.get("servers"));
     }

     public String getServiceName(){
         return (String) apiDefinition.get("title");
     }

     /* Data structures for store translation */
    HashMap<String, Object> apiDefinition = new HashMap<>();
     // Flags
    private Boolean insideDefinitions = false;
    private Boolean insideService = false;

    private String listServers(HashSet<String> list){
        StringBuilder out = new StringBuilder();
        for(String url:list){
            out.append("\t- url: ").append(url).append("\n");
        }
        return out.toString();
    }
     private String getTagType(String tag){
         List<String> tagTypes = Arrays.asList("types", "message", "portType", "binding", "definitions",
                                                "address", "service");

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

    @Override
    public void enterElement(XMLParser.ElementContext ctx) {
         String elementTag = ctx.Name(0).toString();
         switch (getTagType(elementTag)){
             case "definitions":
                 insideDefinitions = true;
                 break;
             case "types":
                 System.out.println("Has a type declaration");
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
             default:
                 //System.out.println(elementTag);
                 break;
         }
        //super.enterElement(ctx);
    }

    @Override
    public void exitElement(XMLParser.ElementContext ctx) {
        super.exitElement(ctx);
    }
}