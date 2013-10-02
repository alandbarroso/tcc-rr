package chichiCreator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Create By: Mostafa Shabani
 * Date: 2/22/11
 * Time: 3:24 PM
 */
public class ChichiReplace {
    public static void repl() {
        try {
            File sourceFile = new File("poly/Paris.pol");
//            File file = new File("data/temp");
//            sourceFile.renameTo(file);

            BufferedReader bf = new BufferedReader(new FileReader(sourceFile));
            FileWriter fr = new FileWriter("poly/Paris.pol0", true);

            String str = bf.readLine();
            int id = 0;
            while (str != null) {
                if (!str.isEmpty()) {
                    if (str.startsWith("-1")) {
                        fr.write("\r\n");
                        fr.write("id: " + (id++) + "\r\n");
//                        fr.write("neighbour: \r\n");
                    } else {
                        String[] values = str.split(",");
                        int x = Integer.parseInt(values[0]);
                        int y = Integer.parseInt(values[1]);
                        fr.write(x + "," + y + "\r\n");
                    }
                }
                str = bf.readLine();
            }
            fr.flush();
            fr.close();
//            file.delete();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public static void main(String[] args) {
        repl();
    }
}
