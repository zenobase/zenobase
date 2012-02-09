package au.com.louth;

import play.Logger;

public class Toyota implements Car {

	@Override
	public String drive() {
		return "Driving a Toyota";
	}

}
