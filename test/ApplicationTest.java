import org.junit.*;

import play.test.*;
import play.mvc.*;
import play.mvc.Http.*;

import models.*;

public class ApplicationTest extends FunctionalTest {

    @Test
    public void testAdminSecurity() {
        Response response = GET("/admin");
        assertStatus(302, response);
    }

    @Test
    public void testLanding() {
        Response response = GET("/landing");
        assertStatus(200, response);
    }

    @Test
    public void testAbout() {
        Response response = GET("/about");
        assertStatus(200, response);
    }

    @Test
    public void testHome() {
        Response response = GET("/home");
        assertStatus(302, response);
    }
    
}