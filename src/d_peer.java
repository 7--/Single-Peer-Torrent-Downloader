import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.io.*;

public class d_peer {
	
	private int indicator;
	boolean in_special_map;
	private InputStreamReader in;
	private String popo;
	boolean correct = false;
	private String peer_id="";
	private String IP="";
	private int port=0;
	private int test=0;
	

	
public d_peer(InputStreamReader in) throws IOException{
	extract_ip(in);
	System.out.print(port);
	
}
	
	public void extract_ip(InputStreamReader in) throws IOException{
		this.in=in;
		indicator = 0;
	    while (port ==0){
	    	bdecode();
	    }
	   
	return;
		
	}
	 public void bdecode() throws IOException
	  {
	    indicator = getNextIndicator();
	    if (indicator == -1)
	      return;
	    
	    
	    if (indicator >= '0' && indicator <= '9')
	      popo = Bytes();
	    else if (indicator == 'i')
	     test = Number();
// don't need to implement list/dictionaries for single peer
	    else
	      throw new InvalidBEncodingException
		("Unknown indicator '" + indicator + "'");
	    if (correct){
	    	port = test;
	    }
	    if (IP.equals("128.6.171.130")){
    	peer_id.concat(popo);
    	correct = true;
	    }
	    if (popo.equals("128.6.171.130")){
	    	IP.concat(popo);
	    }
	   	   
	  return;
	  }
	 
	 public String getIP(){
		 return IP;
	 }
	 public int getPort(){
		 return port;
	 }
	 public String getId(){
		 return peer_id;
	 }
	 
	 public String Bytes() throws IOException{
		 int c = getNextIndicator();
		 int up = 0;
		 do {
			 up = up*10+c;
			 c = getNextIndicator();
		 } while (c<=0 || c>=9);
		
		 char [] answer = new char[up];
		    int num = c - '0';
		    if (num < 0 || num > 9)
		      throw new InvalidBEncodingException("Number expected, not '"
							  + (char)c + "'");
		    indicator = 0;

		    c = in.read();
		    if (c != ':')
			      throw new InvalidBEncodingException("Colon expected, not '"
								  + (char)c + "'");
		    c=in.read();
		    int off = 0;
		    while (up>0)
		      {

		    	
			answer[off] = (char)c;
			c=in.read();
			up--;
		      }

		   

		    return String.valueOf(answer);
	 }
	 
	 public int Number() throws IOException{
		 
		 int c = getNextIndicator();
		    if (c != 'i')
		      throw new InvalidBEncodingException("Expected 'i', not '"
							  + (char)c + "'");
		    indicator = 0;

		    c = in.read();
		    if (c == '0')
		      {
			c = in.read();
			if (c == 'e')
			  return 0;
			else
			  throw new InvalidBEncodingException("'e' expected after zero,"
							      + " not '" + (char)c + "'");
		      }

		    char[] chars = new char[256];
		    int off = 0;
		    int answer = 0;
		    if (c == '-')
		      {
			c = in.read();
			if (c == '0')
			  throw new InvalidBEncodingException("Negative zero not allowed");
			
			 while (c >= 0 && c <= 9)
		      {

			 c = c - '0';
			answer = answer*(-10)-  c;
			c = in.read();
			 if (c < '1' || c > '9')
			      throw new InvalidBEncodingException("Invalid Integer start '"
								  + (char)c + "'");
		      }
		
		      }
		    else{
		    	
				 while (c >= 0 && c <= 9)
			      {

				 c = c - '0';
				answer = answer*(10)+  c;
				c = in.read();
				 if (c < '1' || c > '9')
				      throw new InvalidBEncodingException("Invalid Integer start '"
									  + (char)c + "'");
			      }
		    }

		


		    if (c != 'e')
		      throw new InvalidBEncodingException("Integer should end with 'e'");

		   
		    return answer;
	 }
	 
	 
	  public void BencodingException(final String message)
	    {
	        System.out.print("BencodingException");
	    }
	 
	 
	 
	  public int getNextIndicator() throws IOException
	  {
	    if (indicator == 0)
	      {
		indicator = in.read();
	      }
	    return indicator;
	  }
}
