import sensitivity.Security;

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
public class Main {
    public static void main(String[] args) {
        String str = "10.";
        double d = conv(str);
        Security.leak((int) d);
        System.out.println(d);
        d = conv(str);
        Security.leak((int) d);
        System.out.println(d);
    }

    static double conv(String input) {
        double result = 0.0;
        double precision = (Security.SECRET > 0 ? 1.0 : 1.1);
        boolean point = false;
        boolean error = false;
        int pos = 0;

        while (pos < input.length() && !error) {
            char c = input.charAt(pos);
            if (Character.isDigit(c)) {
                if (point) {
                    precision = precision / 10.0;
                }
                result = 10.0 * result +
                Character.digit(c, 10);
            } else if ((c == '.') && !point) {
                point = true;
                if (pos == input.length() - 1) {
                    error = true;
                }
            } else {
                error = true;
            }
            pos = pos + 1;
        }

        if (error || (input.length() == 0)) {
            return -1.0;
        } else {
            return result * precision;
        }
    }
}
