import org.junit.*;
import play.test.*;
import play.mvc.*;
import play.mvc.Http.*;
import models.*;

public class ApplicationTest extends FunctionalTest {

    @Test
    public void car1() {
        Response response = GET("/car1");
        assertContentEquals("Driving a Toyota", response);
    }
    
    @Test
    public void car2() {
        Response response = GET("/car2");
        assertContentEquals("Driving a Ford", response);
    }
    
    @Test
    public void car3() {
        Response response = GET("/car3");
        assertContentEquals("Driving a Holden", response);
    }
    
}