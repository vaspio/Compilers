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

public class WriterVisitor extends Offset{
    Vector AllocaVars = new Vector();
    Vector<String> ArgVector = new Vector<String>();

    String filename;
    String classname;
    String IdentifierType;

    int variableCounter = -1;
    int ifCounter = -1;
    int oobCounter = -1;
    int loopCounter = -1;
    boolean InMethod=false;
    boolean InMain=false;
    boolean Left=false;
    boolean Load=false;

    public WriterVisitor(String file){
        super(file);

        file = file.substring(0, file.lastIndexOf('.'));
        filename = file+".ll";

        System.out.println("\n\nConverting java code to LLVM...");


    } 

    public String GetVariableCounter(){
        return "%_"+Integer.toString(variableCounter);
    }  
    public String CreateVariable(){
        variableCounter++;
        return "%_"+Integer.toString(variableCounter);
    }  
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
        InMain=true;

        classname = n.f1.accept(this, null);
        // System.out.println("Class: " + classname);

        String line = "define i32 @main() {";

        WriteLine(line);

        n.f14.accept(this, null);
        n.f15.accept(this, null);

        // System.out.println();

        WriteLine("\n\tret i32 0\n}\n\n");
        
        
        InMain=false;

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
        classname=null;
        classname = n.f1.accept(this, null);
        // System.out.println("Class: " + classname);

        // n.f3.accept(this, null);
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
        classname=null;
        classname = n.f1.accept(this, null);
        // System.out.println("Class: " + classname);

        super.visit(n, argu);

        // System.out.println();

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
        variableCounter = -1;

        String argumentList = n.f4.present() ? n.f4.accept(this, null) : "";


        String Type = n.f1.accept(this, null);
        String Name = n.f2.accept(this, null);
        Name = Name.substring(1);

        InMethod = true;

        CustomPair pair = new CustomPair(Name,Type);
        PairMethod = pair;

        // System.out.println(Type + " " + Name + " -AAAA- " + classname);
        String MethodType = ClassDeclared.SearchMethodName(classname, Name);


        // llvm method declaration
        String line = "define "+ConvertTypeLLVM(MethodType)+" @"+classname+"."+Name+"(i8* %this";

        String allocaParam="";
        if(!argumentList.equals("")){
            String[] argumentArray = argumentList.split("\\s*,\\s*");

            for(int i = 0; i < argumentArray.length; i++){
                // Split type and name
                String[] splitArray = argumentArray[i].split(" ");

                line += ", "+ConvertTypeLLVM(splitArray[0])+" %."+splitArray[1].substring(1);

                // %x = alloca i32
                // store i32 %.x, i32* %x
                allocaParam += "\n\t"+splitArray[1]+" = alloca "+ConvertTypeLLVM(splitArray[0]);
                allocaParam += "\n\tstore "+ConvertTypeLLVM(splitArray[0])+" %."+splitArray[1].substring(1)+", "+ConvertTypeLLVM(splitArray[0])+"* "+splitArray[1];

                AllocaVars.add(splitArray[1].substring(1));
            }
        }

        //finish parameters
        line += ") {\n" + allocaParam + "\n";
        WriteLine(line);

        //var declaration
        n.f7.accept(this, null);

        //statements 
        Load=true;  
        n.f8.accept(this, null);


        //return
        String ret = n.f10.accept(this, null);

        String f = ret.substring(0, 1);
        if(f.equals("%")) ret = GetVariableCounter();

        WriteLine("\n\tret "+ConvertTypeLLVM(IdentifierType)+" "+ret+"\n}\n");




        Load=false;
        InMethod = false;
        AllocaVars.clear();

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

     /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    @Override
    public String visit(VarDeclaration n, Void argu) throws Exception {

        String type = n.f0.accept(this, null);
        String var = n.f1.accept(this, null);


        String line = "\t"+var+" = alloca ";

        line += ConvertTypeLLVM(type);

        WriteLine(line);

        return null;
    }

     /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    @Override
    public String visit(AssignmentStatement n, Void argu) throws Exception {
        WriteLine("");

        boolean keep = Load;
        Load = true;
        String expr = n.f2.accept(this, null);
        String exprType = IdentifierType;
        Load = keep;


        Left=true;
        String id = n.f0.accept(this, null);
        String idType = IdentifierType;
        Left=false;
        // System.out.println("id:"+id+" type:"+idType+" = expr:"+expr+" type:"+exprType);


        String line = "\tstore " + ConvertTypeLLVM(exprType) +" "+expr+", "+ConvertTypeLLVM(idType)+"* "+id+"\n";

        WriteLine(line);


        Load = keep;

        return null;
    }

    /**
    * f0 -> -AndExpression()
    *       -| CompareExpression()
    *       -| PlusExpression()
    *       -| MinusExpression()
    *       -| TimesExpression()
    *       -| ArrayLookup()
    *       -| ArrayLength()
    *       -| MessageSend()
    *       -| PrimaryExpression()
    */
    @Override
    public String visit(Expression n, Void argu) throws Exception {
        String expr = n.f0.accept(this, null);

        // System.out.println("Expression:"+expr);
        return expr;
    }

    /**
    * f0 -> IntegerLiteral()
    *       -| TrueLiteral()
    *       -| FalseLiteral()
    *       -| Identifier()
    *       -| ThisExpression()
    *       -| ArrayAllocationExpression()
    *       -| AllocationExpression()
    *       -| NotExpression()
    *       -| BracketExpression()
    */
    @Override
    public String visit(PrimaryExpression n, Void argu) throws Exception {

        String expr = n.f0.accept(this, null);
        // System.out.println("primaryexpr:"+expr);
        return expr;
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    // b.set(1)
    @Override
    public String visit(MessageSend n, Void argu) throws Exception {


        String pr = n.f0.accept(this, null);
        String functionType = IdentifierType;

        // bitcast
        String curVar = GetVariableCounter();
        // WriteLine("\t"+CreateVariable()+" = bitcast i8* "+curVar+" to i8***");
        WriteLine("\t"+CreateVariable()+" = bitcast i8* "+pr+" to i8***");

        // Load vtable pointer
        curVar = GetVariableCounter();
        WriteLine("\t"+CreateVariable()+" = load i8**, i8*** "+curVar);

        boolean keep=Load;
        Load = false;

        String id = n.f2.accept(this, null);

        Load = keep;

        //get rid of %
        id = id.substring(1);

        Vector OffVector = ClassOffsets.get(functionType);
        HashMap<String, Integer> MethodsHash = (HashMap<String, Integer>) OffVector.get(1);

        if(MethodsHash.get(id) == null){
            // extended variable
            String extendedClass = ClassDeclared.GetExtendedClass(functionType);  

            OffVector = ClassOffsets.get(extendedClass);
            MethodsHash = (HashMap<String, Integer>) OffVector.get(1);
            
        }
        int offset = MethodsHash.get(id);

        curVar = GetVariableCounter();
        WriteLine("\t"+CreateVariable()+" = getelementptr i8*, i8** "+curVar+", i32 "+offset/8);

        curVar = GetVariableCounter();
        WriteLine("\t"+CreateVariable()+" = load i8*, i8** "+curVar);

        //e.x Base.get
        String check = functionType+"."+id;
        String info = MethodLLVMInfo.get(check);

        curVar = GetVariableCounter();
        WriteLine("\t"+CreateVariable()+" = bitcast i8* "+curVar+" to "+info);
        curVar = GetVariableCounter();


        String List = n.f4.accept(this, null);
        if(List != null){
            ArgVector.insertElementAt(List, 0);
        }

        String[] parts = info.split(" ");
        String type = parts[0]; 
        String type2 = parts[1]; 
   

        //%_12 = call i32 %_11(i8* %_6, i32 1)  pr=%_6
        String line = "\t"+CreateVariable()+" = call "+type+" "+curVar+"(i8* "+pr;


        String normal = ClassDeclared.SearchMethodName(functionType, id);

        CustomPair pair = new CustomPair(id,normal);
        Vector MethodBodyVector = ClassDeclared.GetMethodVector(functionType, pair);
        LinkedHashMap<String, String> ArgumentsHash = (LinkedHashMap<String, String>) MethodBodyVector.get(0);

        Iterator ArgumentsIt = ArgumentsHash.entrySet().iterator();
        int i=0;
        while(ArgumentsIt.hasNext()) {
            Map.Entry mapElement = (Map.Entry)ArgumentsIt.next();

            String variable = (String) mapElement.getKey();
            String typeArg = (String) mapElement.getValue();

            line += ", "+ConvertTypeLLVM(typeArg)+" "+ArgVector.get(i);

            i++;
        }

        WriteLine(line+")");


        IdentifierType = ConvertLLVMtoJavaType(type);
        ArgVector.clear();

        return GetVariableCounter();
    }

    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    @Override
    public String visit(ExpressionList n, Void argu) throws Exception {
        String expr = n.f0.accept(this, null);

        String tail = n.f1.accept(this, null);

        return expr;
    }

    /**
    * f0 -> ( ExpressionTerm() )*
    */
    @Override
    public String visit(ExpressionTail n, Void argu) throws Exception {
        return n.f0.accept(this, null);
    }

    /**
    * f0 -> ","
    * f1 -> Expression()
    */
    @Override
    public String visit(ExpressionTerm n, Void argu) throws Exception {
        n.f0.accept(this, null);


        String expr = n.f1.accept(this, null);
        ArgVector.add(expr);    // keep args in vector

        return expr;
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    @Override
    public String visit(AllocationExpression n, Void argu) throws Exception {
        boolean keepL = Load;
        Load=false;

        String name = n.f1.accept(this, null);
        name = name.substring(1);

        IdentifierType = name;


        //calculates bytes we need to calloc (we begin at 8 for vtable pointer)
        int bytesNeeded = 8;

        Vector ClassBodyVector = ClassDeclared.ClassDeclMap.get(name);
        HashMap<String, String> ClassVariablesHash = (HashMap<String, String>) ClassBodyVector.get(0);

        Iterator ClassVarIt = ClassVariablesHash.entrySet().iterator();
        Vector Duplicates = new Vector();
        while(ClassVarIt.hasNext()) {
            Map.Entry mapElement = (Map.Entry)ClassVarIt.next();
            String type = (String) mapElement.getValue();
            String var = (String) mapElement.getKey();

            if(type.equals("int")) bytesNeeded += 4;
            else if(type.equals("boolean")) bytesNeeded += 1;
            else bytesNeeded += 8;   

            Duplicates.add(var);
        }

        String extended = ClassDeclared.GetExtendedClass(name);
        if(!extended.equals("0")){
            ClassBodyVector = ClassDeclared.ClassDeclMap.get(extended);
            ClassVariablesHash = (HashMap<String, String>) ClassBodyVector.get(0);

            ClassVarIt = ClassVariablesHash.entrySet().iterator();
            while(ClassVarIt.hasNext()) {
                Map.Entry mapElement = (Map.Entry)ClassVarIt.next();
                String type = (String) mapElement.getValue();
                String kk = (String) mapElement.getKey();

                if(!Duplicates.contains(kk)){

                    if(type.equals("int")) bytesNeeded += 4;
                    else if(type.equals("boolean")) bytesNeeded += 1;
                    else bytesNeeded += 8;   
                }
            }
        }


        // calloc            
        String line = "\t"+CreateVariable()+" = call i8* @calloc(i32 1, i32 "+Integer.toString(bytesNeeded)+")";
        WriteLine(line);

        String keep = GetVariableCounter();

        // bitcast
        String curVar = GetVariableCounter();

        line = "\t"+CreateVariable()+" = bitcast i8* "+curVar+" to i8***";
        WriteLine(line);




        //Get the address of the first element of the vtable
        Vector ClassOffsetVector = ClassOffsets.get(name);

        HashMap<String, Integer> Variables = (HashMap<String, Integer>) ClassOffsetVector.get(0);   
        LinkedHashMap<String, Integer> Methods = (LinkedHashMap<String, Integer>) ClassOffsetVector.get(1);   

        int numofmethods = Methods.size();


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
                Map.Entry mapElement = (Map.Entry)MethodIterator.next();

                String methodname = (String) mapElement.getKey();

                Set<CustomPair> pairs = MethodHashTemp.keySet();
                for(CustomPair pair: pairs){

                    if(methodname == pair.Name){
                        OverrideMethods.add(methodname);
                    }

                }         
            } 

            //substract duplicates
            numofmethods -= OverrideMethods.size();

        }

        curVar = GetVariableCounter();

        line = "\t"+CreateVariable()+" = getelementptr ["+Integer.toString(numofmethods)+" x i8*], ["+Integer.toString(numofmethods)+" x i8*]* ";
        line += "@."+name+"_vtable, i32 0, i32 0";
        WriteLine(line);


        //Set the vtable to the correct address
        line = "\tstore i8** "+GetVariableCounter()+", i8*** "+curVar;
        WriteLine(line);

        Load = keepL;

        return keep;
    }
    
    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    @Override
    public String visit(ArrayAllocationExpression n, Void argu) throws Exception {

        String expr = n.f3.accept(this, null);

        WriteLine("\t"+CreateVariable()+" = add i32 1, "+expr);


        String curVar = GetVariableCounter();
        WriteLine("\t"+CreateVariable()+" = icmp sge i32 "+curVar+", 1");
        WriteLine("\tbr i1 "+GetVariableCounter()+", label %nsz_ok_0, label %nsz_err_0");
        
        // throw error if needed
        WriteLine("\n\tnsz_err_0:\n\tcall void @throw_nsz()\n\tbr label %nsz_ok_0\n\n\tnsz_ok_0:");

        WriteLine("\t"+CreateVariable()+" = call i8* @calloc(i32 "+curVar+", i32 4)");

        curVar = GetVariableCounter();
        WriteLine("\t"+CreateVariable()+" = bitcast i8* "+curVar+" to i32*");

        WriteLine("\tstore i32 "+expr+", i32* "+GetVariableCounter()+"\n");


        IdentifierType = "int[]";
        return GetVariableCounter();
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(TimesExpression n, Void argu) throws Exception {

        String p1 = n.f0.accept(this, null);

        String p2 = n.f2.accept(this, null);

        // System.out.println("p1 "+p1+" p2 "+p2);
        WriteLine("\t"+CreateVariable()+" = mul i32 "+p1+", "+p2+"\n");


        return GetVariableCounter();
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "&&"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(AndExpression n, Void argu) throws Exception {
        boolean keep=Load;
        Load=true; 

        String CurrentVar = GetVariableCounter();

        String p1 = n.f0.accept(this, null);

        //check if integer literal
        String temp = GetVariableCounter();
        if(!temp.equals(CurrentVar)) WriteLine("\tbr i1 "+GetVariableCounter()+", label %exp_res_1, label %exp_res_0\n");
        else  WriteLine("\tbr i1 "+p1+", label %exp_res_1, label %exp_res_0\n");
        CurrentVar = temp;


        // WriteLine("\tbr i1 "+GetVariableCounter()+", label %exp_res_1, label %exp_res_0\n");
        WriteLine("\texp_res_0:\n\tbr label %exp_res_3\n\n\texp_res_1:");

        String p2 = n.f2.accept(this, null);
        temp = GetVariableCounter();
        if(!temp.equals(CurrentVar)) p2 = temp;

        WriteLine("\tbr label %exp_res_2\n\n\texp_res_2:\n\tbr label %exp_res_3\n");
        WriteLine("\texp_res_3:\n\t"+CreateVariable()+" = phi i1  [ 0, %exp_res_0 ], [ "+p2+", %exp_res_2 ]");


        Load=keep;
        return GetVariableCounter();
    }

    /**
    * f0 -> "this"
    */
    @Override
    public String visit(ThisExpression n, Void argu) throws Exception {
        IdentifierType = classname;

        n.f0.accept(this, null);
        return "%this";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(CompareExpression n, Void argu) throws Exception {
        boolean keep=Load;
        Load=true;

        String CurrentVar = GetVariableCounter();


        String p1 = n.f0.accept(this, null);

        //check if integer literal
        String temp = GetVariableCounter();
        if(!temp.equals(CurrentVar)) p1 = temp;
        CurrentVar = temp;


        String p2 = n.f2.accept(this, null);
        temp = GetVariableCounter();
        if(!temp.equals(CurrentVar)) p2 = temp;

        WriteLine("\t"+CreateVariable()+" = icmp slt i32 "+p1+", "+p2);

        Load=keep;
        return GetVariableCounter();
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(PlusExpression n, Void argu) throws Exception {
        String p1 = n.f0.accept(this, null);

        String p2 = n.f2.accept(this, null);

        WriteLine("\t"+CreateVariable()+" = add i32 "+p1+", "+p2);

        return GetVariableCounter();
    }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n, Void argu) throws Exception {
        String p1 = n.f0.accept(this, null);

        String p2 = n.f2.accept(this, null);

        WriteLine("\t"+CreateVariable()+" = sub i32 "+p1+", "+p2);


        return GetVariableCounter();
    }


    /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    @Override
    public String visit(ArrayLookup n, Void argu) throws Exception {
        boolean keep=Load;
        Load=true;
        oobCounter++;

        String id = n.f0.accept(this, null);

        String old = GetVariableCounter();
        String curVar = old;
        WriteLine("\t"+CreateVariable()+" = load i32, i32* "+curVar);
        curVar = GetVariableCounter();
        
        //index
        String index = n.f2.accept(this, null);
        WriteLine("\t"+CreateVariable()+" = icmp sge i32 "+index+", 0");

        String needVar = GetVariableCounter();
        WriteLine("\t"+CreateVariable()+" = icmp slt i32 "+index+", "+curVar);

        curVar = GetVariableCounter();
        WriteLine("\t"+CreateVariable()+" = and i1 "+needVar+", "+curVar);


        WriteLine("\tbr i1 "+GetVariableCounter()+", label %oob_ok_"+oobCounter+", label %oob_err_"+oobCounter+"\n");
        //throw
        WriteLine("\toob_err_"+oobCounter+":\n\tcall void @throw_oob()\n\tbr label %oob_ok_"+oobCounter+"\n\n\toob_ok_"+oobCounter+":");

        WriteLine("\t"+CreateVariable()+" = add i32 1, "+index);

        curVar = GetVariableCounter();
        WriteLine("\t"+CreateVariable()+" = getelementptr i32, i32* "+old+", i32 "+curVar);
  
        curVar = GetVariableCounter();
        WriteLine("\t"+CreateVariable()+" = load i32, i32* "+curVar+"\n");


        // System.out.println(GetVariableCounter()+"LOOKUP \n");
        IdentifierType = "int";

        Load=keep;
        return GetVariableCounter();
    }

    /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    @Override
    public String visit(PrintStatement n, Void argu) throws Exception {
        boolean keep = Load;
        Load=true;

        String expr = n.f2.accept(this, null);

        String f = expr.substring(0, 1);
        if(f.equals("%")) expr = GetVariableCounter();

        WriteLine("\tcall void (i32) @print_int(i32 "+expr+")\n");


        Load=keep;
        return null;
    }

       /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    @Override  
    public String visit(IfStatement n, Void argu) throws Exception {
        ifCounter++;

        String expr = n.f2.accept(this, null);

        // br i1 %_1, label %if_then_0, label %if_else_0
        WriteLine("\tbr i1 "+expr+", label %if_then_"+ifCounter+", label %if_else_"+ifCounter+"\n");


        WriteLine("\tif_else_"+ifCounter+":");
        int kelse = ifCounter;

        String st2 = n.f6.accept(this, null);
        WriteLine("\tbr label %if_end_"+ifCounter+"\n");
        int keep = ifCounter;


        WriteLine("\tif_then_"+kelse+":");
        String st1 = n.f4.accept(this, null);

        WriteLine("\tbr label %if_end_"+keep+"\n");


        WriteLine("\tif_end_"+keep+":");

        return null;
    }

      /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    @Override
    public String visit(WhileStatement n, Void argu) throws Exception {
        loopCounter++;

        WriteLine("\n\tbr label %loop"+loopCounter+"\n\n\tloop"+loopCounter+":");
        String expr = n.f2.accept(this, null);

        // br i1 %_7, label %loop1, label %loop2
        int ll = loopCounter;
        WriteLine("\tbr i1 "+expr+", label %loop"+(++loopCounter)+", label %loop"+(++loopCounter));

        int prev = loopCounter-1;
        WriteLine("\n\tloop"+prev+":");
        prev = loopCounter;

       
        String statement = n.f4.accept(this, null);

        WriteLine("\tbr label %loop"+ll);

        
        WriteLine("\n\tloop"+prev+":");
        

        return null;
    }

      /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    @Override
    public String visit(ArrayAssignmentStatement n, Void argu) throws Exception {

        boolean keep=Load;
        Load=true;
        oobCounter++;

        //identifier
        String id = n.f0.accept(this, null);

        String old = GetVariableCounter();
        String curVar = old;
        WriteLine("\t"+CreateVariable()+" = load i32, i32* "+curVar);
        curVar = GetVariableCounter();

        //index
        String index = n.f2.accept(this, null);
        WriteLine("\t"+CreateVariable()+" = icmp sge i32 "+index+", 0");

        String needVar = GetVariableCounter();
        WriteLine("\t"+CreateVariable()+" = icmp slt i32 "+index+", "+curVar);

        curVar = GetVariableCounter();
        WriteLine("\t"+CreateVariable()+" = and i1 "+needVar+", "+curVar);

        WriteLine("\tbr i1 "+GetVariableCounter()+", label %oob_ok_"+oobCounter+", label %oob_err_"+oobCounter+"\n");
        WriteLine("\toob_err_"+oobCounter+":\n\tcall void @throw_oob()\n\tbr label %oob_ok_"+oobCounter+"\n\n\toob_ok_"+oobCounter+":");

        WriteLine("\t"+CreateVariable()+" = add i32 1, "+index);

        curVar = GetVariableCounter();
        WriteLine("\t"+CreateVariable()+" = getelementptr i32, i32* "+old+", i32 "+curVar);
        curVar = GetVariableCounter();

        // = expr
        String expr = n.f5.accept(this, null);

        WriteLine("\tstore i32 "+expr+", i32* "+curVar+"\n");


        // System.out.println("identidfier "+id+" index "+index+" expr "+expr);
        Load=keep;
        return null;
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
    public String visit(TrueLiteral n, Void argu) throws Exception {
        IdentifierType = "boolean";
        n.f0.accept(this, argu);
        return "1";
    }

    /**
    * f0 -> "false"
    */
    public String visit(FalseLiteral n, Void argu) throws Exception {
        IdentifierType = "boolean";
        n.f0.accept(this, argu);
        return "0";
    }

    @Override
    public String visit(IntegerLiteral n, Void argu) throws Exception {
        IdentifierType = "int";
        return n.f0.toString();
    
    }
     /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    @Override
    public String visit(BracketExpression n, Void argu) throws Exception {

        return n.f1.accept(this, null);
    }
    /**
    * f0 -> "!"
    * f1 -> PrimaryExpression()
    */
    public String visit(NotExpression n, Void argu) throws Exception {
        n.f0.accept(this, null);

        				// %_48 = xor i1 1, %_51

        String p = n.f1.accept(this, null);

        WriteLine("\t"+CreateVariable()+" = xor i1 1, "+p);
    
        return GetVariableCounter();
    }

    @Override
    public String visit(Identifier n, Void argu) {
        String id = n.f0.toString();

        String type=null;
        String line=null;
        boolean Already = false;
        if(classname!=null){

            if(InMethod){
                // look in variable methods
                type = ClassDeclared.SearchMethodVector(classname, PairMethod, id);
            }

            if(type == null){
                type = ClassDeclared.SearchClassIdentifier(classname, id);


                if(type == null){
                    
                    // look at extended class vars
                    String extendedClass = ClassDeclared.GetExtendedClass(classname);  

                    if(!extendedClass.equals("0")){
                        type = ClassDeclared.SearchClassIdentifier(extendedClass, id);
                    }

                }

                // found the type, check if we want getelementptr
                if(Load && !InMain){

                    Vector OffVector = ClassOffsets.get(classname);
                    HashMap<String, Integer> VariablesHash = (HashMap<String, Integer>) OffVector.get(0);

                    if(VariablesHash.get(id) == null){
                        // extended variable
                        String extendedClass = ClassDeclared.GetExtendedClass(classname);  

                        OffVector = ClassOffsets.get(extendedClass);
                        VariablesHash = (HashMap<String, Integer>) OffVector.get(0);
                        
                    }

                    // keep offset + 8
                    int offset = VariablesHash.get(id) + 8;

                    WriteLine("\t"+CreateVariable()+" = getelementptr i8, i8* %this, i32 "+Integer.toString(offset));
                    
                    String curVar = GetVariableCounter();
                    String newVar = CreateVariable();
                    WriteLine("\t"+newVar+" = bitcast i8* "+curVar+" to "+ConvertTypeLLVM(type)+"*");

                    id = GetVariableCounter().substring(1);


                    if(Left){ // we dont want to load var
                        Already = true;
                    }

                }            
            }
        }
        else return id;

        IdentifierType = type;
        if(Left==true ) Already=true;
        

        //for LLVM 
        id = "%"+id;

        if(Load && !Already){
            WriteLine("\t"+CreateVariable()+" = load "+ConvertTypeLLVM(type)+", "+ConvertTypeLLVM(type)+"* "+id);
            id = GetVariableCounter();
        }

        return id;
    }

    public String ConvertTypeLLVM(String type){
        String temp=null;

        if(type.equals("int")) temp = "i32";
                    
        else if(type.equals("int[]")) temp = "i32*";
        else if(type.equals("boolean")) temp = "i1";
        else temp = "i8*";    // class type

        return temp;
    }

    public String ConvertLLVMtoJavaType(String type){
        String temp=null;

        if(type.equals("i32")) temp = "int";
                    
        else if(type.equals("i32*")) temp = "int[]";
        else if(type.equals("i1")) temp = "boolean";
        else temp = "ClassType";    // class type

        return temp;
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