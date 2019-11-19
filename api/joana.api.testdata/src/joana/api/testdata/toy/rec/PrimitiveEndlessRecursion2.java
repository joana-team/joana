package joana.api.testdata.toy.rec;

public class PrimitiveEndlessRecursion2 {

  public static void bla(String[] args){
    main(null);
  }

  public static void main(String[] args) {
    bla(args);
  }

}
