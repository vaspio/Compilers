import syntaxtree.*;
import visitor.*;

import java.util.*;
import java.io.IOException;

public class TypeCheckVisitor extends SymbolTableVisitor{
    static boolean ReturnType;
    Vector<String> ArgVector = new Vector<String>();


    public TypeCheckVisitor(){
        // System.out.println(""+ClassDeclared.ClassDeclMap);
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
        classname = n.f1.accept(this, null);

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

        String argumentList = n.f4.present() ? n.f4.accept(this, null) : "";

        String myType = n.f1.accept(this, null);
        String myName = n.f2.accept(this, null);

        CustomPair pair = new CustomPair(myName,myType);
        PairMethod = pair;

        n.f8.accept(this, null);
        
        boolean found = false;
        if(argumentList != ""){
            // split the argument list
            String[] argumentArray = argumentList.split("\\s*,\\s*");
            for(int i = 0; i < argumentArray.length; i++){
                
                // Split type for name
                String[] splitArray = argumentArray[i].split(" ");

                if(!splitArray[0].equals("int") && !splitArray[0].equals("boolean") && !splitArray[0].equals("int[]")){

                    // look at class names
                    Set<String> keys = ClassDeclared.ClassDeclMap.keySet();
                    for(String key: keys){

                        // found type 
                        if(splitArray[0].equals(key)) 
                            found = true;
                    }

                    if(!found) throw new Exception("Argument Type error!");
                }
            }      
        }  

        String ret = n.f10.accept(this, null);


        // check if return type matches
        if(!myType.equals(ret)){
            throw new Exception("Return Type error!");
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
        n.f0.accept(this, null);
        n.f1.accept(this, null);
        
        String expr = n.f2.accept(this, null);

        if(!expr.equals("boolean")) throw new Exception("Type error!");

        n.f3.accept(this, null);
        n.f4.accept(this, null);
        n.f5.accept(this, null);
        n.f6.accept(this, null);
        
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
        n.f0.accept(this, null);
        n.f1.accept(this, null);
        
        String expr = n.f2.accept(this, null);

        if(!expr.equals("boolean")) throw new Exception("Invalid type!");

        n.f3.accept(this, null);
        n.f4.accept(this, null);

        
        return null;
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
        n.f0.accept(this, null);

        String expr = n.f2.accept(this, null);

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

        String id = n.f0.accept(this, null);

        String type = ClassDeclared.SearchClassVector(classname, id);
        if(type == null){
            type = ClassDeclared.SearchMethodVector(classname, PairMethod, id);

            // variable doesnt exist
            if(type == null) throw new Exception("Invalid variable name!");
        }


        n.f1.accept(this, null);
        String index = n.f2.accept(this, null);

        if(!index.equals("int")) throw new Exception("Invalid index!");

        n.f3.accept(this, null);
        n.f4.accept(this, null);
        String expr = n.f5.accept(this, null);

        // keep type without []
        String[] parts = type.split("\\[");
        type = parts[0];
        

        // different types
        if(!type.equals(expr)){
            throw new Exception("Type Error!");
        }

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

        String id = n.f0.accept(this, null);

        // search vectors for that variable
        String type = ClassDeclared.SearchClassVector(classname, id);
        if(type == null){
            String extended = ClassDeclared.GetExtendedClass(classname);

            if(!extended.equals("0")){
                type = ClassDeclared.SearchClassVector(extended, id);
            }

            if(type == null)
                type = ClassDeclared.SearchMethodVector(classname, PairMethod, id);

            // variable doesnt exist
            if(type == null) throw new Exception("Invalid variable name!");
        }


        n.f1.accept(this, null);
        String expr = n.f2.accept(this, null);
        n.f3.accept(this, null);


        // different types
        if(!type.equals(expr)){
            // check first if we have extended type
            boolean found = false;

            Set<String> keys = ClassDeclared.ClassDeclMap.keySet();
            for(String key: keys){

                // found type 
                if(type.equals(key)) 
                    found = true;
            }

            if(found){
                // our type is a class so we look at extended
                String extended = ClassDeclared.GetExtendedClass(type);

                if(!extended.equals(expr)) throw new Exception("Assignment Type Error!");
                
            }
            else throw new Exception("Assignment Type Error!");

        }



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
    *       | PrimaryExpression()
    */
    @Override
    public String visit(Expression n, Void argu) throws Exception {
        String type = n.f0.accept(this, null);

        return type;
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(PlusExpression n, Void argu) throws Exception {

        String type = n.f0.accept(this, null);
        n.f1.accept(this, null);
        String type2 = n.f2.accept(this, null);

        // addition with int only
        if(!type.equals("int") || !type2.equals("int")){
            throw new Exception("Type Error!");
        }

        return type;
    }

       /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(MinusExpression n, Void argu) throws Exception {

        String type = n.f0.accept(this, null);
        n.f1.accept(this, null);
        String type2 = n.f2.accept(this, null);

        // int only
        if(!type.equals("int") || !type2.equals("int")){
            throw new Exception("Type Error!");
        }

        return type;
    }

    @Override
    public String visit(TimesExpression n, Void argu) throws Exception {

        String type = n.f0.accept(this, null);
        n.f1.accept(this, null);
        String type2 = n.f2.accept(this, null);

        // int only
        if(!type.equals("int") || !type2.equals("int")){
            throw new Exception("Type Error!");
        }

        return type;
    }

    @Override
    public String visit(AndExpression n, Void argu) throws Exception {

        String type = n.f0.accept(this, null);
        n.f1.accept(this, null);
        String type2 = n.f2.accept(this, null);

        // boolean only
        if(!type.equals("boolean") || !type2.equals("boolean")){
            throw new Exception("Type Error!");
        }

        return type;
    }

    @Override
    public String visit(CompareExpression n, Void argu) throws Exception {

        String type = n.f0.accept(this, null);
        n.f1.accept(this, null);
        String type2 = n.f2.accept(this, null);

        // int only
        if(!type.equals("int") || !type2.equals("int")){
            throw new Exception("Type Error!");
        }

        return "boolean";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    @Override
    public String visit(ArrayLookup n, Void argu) throws Exception {

        String type = n.f0.accept(this, null);
        n.f1.accept(this, null);
        String type2 = n.f2.accept(this, null);

        // int only
        if(!type.equals("int[]") || !type2.equals("int")){
            throw new Exception("Type Error!");
        }

        return "int";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    @Override
    public String visit(ArrayLength n, Void argu) throws Exception {

        String type = n.f0.accept(this, null);
        n.f1.accept(this, null);
        n.f2.accept(this, null);

        // int only
        if(!type.equals("int")){
            throw new Exception("Type Error!");
        }

        return type;
    }


    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    @Override
    public String visit(MessageSend n, Void argu) throws Exception {

        // accept Primary Expression
        String primary = n.f0.accept(this, null);
        n.f1.accept(this, null);

        // see extended classes
        String extended = ClassDeclared.GetExtendedClass(classname);

        // keep flag if class extended
        boolean extendFlag = false;
        if(!extended.equals("0")){
            extendFlag = true;
        }

        // check for invalid classnames
        if(!primary.equals(classname)){
            boolean found = false;

            Set<String> keys = ClassDeclared.ClassDeclMap.keySet();
            for(String key: keys){

                // found type 
                if(primary.equals(key)) 
                    found = true;
            }

            if(!found) throw new Exception("Invalid Class name!");
        }

        // accept Method name
        String id = n.f2.accept(this, null);
        String MethodType = ClassDeclared.SearchMethodName(primary, id);

        if(MethodType == null) throw new Exception("Invalid Method name!");
        
        n.f3.accept(this, null);
        String ExprList = n.f4.accept(this, null);
        n.f5.accept(this, null);

        if(ExprList != null){
            ArgVector.insertElementAt(ExprList, 0);
        }

        // check errors with arguments
        CustomPair pair = new CustomPair(id,MethodType);
        Vector MethodBodyVector = ClassDeclared.GetMethodVector(primary, pair);

        // check extended too
        if(MethodBodyVector == null){
            if(extendFlag){
                MethodBodyVector = ClassDeclared.GetMethodVector(extended, pair);
            }
            else throw new Exception("Invalid Method name!");
        }

        LinkedHashMap<String, String> ArgumentsHash = (LinkedHashMap<String, String>) MethodBodyVector.get(0);

        // make a vector with argument values
        Vector<String> TempVector = new Vector<String>();
        for(String value : ArgumentsHash.values()) {
            TempVector.add(value);
        }

        // System.out.println("ArgVector"+ArgVector+" "+TempVector);


        if(ArgVector.size() == TempVector.size()){

            for(int i=0 ; i<ArgVector.size() ; i++){

                if(!(ArgVector.get(i)).equals(TempVector.get(i))){
                    boolean found = false;

                    Set<String> keys = ClassDeclared.ClassDeclMap.keySet();
                    for(String key: keys){

                        // found type 
                        if((ArgVector.get(i)).equals(key)) 
                            found = true;
                    }

                    if(found){
                        // our type is a class so we look at extended
                        extended = ClassDeclared.GetExtendedClass(ArgVector.get(i));

                        if(!extended.equals(TempVector.get(i))) throw new Exception("Invalid Method arguments!");
                    }
                    else throw new Exception("Invalid Method arguments!");
                    
                }
            }
        }   

        ArgVector.clear();

        return MethodType;
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
    * f0 -> -IntegerLiteral()
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
        ReturnType = true;

        String type = n.f0.accept(this, null);

        ReturnType = false;
        return type;
    }

    @Override
    public String visit(IntegerLiteral n, Void argu) throws Exception {
        n.f0.accept(this, null);
        return "int";
    
    }
    @Override
    public String visit(TrueLiteral n, Void argu) throws Exception {
        n.f0.accept(this, null);
        return "boolean";
    }
    @Override
    public String visit(FalseLiteral n, Void argu) throws Exception {
        n.f0.accept(this, null);
        return "boolean";
    }
    @Override
    public String visit(ThisExpression n, Void argu) throws Exception {
        n.f0.accept(this, null);
        return classname;
    }
    @Override
    public String visit(ArrayAllocationExpression n, Void argu) throws Exception {
        n.f0.accept(this, null);
        n.f1.accept(this, null);
        n.f2.accept(this, null);
        
        n.f3.accept(this, null);

        return "int[]";

    }
    @Override
    public String visit(AllocationExpression n, Void argu) throws Exception {
        n.f0.accept(this, null);

        ReturnType = false;

        String id = n.f1.accept(this, null);

        String extended = ClassDeclared.GetExtendedClass(id);


        if(!extended.equals("0")){
            if(!id.equals(classname)) return extended;
        } 
        
        return id;
    }
    @Override
    public String visit(NotExpression n, Void argu) throws Exception {
        n.f0.accept(this, null);
        
        return n.f1.accept(this, null);
    }
    @Override
    public String visit(BracketExpression n, Void argu) throws Exception {
        n.f0.accept(this, null);
        
        return n.f1.accept(this, null);
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

        String id = n.f0.toString();

        if(ReturnType){ // if we have primary expression return type of identifier
            String keepId = id;

            // Get type of variable
            id = ClassDeclared.SearchMethodVector(classname, PairMethod, id);

            // Didnt find variable in method vector
            if(id == null){

                id = ClassDeclared.SearchClassVector(classname, keepId);

                // didnt find variable in class
                if(id == null){
                    // see extended variables
                    String extendedClass = ClassDeclared.GetExtendedClass(classname);  

                    if(extendedClass.equals("0")) throw new Exception("Invalid variable name!");

                    id = ClassDeclared.SearchClassVector(extendedClass,keepId);

                    // invalid variable
                    if(id == null) throw new Exception("Invalid variable name!");
                }
            }
        }

        return id;
    }
}
