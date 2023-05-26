package co.restursoap.restyoursoap.service;

import co.restursoap.restyoursoap.antlr.gen.XMLLexer;
import co.restursoap.restyoursoap.antlr.gen.XMLParser;
import co.restursoap.restyoursoap.antlr.translator.WSDLToOpenAPI;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.springframework.stereotype.Service;


@Service
public class SOAPservice {
    public String translateToOpenAPI(String wsdl){
        // Generate Lexer from WSDL String
        XMLLexer baseLexer = new XMLLexer(CharStreams.fromString(wsdl));

        // Create a buffer of tokens pulled from lexer
        CommonTokenStream tokens = new CommonTokenStream(baseLexer);

        // Create a parser that feeds oof the tokens buffer
        XMLParser baseParser = new XMLParser(tokens);
        ParseTree tree = baseParser.document(); // Init rule

        // Create a generic parseTree Walker can trigger callbacks
        ParseTreeWalker walker = new ParseTreeWalker();
        WSDLToOpenAPI translator = new WSDLToOpenAPI();

        // Walk the tree created during the parse, trigger callbacks
        walker.walk(translator, tree);

        return translator.getOutput();

    }
}
