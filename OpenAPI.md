OpenAPI
=======
- works with default generated Java client
- works with servers generated with jaxrs-cxf
  - reason: the abstract API classes are far simpler
  ```java
  public class UserApiServiceImpl implements UserApi {
    public User getUserByName(String username) {
      // TODO: Implement...
      return null;
    }
  }
  ```

Server
------

### How to find API Server Interface?
It's an interface with the following properties:
- has an annotation `@io.swagger.annotations.Api`
- ends with `Api` → get the name
- has at least one method with an `javax.ws.rs.[fully uppercase]` annotation
  - … and `io.swagger.annotations.{ApiResponse,ApiOperation}`
  - these are the operations from the OpenApi spec

### How to make the Api impl methods known?
Setting:
```java
PetApiServiceImpl implementor = new PetApiServiceImpl();
String address = "http://localhost:9000/petapi";
Endpoint.publish(address, implementor, new LoggingFeature());
```
Problem:
```java
public class Provider {
  /* … */
  public static Provider provider() {
    try {
      Object provider = getProviderUsingServiceLoader();
      if (provider == null) {
        provider = FactoryFinder.find(JAXWSPROVIDER_PROPERTY, DEFAULT_JAXWSPROVIDER);
      }
      if (!(provider instanceof Provider)) {
        Class pClass = Provider.class;
        String classnameAsResource = pClass.getName().replace('.', '/') + ".class";
        ClassLoader loader = pClass.getClassLoader();
        if (loader == null) {
          loader = ClassLoader.getSystemClassLoader();
        }
        URL targetTypeURL = loader.getResource(classnameAsResource);
        throw new LinkageError(
                "ClassCastException: attempting to cast" + provider.getClass().getClassLoader().getResource(classnameAsResource)
                        + "to" + targetTypeURL.toString());
      }
      return (Provider) provider;
    } catch (WebServiceException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new WebServiceException("Unable to createEndpointReference Provider", ex);
    }
  }
  /* … */
}
```
is hard for program analysis.

Proposed solution (using the preprocessor):
1. create a new method per Api impl that calls all Api methods
  ```java
  switch ((int)Math.random() * 10000) {
    case 1:
      impl.method1([dummy arguments])
    // …
  }
  ```
2. call this method before every call of a method from a class of the
  `javax.xml.ws` package that gets passed an impl object