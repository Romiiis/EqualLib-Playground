public class Primitives {
    boolean bool;
    Boolean boolObj;

    volatile int i;
    Integer intObj;

    transient long l;
    Long longObj;

    double d;
    Double doubleObj;

    float f;
    Float floatObj;

    byte b;
    Byte byteObj;

    short s;
    Short shortObj;

    char c;
    Character charObj;

    String str;

    public Primitives() {
        boolObj = true;

        intObj = 0;

        longObj = 0L;

        doubleObj = 0.0;

        floatObj = 0.0f;

        byteObj = (byte) 0;

        shortObj = (short) 0;

        charObj = '\u0000';

        str = null;


    }

}