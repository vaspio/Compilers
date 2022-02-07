class LinkedList{
    public static void main(String[] a){
    }
}

class Element {
    int Age ;          
    int Salary ;
    boolean Married ;

    // Initialize some class variables
    public boolean Init(int v_Age, int v_Salary, boolean v_Married){
	Age = v_Age ;
	Salary = v_Salary ;
	Married = v_Married ;
	return true ;
    }

    public int GetAge(){
	return Age ;
    }
    
    public int GetSalary(){
	return Salary ;
    }

    public boolean GetMarried(){
	return Married ;
    }

    // This method returns true if the object "other"
    // has the same values for age, salary and 
    public boolean Equal(Element other){
	boolean ret_val ;
	int aux01 ;
	int aux02 ;
	int nt ;
	ret_val = true ;

	aux01 = other.GetAge();
	if (!(this.Compare(aux01,Age))) ret_val = false ;
	else { 
	    aux02 = other.GetSalary();
	    if (!(this.Compare(aux02,Salary))) ret_val = false ;
	    else 
		if (Married) 
		    if (!(other.GetMarried())) ret_val = false;
		    else nt = 0 ;
		else
		    if (other.GetMarried()) ret_val = false;
		    else nt = 0 ;
	}

	return ret_val ;
    }

    // This method compares two integers and
    // returns true if they are equal and false
    // otherwise
    public boolean Compare(int num1 , int num2){
	boolean retval ;
	int aux02 ;
	retval = false ;
	aux02 = num2 + 1 ;
	if (num1 < num2) retval = false ;
	else if (!(num1 < aux02)) retval = false ;
	else retval = true ;
	return retval ;
    }

}

class List{
    Element elem ;
    List next ;
    boolean end ;

    // Initialize the node list as the last node
    public boolean Init(){
	end = true ;
	return true ;
    }

    // Initialize the values of a new node
    public boolean InitNew(Element v_elem, List v_next, boolean v_end){
	end = v_end ;
	elem = v_elem ;
	next = v_next ;
	return true ;
    }
    
    // Insert a new node at the beginning of the list
    public List Insert(Element new_elem){
        List aux02 ;
        return aux02 ;
    }
    
    
    // Update the the pointer to the next node
    public boolean SetNext(List v_next){
	next = v_next ;
	return true ;
    }
 
    

}
    
