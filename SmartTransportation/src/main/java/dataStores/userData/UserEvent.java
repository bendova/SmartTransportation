package dataStores.userData;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import uk.ac.imperial.presage2.util.location.Location;
import agents.User;

public class UserEvent
{
	private StringProperty mEventName;
	private StringProperty mEventDetails;
	private IntegerProperty mTimestep;
	private StringProperty mState;
	private StringProperty mLocation;
	
	public UserEvent(String eventName, String eventDetails, int timestep, 
			User.State state, Location location)
	{
		assert(eventName != null);
		assert(eventDetails != null);
		assert(timestep >= 0);
		assert(state != null);
		assert(location != null);
		
		mEventName = new SimpleStringProperty(eventName);
		mEventDetails = new SimpleStringProperty(eventDetails);
		mTimestep = new SimpleIntegerProperty(timestep);
		mState = new SimpleStringProperty(state.toString());
		mLocation = new SimpleStringProperty(location.toString());
	}
	
	public StringProperty eventNameProperty()
	{
		return mEventName;
	}
	public StringProperty eventDetailsProperty()
	{
		return mEventDetails;
	}
	public IntegerProperty timestepProperty()
	{
		return mTimestep;
	}
	public StringProperty stateProperty()
	{
		return mState;
	}
	public StringProperty locationProperty()
	{
		return mLocation;
	}
}
