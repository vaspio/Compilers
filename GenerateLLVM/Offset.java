import syntaxtree.*;
import visitor.*;

import java.util.*;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;


public class Offset extends SymbolTableVisitor{
    HashMap<String, CustomPair> KeepOffset = new HashMap<String, CustomPair>();

    String inputfile;
    Converter con;
    static HashMap<String, String> MethodLLVMInfo = new HashMap<String, String>();
    static HashMap<String, Vector> ClassOffsets = new HashMap<String, Vector>();

    public Offset(String name){
        inputfile = name;
        // System.out.println("off:"+ClassDeclared.ClassDeclMap);
    }

    public void TraverseClass(String classname){
        Vector ClassBodyVector = ClassDeclared.GetClassVector(classname);

        int offsetVar = 0;
        int offsetMethod = 0;

        String extended = ClassDeclared.GetExtendedClass(classname);

        // keep extended offset
        if(!extended.equals("0")){
            Iterator OffIterator = KeepOffset.entrySet().iterator();

            while(OffIterator.hasNext()) {
                Map.Entry mapElement = (Map.Entry)OffIterator.next();

                String nameclass = (String) mapElement.getKey();

                if(nameclass.equals(extended)){ 
                    // keep extended offset

                    CustomPair name = (CustomPair)mapElement.getValue();

                    offsetVar = Integer.parseInt(name.Name);
                    offsetMethod = Integer.parseInt(name.Type);
                }

                
            }
        }

        HashMap<String, String> VariablesHash = (HashMap<String, String>) ClassBodyVector.get(0);

        Iterator VariableIterator = VariablesHash.entrySet().iterator();

        // System.out.println("---Variables---");

        while(VariableIterator.hasNext()) {
            Map.Entry mapElement = (Map.Entry)VariableIterator.next();

            // System.out.println(classname + "." + mapElement.getKey() + ": " + offsetVar);


            // keep offset of variables
            Vector ClassOffsetVector = ClassOffsets.get(classname);
            HashMap<String, Integer> VariablesOff = (HashMap<String, Integer>) ClassOffsetVector.get(0);    

            VariablesOff.put((String)mapElement.getKey(), offsetVar);


            String type = (String) mapElement.getValue();

            if(type.equals("int")){
                offsetVar += 4;
            }
            else if(type.equals("boolean")){
                offsetVar += 1;
            }
            else offsetVar += 8;        // int[] and class types
        

        }

        HashMap<String, String> MethodHash = (HashMap<String, String>) ClassBodyVector.get(1);
        Iterator MethodIterator = MethodHash.entrySet().iterator();

        // System.out.println("---Methods---");
        while(MethodIterator.hasNext()) {
            Map.Entry mapElement = (Map.Entry)MethodIterator.next();
            
            CustomPair name = (CustomPair)mapElement.getKey();

            // keep offset of methods
            Vector ClassOffsetVector = ClassOffsets.get(classname);
            HashMap<String, Integer> VariablesOff = (HashMap<String, Integer>) ClassOffsetVector.get(1);    

            VariablesOff.put(name.Name, offsetMethod);

            // check for overidding
            if(!extended.equals("0")){
                String k = ClassDeclared.SearchMethodName(extended, name.Name);
                
                // not override
                if(k == null){
                    // System.out.println(classname + "." + name.Name + ": " + offsetMethod);
                    offsetMethod = offsetMethod + 8;
                }
            }
            else{// normal function
                // System.out.println(classname + "." + name.Name + ": " + offsetMethod);
                offsetMethod = offsetMethod + 8;    // pointer +8
            }
            

        }

        // keep offset
        String s = String.valueOf(offsetVar);  
        String s2 = String.valueOf(offsetMethod);  
        CustomPair pair = new CustomPair(s, s2);
        KeepOffset.put(classname, pair);
        
    }


    public void TraverseStructure(){

        con = new Converter(inputfile);


        Iterator ClassIterator = ClassDeclared.ClassDeclMap.entrySet().iterator();

        while(ClassIterator.hasNext()) {
            Map.Entry mapElement = (Map.Entry)ClassIterator.next();
            
            String name = (String) mapElement.getKey();

            // System.out.println("\n-----------Class "+name+"-----------");


            // keep offsets in a structure
            Vector ClassBodyVector = new Vector();
            ClassOffsets.put(name, ClassBodyVector);

            // a hashmap for variables and methods and their offsets
            HashMap<String, Integer> Variables = new HashMap<String, Integer>();
            LinkedHashMap<String, Integer> Methods = new LinkedHashMap<String, Integer>();
            ClassBodyVector.add(Variables);
            ClassBodyVector.add(Methods);

            // traverse the class and make data structures
            TraverseClass(name);

            // System.out.println("\n-----ConvertStruct "+ClassOffsets);




            // now build vtable //////////////////

            // find number of methods
            int numofmethods = Methods.size();

            String extended = ClassDeclared.GetExtendedClass(name);


            // keep overriden methods to a vector
            Vector OverrideMethods = new Vector();
            if(!extended.equals("0")){

                Vector ClassBodyVectorTemp = ClassDeclared.ClassDeclMap.get(extended);
                // Get vector[1] where we have stored method name and body
                LinkedHashMap<CustomPair, Vector> MethodHashTemp = (LinkedHashMap<CustomPair, Vector>) ClassBodyVectorTemp.get(1);   

                numofmethods += MethodHashTemp.size();


                // iterate
                Iterator MethodIterator = Methods.entrySet().iterator();
                while(MethodIterator.hasNext()) {
                    mapElement = (Map.Entry)MethodIterator.next();

                    String methodname = (String) mapElement.getKey();

                    Set<CustomPair> pairs = MethodHashTemp.keySet();
                    for(CustomPair pair: pairs){

                        if(methodname == pair.Name){
                            OverrideMethods.add(methodname);
                        }

                    }         
                } 

                numofmethods -= OverrideMethods.size();

            }

            // new vtable
            String line = "@."+name+"_vtable = global [" + numofmethods + " x i8*] ";
       
            if(numofmethods == 0){
                line += "[]";
            }
            else{
                line += "[";

                String keepLLVMMethod="";

                // look at methods
                Iterator OffIterator = Methods.entrySet().iterator();
                while(OffIterator.hasNext()) {
                    line += "\n\ti8* bitcast (";
                    mapElement = (Map.Entry)OffIterator.next();

                    String methodname = (String) mapElement.getKey();
                    int offset = (Integer)mapElement.getValue();

                    // take type of method
                    String type = ClassDeclared.SearchMethodName(name, methodname);

                    if(type.equals("int")){
                        line += "i32 ";
                        keepLLVMMethod += "i32 ";
                    }
                    
                    else if(type.equals("int[]")){
                        line += "i32* ";
                        keepLLVMMethod += "i32* ";
                    }
                    else if(type.equals("boolean")){
                        line += "i1 ";
                        keepLLVMMethod += "i1 ";
                    }
                    else{
                        line += "i8* ";    // class type
                        keepLLVMMethod += "i8* ";  
                    }
                    

                    // check method arguments
                    line += "(i8*";
                    keepLLVMMethod += "(i8*";

                    CustomPair pair = new CustomPair(methodname,type);
                    Vector MethodBodyVector = ClassDeclared.GetMethodVector(name, pair);
                    LinkedHashMap<String, String> ArgumentsHash = (LinkedHashMap<String, String>) MethodBodyVector.get(0);

                    int numofargs = ArgumentsHash.size();

                    if(numofargs != 0){
                        Iterator ArgIterator = ArgumentsHash.entrySet().iterator();
                        while(ArgIterator.hasNext()) {
                            Map.Entry args = (Map.Entry)ArgIterator.next();

                            String argname = (String) args.getKey();
                            String argtype = (String) args.getValue();

                            if(argtype.equals("int")){
                                line += ",i32";
                                keepLLVMMethod += ",i32";
                            }
                    
                            else if(argtype.equals("int[]")){
                                line += ",i32*";
                                keepLLVMMethod += ",i32*";
                            }
                            else if(argtype.equals("boolean")){
                                line += ",i1";
                                keepLLVMMethod += ",i1";
                            }
                            else{ 
                                line += ",i8*";  
                                keepLLVMMethod += ",i8*";  
                            }


                        }
                    }

                    line += ")* ";
                    keepLLVMMethod += ")*";

                    MethodLLVMInfo.put(name+"."+methodname, keepLLVMMethod);

                    keepLLVMMethod = "";


                    // name of method

                    String tempname = name;

                    line += "@"+tempname+"."+methodname+" to i8*)";

                    if(OffIterator.hasNext()) line += ",";
                }
                // see in extended class
                if(!extended.equals("0")){
                    Vector ClassBodyVectorTemp = ClassDeclared.ClassDeclMap.get(extended);
                    LinkedHashMap<CustomPair, Vector> MethodHashTemp = (LinkedHashMap<CustomPair, Vector>) ClassBodyVectorTemp.get(1);    

                    Set<CustomPair> pairs = MethodHashTemp.keySet();
                    for(CustomPair pair: pairs){

                        if(OverrideMethods.contains(pair.Name)) continue;

                        line += ",\n\ti8* bitcast (";

                        if(pair.Type.equals("int")){
                            line += "i32 ";
                            keepLLVMMethod += "i32 ";
                        }
                        
                        else if(pair.Type.equals("int[]")){
                            line += "i32* ";
                            keepLLVMMethod += "i32* ";
                        }
                        else if(pair.Type.equals("boolean")){
                            line += "i1 ";
                            keepLLVMMethod += "i1 ";
                        }
                        else{
                            line += "i8* ";    // class type
                            keepLLVMMethod += "i8* ";  
                        }


                            
                        // check method arguments
                        line += "(i8*";

                        Vector MethodBodyVector = ClassDeclared.GetMethodVector(extended,pair);
                        HashMap<String, String> ArgumentsHash = (HashMap<String, String>) MethodBodyVector.get(0);

                        
                        int numofargs = ArgumentsHash.size();

                        if(numofargs != 0){
                            Iterator ArgIterator = ArgumentsHash.entrySet().iterator();
                            while(ArgIterator.hasNext()) {
                                Map.Entry args = (Map.Entry)ArgIterator.next();

                                String argname = (String) args.getKey();
                                String argtype = (String) args.getValue();

                                if(argtype.equals("int")){
                                    line += ",i32";
                                    keepLLVMMethod += ",i32";
                                }
                        
                                else if(argtype.equals("int[]")){
                                    line += ",i32*";
                                    keepLLVMMethod += ",i32*";
                                }
                                else if(argtype.equals("boolean")){
                                    line += ",i1";
                                    keepLLVMMethod += ",i1";
                                }
                                else{ 
                                    line += ",i8*";  
                                    keepLLVMMethod += ",i8*";  
                                }


                            }
                        }

                        line += ")* ";

                        line += "@"+extended+"."+pair.Name+" to i8*)";


                        MethodLLVMInfo.put(extended+"."+pair.Name, keepLLVMMethod);
                        keepLLVMMethod = "";
                    }

                }
                line += "\n]";


            }
            // write vtable to file
            con.WriteLine(line);
        }
        WriteBoilerplate();
        WriteMethods();


    }

    public void WriteMethods(){

        FileInputStream fis = null;
        try{
            fis = new FileInputStream(inputfile);
            MiniJavaParser parser = new MiniJavaParser(fis);
            Goal root = parser.Goal();


            // Visit 
            WriterVisitor Visitor = new WriterVisitor(inputfile);
            root.accept(Visitor, null);





        }
        catch(ParseException ex){
            System.out.println(ex.getMessage());
        }
        catch(FileNotFoundException ex){
            System.err.println(ex.getMessage());
        }
        catch(Exception ex){
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


    public void WriteBoilerplate(){
        String line = "\ndeclare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\ndeclare void @exit(i32)\n\n"
            +"@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n@_cNSZ = constant [15 x i8] c\"Negative size\\0a\\00\"\n\n"
            +"define void @print_int(i32 %i) {\n\t%_str = bitcast [4 x i8]* @_cint to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n\tret void\n}\n\n"
            +"define void @throw_oob() {\n\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n\t"
            +"call i32 (i8*, ...) @printf(i8* %_str)\n\tcall void @exit(i32 1)\n\tret void\n}\n\n"
            +"define void @throw_nsz() {\n\t%_str = bitcast [15 x i8]* @_cNSZ to i8*\n\t"
            +"call i32 (i8*, ...) @printf(i8* %_str)\n\t"
            +"call void @exit(i32 1)\n\tret void\n}\n\n";

        con.WriteLine(line);

    }

}
