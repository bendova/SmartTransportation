package agents.helpers;

import java.util.List;

import agents.User.TransportPreference;

import conversations.userMediator.messages.messageData.ITransportServiceRequest;

import transportOffers.TransportOffer;

public interface ITransportServiceRecord 
{
	ITransportServiceRequest getTransportServiceRequest(); 
	List<TransportOffer> getTransportOffers();
	void addTransportOffer(TransportOffer offer);
	void removeTransportOffer(TransportOffer offer);
	void applyTransportPreference(TransportPreference pref, TransportOffer transportOffer);
	void sendTransportOffers();
	void sortTransportOffers();
}
