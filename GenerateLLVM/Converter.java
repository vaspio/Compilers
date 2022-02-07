import syntaxtree.*;
import visitor.*;

import java.util.*;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.FileOutputStream;

public class Converter {
    String filename;

    public Converter(String name){
        // System.out.println("off:"+ClassDeclared.ClassDeclMap);

        name = name.substring(0, name.lastIndexOf('.'));
        filename = name+".ll";

        // create file
        try(Writer file = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"))){
            file.write("; "+filename+"\n");
        }
        catch(IOException ex){
            System.out.println(ex);
        }
        
    }   

    // write line in file
    public void WriteLine(String line){

        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
            out.println(line);
        } 
        catch (IOException ex) {
            System.out.println(ex);
        }
        finally{
            // close file
            if(out != null){
                out.close();
            }
        }

    }

}