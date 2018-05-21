import java.io.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.StringBuilder;

public class Interpret
{

   public static String reverse(String s) {
      return new StringBuilder(s).reverse().toString();
   }

   public static void main(String argv[])
   {
     // try {
     //   InputStream streamm;
     //   for(int i = 0; i <= 9999; i++ ){
     //    String oi = String.valueOf(i);
     //    oi = reverse(oi);
     //    streamm = new ByteArrayInputStream(oi.getBytes("UTF-8"));
     //    Num2Ext.Parser n2ee = new Num2Ext.Parser(new Num2Ext.Scanner(streamm));
     //    n2ee.Parse();
     //    System.out.print(i);
     //    System.out.println("  " + n2ee.result);
     //  }
     //  }catch (Exception e) {
     //    e.printStackTrace();
     //
     //  }
      String str;
      System.out.println("Calculadora por extenso (digite 's' para sair)");
      System.out.println("Exemplos de expressoes: dois mais cinco, sete vezes (doze mais um)");
      while (true) {
         try {
            System.out.print("? ");
            BufferedReader bufferRead = new
                           BufferedReader(new InputStreamReader(System.in));
            str = bufferRead.readLine();
            if (str.equalsIgnoreCase("s"))
                    System.exit(0);
            InputStream stream = new
                                 ByteArrayInputStream(str.getBytes("UTF-8"));
            ExtCalc.Parser ecp = new ExtCalc.Parser(new ExtCalc.Scanner(stream));
            ecp.Parse();
            String result = String.valueOf(ecp.result);
            System.out.println(result);
             result = reverse(String.valueOf(ecp.result));
           stream = new ByteArrayInputStream(result.getBytes("UTF-8"));
           Num2Ext.Parser n2e = new Num2Ext.Parser(new Num2Ext.Scanner(stream));
           n2e.Parse();
           System.out.println("  " + n2e.result);


         } catch(IOException e) {
             e.printStackTrace();
        }
     }
  }
}
