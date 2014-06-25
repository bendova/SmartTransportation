package dataStores.userData;

import java.util.UUID;

public interface IUserDataStore
{
	void addUserData(UUID userID, UserData userDataStore);
}
