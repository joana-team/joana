package edu.kit.joana.wala.summary.test;

class BasicTestClass {

    static class A {
        int b(int a){
            return a;
        }
    }

    static class B extends A {
        @Override int b(int a) {
            return a + 1;
        }
    }

    public static void main(String[] args) {
        bla(args.length);
    }
    static int bla(int a) {
        return a;
    }

    static int blub(int a, int b){
        return a * b;
    }
}
