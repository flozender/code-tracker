public class Foo {
    static Foo
            f1 = new Foo(){
                public String toString() {
                    return newMethod();
                }
            };

    private static String newMethod() {
        return "a" + "b";
    }

    static Foo f2 = new Foo(){}
    ;

}