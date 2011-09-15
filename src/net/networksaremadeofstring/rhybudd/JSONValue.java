package net.networksaremadeofstring.rhybudd;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;

public class JSONValue {
	 /**
     * parse into java object from input source.
     * @param in
     * @return instance of : JSONObject,JSONArray,String,Boolean,Long,Double or null
     */
    public static Object parse(Reader in){
            try{
                    JSONParser parser=new JSONParser();
                    return parser.parse(in);
            }
            catch(Exception e){
                    return null;
            }
    }
    
    public static Object parse(String s){
            StringReader in=new StringReader(s);
            return parse(in);
    }

	public static String toJSONString(HashMap reqData) {
		// TODO Auto-generated method stub
		return null;
	}

}
