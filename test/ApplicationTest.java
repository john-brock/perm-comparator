import org.junit.*;
import play.test.*;
import play.mvc.*;
import play.mvc.Http.*;
import models.*;

public class ApplicationTest extends FunctionalTest {

    @Test
    public void testIndexPage() {
        Response response = GET("/");
        assertIsOk(response);
        assertContentType("text/html", response);
        assertCharset(play.Play.defaultWebEncoding, response);
    }
    
    @Test
    public void testSuccessPage() {
    	Response response = GET("/success");
    	assertIsOk(response);
    	assertContentType("text/html", response);
    	assertCharset(play.Play.defaultWebEncoding, response);
    }
    
}