import java.util.*;

class ClassDeclarationStruct {
    HashMap<String, Vector> ClassDeclMap = new HashMap<String, Vector>();
    
    public ClassDeclarationStruct(){        
    }
    
    public void CreateClass(String classname){
        // Create the basic structure to save the body and name of class 

        // New vector for the body of class
        Vector ClassBodyVector = new Vector();
        
        // Keep vector and name of class
        ClassDeclMap.put(classname, ClassBodyVector);
        
        // Create 2 seperate hashmaps for Variables and Methods of class
        HashMap<String, String> VariablesMap = new HashMap<String, String>();
        LinkedHashMap<CustomPair, Vector> MethodsMap = new LinkedHashMap<CustomPair, Vector>();
        
        ClassBodyVector.add(VariablesMap);
        ClassBodyVector.add(MethodsMap);
        ClassBodyVector.add("0");
    }

    public void CreateExtendedClass(String classname, String ExtendedClass){
        // New vector for the body of class
        Vector ClassBodyVector = new Vector();
        
        // Keep vector and name of class
        ClassDeclMap.put(classname, ClassBodyVector);
        
        // Create 2 seperate hashmaps for Variables and Methods of class
        HashMap<String, String> VariablesMap = new HashMap<String, String>();
        LinkedHashMap<CustomPair, Vector> MethodsMap = new LinkedHashMap<CustomPair, Vector>();
        
        ClassBodyVector.add(VariablesMap);
        ClassBodyVector.add(MethodsMap);
        ClassBodyVector.add(ExtendedClass);
    }

    
    public Vector CreateMethodsBodyVector(){
        // Create vector for variables and argument list of methods

        Vector MethodsBodyVector = new Vector();
        
        LinkedHashMap<String, String> MethodsArgs = new LinkedHashMap<String, String>();
        HashMap<String, String> MethodsVars = new HashMap<String, String>();
        
        MethodsBodyVector.add(MethodsArgs);
        MethodsBodyVector.add(MethodsVars);

        return MethodsBodyVector;
    }

    public Vector GetClassVector(String classname){
        return ClassDeclMap.get(classname);
    }
    
    public Vector GetMethodVector(String classname, CustomPair method){
        // Get body of our class
        Vector ClassBodyVectorTemp = ClassDeclMap.get(classname);
        
        // Get vector[1] where we have stored method name and body
        LinkedHashMap<CustomPair, Vector> MethodHashTemp = (LinkedHashMap<CustomPair, Vector>) ClassBodyVectorTemp.get(1);         

        return MethodHashTemp.get(method);
    }

    public String SearchClassVector(String classname, String find){
        Vector ClassBodyVector = GetClassVector(classname);

        HashMap<String, String> VariablesHash = (HashMap<String, String>) ClassBodyVector.get(0);

        find = VariablesHash.get(find);

        return find;
    }

    public String SearchMethodVector(String classname, CustomPair method, String find){
        Vector MethodBodyVector = GetMethodVector(classname,method);

        HashMap<String, String> VariablesHash = (HashMap<String, String>) MethodBodyVector.get(1);

        String keepFind = find;

        find = VariablesHash.get(find);

        if(find == null){
            HashMap<String, String> ArgumentsHash = (HashMap<String, String>) MethodBodyVector.get(0);
            find = ArgumentsHash.get(keepFind);
        }

        return find;
    }

    public String SearchMethodName(String classname, String find){
        Vector ClassBodyVectorTemp = ClassDeclMap.get(classname);


        // Get vector[1] where we have stored method name and body
        LinkedHashMap<CustomPair, Vector> MethodHashTemp = (LinkedHashMap<CustomPair, Vector>) ClassBodyVectorTemp.get(1);    


        Set<CustomPair> pairs = MethodHashTemp.keySet();
        for(CustomPair pair: pairs){
            // if found return its type
            if(find.equals(pair.Name)){
                return pair.Type;
            }
        }

        // Didnt found it. Look at extended class if exists
        String extended = GetExtendedClass(classname);

        if(!extended.equals("0")){
            ClassBodyVectorTemp = ClassDeclMap.get(extended);
            MethodHashTemp = (LinkedHashMap<CustomPair, Vector>) ClassBodyVectorTemp.get(1); 

            pairs = MethodHashTemp.keySet();
            for(CustomPair pair: pairs){
                // if found return its type
                if(find.equals(pair.Name)){
                    return pair.Type;
                }
            }

        }


        return null;
    }

    public String SearchClassIdentifier(String classname, String id){
        Vector ClassBodyVector = ClassDeclMap.get(classname);

        Map.Entry mapElement;

        // look at class variables
        HashMap<String, String> ClassVariablesHash = (HashMap<String, String>) ClassBodyVector.get(0);

        Iterator ClassVarIt = ClassVariablesHash.entrySet().iterator();
        while(ClassVarIt.hasNext()) {
            mapElement = (Map.Entry)ClassVarIt.next();

            String variable = (String) mapElement.getKey();
            String type = (String) mapElement.getValue();

            if(variable.equals(id)) return type;

        }

        return null;
    }


    public String GetExtendedClass(String classname){

        Vector ClassBodyVector = GetClassVector(classname);
        String extendedClass = (String)ClassBodyVector.get(2);  

        return extendedClass;
    }

}

class CustomPair{
    // pair for method name and type
    String Name;
    String Type;

    public CustomPair(String name, String type){
        this.Name = name;
        this.Type = type;
    }

    @Override
    public String toString()
    {
         return "Pair:<" + Type + ", " + Name+">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof CustomPair)) return false;

        CustomPair pair = (CustomPair) o;

        if(!Name.equals(pair.Name)) {
            return false;
        }
        if(!Type.equals(pair.Type)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        String result = Name + Type;
        return result.hashCode();
        // return result.length();
    }
}
