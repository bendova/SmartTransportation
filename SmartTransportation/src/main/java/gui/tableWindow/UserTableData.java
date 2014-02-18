package gui.tableWindow;

import java.util.UUID;

import agents.User.TransportMode;
import agents.User.TransportPreference;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class UserTableData 
{
	private StringProperty mNameProperty;
	private StringProperty mIDProperty;
	private BooleanProperty mHasReachedDestinationProperty;
	private DoubleProperty mTravelTimeProperty;
	private DoubleProperty mTravelTimeTargetProperty;
	private StringProperty mTransportPreferenceProperty;
	private StringProperty mTransportModeUsedProperty;
	
	public UserTableData(String userName, UUID userID, boolean hasReachedDestination,
			double travelTime, double targetTravelTime,
			TransportPreference transportPreference, TransportMode transportModeUsed)
	{
		mNameProperty = new SimpleStringProperty(userName);
		mIDProperty = new SimpleStringProperty(userID.toString());
		mHasReachedDestinationProperty = new SimpleBooleanProperty(hasReachedDestination);
		mTravelTimeProperty = new SimpleDoubleProperty(travelTime);
		mTravelTimeTargetProperty = new SimpleDoubleProperty(targetTravelTime);
		mTransportPreferenceProperty = new SimpleStringProperty(transportPreference.toString());
		mTransportModeUsedProperty = new SimpleStringProperty(transportModeUsed.toString());
	}
	
	public StringProperty nameProperty()
	{
		return mNameProperty;
	}
	public StringProperty idProperty()
	{
		return mIDProperty;
	}
	public BooleanProperty hasReachedDestinationProperty()
	{
		return mHasReachedDestinationProperty;
	}
	public DoubleProperty travelTimeProperty()
	{
		return mTravelTimeProperty;
	}
	public DoubleProperty travelTimeTargetProperty()
	{
		return mTravelTimeTargetProperty;
	}
	public StringProperty transportPreferenceProperty()
	{
		return mTransportPreferenceProperty;
	}
	public StringProperty transportModeUsedProperty()
	{
		return mTransportModeUsedProperty;
	}
	
}
