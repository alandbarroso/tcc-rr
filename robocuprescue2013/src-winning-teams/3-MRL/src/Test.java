import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * User: roohola
 * Date: 7/6/11
 * Time: 1:02 AM
 */
public class Test {
    public static void main(String[] args) {
        try {
            // Encode a String into bytes
            String inputString = "kjshdfjg gfed hgdflkjwefkhwefkjshdfjg gfed hgdflkjwefkhwefkjshdfjg gfed hgdflkjwefkhwef";
            byte[] input = inputString.getBytes();

            // Compress the bytes
            byte[] output = new byte[100];
            Deflater compresser = new Deflater();
            compresser.setInput(input);
            compresser.finish();
            int compressedDataLength = compresser.deflate(output);
            System.out.println("compressedDataLength:"+compressedDataLength);
            // Decompress the bytes
            Inflater decompresser = new Inflater();
            decompresser.setInput(output, 0, compressedDataLength);
            byte[] result = new byte[100];
            int resultLength = decompresser.inflate(result);
            decompresser.end();

            // Decode the bytes into a String
            String outputString = new String(result, 0, resultLength);
            System.out.println("len:"+outputString.length()+" out:"+outputString);
        } catch (java.util.zip.DataFormatException ex) {
            // handle
        }
    }
}
