package co.restursoap.restyoursoap.antlr.translator;

import co.restursoap.restyoursoap.antlr.gen.XMLParser;
import co.restursoap.restyoursoap.antlr.gen.XMLParserBaseListener;

public class WSDLToOpenAPI extends XMLParserBaseListener {
     public String getOutput(){
         return " doe: \"a deer, a female deer\"\n" +
                 " ray: \"a drop of golden sun\"\n" +
                 " pi: 3.14159\n" +
                 " xmas: true\n" +
                 " french-hens: 3\n" +
                 " calling-birds:";
     }

    @Override
    public void enterElement(XMLParser.ElementContext ctx) {
        System.out.println(ctx.Name().get(0));
        //super.enterElement(ctx);
    }
}
