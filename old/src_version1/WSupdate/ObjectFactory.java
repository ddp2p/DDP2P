
package WSupdate;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the WSupdate package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: WSupdate
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Downloadable }
     * 
     */
    public Downloadable createDownloadable() {
        return new Downloadable();
    }

    /**
     * Create an instance of {@link Test }
     * 
     */
    public Test createTest() {
        return new Test();
    }

    /**
     * Create an instance of {@link History }
     * 
     */
    public History createHistory() {
        return new History();
    }

    /**
     * Create an instance of {@link VersionInfo }
     * 
     */
    public VersionInfo createVersionInfo() {
        return new VersionInfo();
    }

    /**
     * Create an instance of {@link TesterInfo }
     * 
     */
    public TesterInfo createTesterInfo() {
        return new TesterInfo();
    }

    /**
     * Create an instance of {@link TestDef }
     * 
     */
    public TestDef createTestDef() {
        return new TestDef();
    }

}
