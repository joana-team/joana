package joana.api.testdata.toy.rec;

public class PrimitiveEndlessRecursion3 {

  public static void bla(String[] args){
    main(null);
    blub(args);
  }

  public static void blub(String[] args){
    blob(args);
  }

  public static void blob(String[] args){

  }

  public static void main(String[] args) {
    bla(args);
  }

}
