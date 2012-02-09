package controllers;

public class Security extends controllers.Secure.Security {

	static boolean authenticate(String username, String password) {
        return "123".equals(password);
    }

	static void onDisconnected() {
		DashboardController.get();
	}
}
