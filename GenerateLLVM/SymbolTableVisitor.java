import syntaxtree.*;
import visitor.*;

import java.util.*;
import java.io.IOException;

public class SymbolTableVisitor extends GJDepthFirst<String, Void>{

    static ClassDeclarationStruct ClassDeclared = new ClassDeclarationStruct();
    static String classname;
    static boolean InMethod;
    static CustomPair PairMethod;
    static String MainClass;
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, Void argu) throws Exception {
        classname = n.f1.accept(this, null);
        // System.out.println("Class: " + classname);

        MainClass = classname;

        ClassDeclared.CreateClass(classname);
        

        n.f1.accept(this, null);
        n.f11.accept(this, null);
        n.f14.accept(this, null);
        n.f15.accept(this, null);
                
        return null;
    }
    
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, Void argu) throws Exception {
        classname = n.f1.accept(this, null);
        // System.out.println("Class: " + classname);

        ClassDeclared.CreateClass(classname);
        
        n.f1.accept(this, null);
        n.f3.accept(this, null);
        n.f4.accept(this, null);
                
        return null;
    }
    
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {
        classname = n.f1.accept(this, null);
        // System.out.println("Class extended: " + n.f3.accept(this, null));
        
        String extended = n.f3.accept(this, null);
        ClassDeclared.CreateExtendedClass(classname,extended);

        n.f1.accept(this, null);
        n.f5.accept(this, null);
        n.f6.accept(this, null);
        
        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, Void argu) throws Exception {
        InMethod = true;

        String argumentList = n.f4.present() ? n.f4.accept(this, null) : "";

        String myType = n.f1.accept(this, null);
        String myName = n.f2.accept(this, null);

        // Get body of our class
        Vector ClassBodyVectorTemp = ClassDeclared.GetClassVector(classname);
        
        // Get vector[1] where we want to store method name and body
        LinkedHashMap<CustomPair, Vector> VariableHashTemp = (LinkedHashMap<CustomPair, Vector>) ClassBodyVectorTemp.get(1);

        // Create vector to store variables and argument and put it in the structure
        Vector vec = ClassDeclared.CreateMethodsBodyVector();

        // Get hashmap of methods arguments
        HashMap<String, String> ArgsHashTemp = (HashMap<String, String>) vec.get(0);

        if(argumentList != ""){
            // split the argument list
            String[] argumentArray = argumentList.split("\\s*,\\s*");
            for(int i = 0; i < argumentArray.length; i++){
                
                // Split type for name
                String[] splitArray = argumentArray[i].split(" ");

                // Put in structure arguments and their type
                ArgsHashTemp.put(splitArray[1], splitArray[0]);
            }      
        }  

        CustomPair pair = new CustomPair(myName,myType);

        // Store method type,name(pair) and vector of arguments in structure
        VariableHashTemp.put(pair,vec);

        PairMethod = pair;
        String var = n.f7.accept(this, null);

        // System.out.println(myType + " " + myName + " -- " + argumentList);

        InMethod = false;
        PairMethod = null;
        return null;
    }

    @Override
    public String visit(VarDeclaration n, Void argu) throws Exception {

        String Type = n.f0.accept(this, argu);
        String Var = n.f1.accept(this, argu);

        // Get body of our class
        Vector ClassBodyVectorTemp = ClassDeclared.ClassDeclMap.get(classname);

        if(!InMethod){
            // Get vector[0] where we want to store variables of class
            HashMap<String, String> VariableHashTemp = (HashMap<String, String>) ClassBodyVectorTemp.get(0);
         
            // Store method variable and type in structure
            VariableHashTemp.put(Var,Type);
        }
        else{
            // Get vector[1] to access Method variable vector
            HashMap<String, Vector> MethodHashTemp = (HashMap<String, Vector>) ClassBodyVectorTemp.get(1);

            // Get vector with current method
            Vector MethodVectorTemp = MethodHashTemp.get(PairMethod);

            HashMap<String, String> VariableHashTemp = (HashMap<String, String>) MethodVectorTemp.get(1);

            // Store method variable and type in structure
            VariableHashTemp.put(Var,Type);
        }

        return null;
     }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, Void argu) throws Exception {
        String ret = n.f0.accept(this, null);

        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n, Void argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, Void argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += ", " + node.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, Void argu) throws Exception{
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);
        return type + " " + name;
    }

    @Override
    public String visit(ArrayType n, Void argu) {
        return "int[]";
    }

    public String visit(BooleanType n, Void argu) {
        return "boolean";
    }

    public String visit(IntegerType n, Void argu) {
        return "int";
    }

    @Override
    public String visit(Identifier n, Void argu) throws Exception{
        return n.f0.toString();
    }
}
