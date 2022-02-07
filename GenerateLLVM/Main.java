import syntaxtree.*;
import visitor.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length == 0){
            System.err.println("Usage: java Main <inputFile> .. <inputFile>");
            System.exit(1);
        }

        int j = 0;
        for(int i = args.length; i>0 ; i--){
            FileInputStream fis = null;
            try{
                fis = new FileInputStream(args[j]);
                MiniJavaParser parser = new MiniJavaParser(fis);

                Goal root = parser.Goal();

                System.out.println("___________________________________________________________________________");
                System.err.println("Program parsed successfully. (filepath:"+args[j]+")");

                // Visit and create symbol table
                SymbolTableVisitor SymVisitor = new SymbolTableVisitor();
                root.accept(SymVisitor, null);


                Offset oset = new Offset(args[j]);
                oset.TraverseStructure();


                System.err.println("Success\n");




                SymVisitor.ClassDeclared.ClassDeclMap.clear();
                j++;

            }
            catch(ParseException ex){
                System.out.println(ex.getMessage());
            }
            catch(FileNotFoundException ex){
                System.err.println(ex.getMessage());
            }
            finally{
                try{
                    if(fis != null) fis.close();
                }
                catch(IOException ex){
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}

