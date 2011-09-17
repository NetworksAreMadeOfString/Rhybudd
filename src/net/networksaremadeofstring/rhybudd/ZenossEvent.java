package net.networksaremadeofstring.rhybudd;

public class ZenossEvent {
	private String evid;
	private int Count;
	private String lastTime;
	private String device;
	private String summary;
	private String eventState;
	private String firstTime;
	private String severity;
	
	
	 // Constructor for the Ticket class
    public ZenossEvent(String _evid, String _device, String _summary, String _eventState, String _severity) 
    {
            super();
            this.evid = _evid;
            this.device = _device;
            this.summary = _summary;
            this.eventState = _eventState;
            this.severity = _severity;
    }
    
    public String getEVID()
    {
    	return this.evid;
    }
    
    public String getDevice()
    {
    	return this.device;
    }
    
    public String getSummary()
    {
    	return this.summary;
    }
    
    public String getEventState()
    {
    	return this.eventState;
    }
    
    public String getSeverity()
    {
    	return this.severity;
    }
    

}
