import java.io.*;
import java.util.*;

public class Main{

    public static void main(String[] args) {
        String inputPath = "src/Compressed.bin";
        String outputPath = "src/Decompressed.txt";
        new HuffmanDecompressor(inputPath, outputPath);
    }
}

class Node {
    String value;
    int frequency;
    Node left;
    Node right;

    public Node(String value, int frequency) {
        this.value = value;
        this.frequency = frequency;
    }
}

class HuffmanDecompressor {
    private Node root;
    private HashMap<String, Integer> map = new HashMap<>();
    private ArrayList<String> mapKeys = new ArrayList<>();//stores keys to access the map
    private int validBits;

    public HuffmanDecompressor(String inputPath, String outputPath) {
        this.map = new HashMap<>();
        this.mapKeys = new ArrayList<>();

        //read the key
        byte[] compressedData = readHeaderAndData(inputPath);

        if (compressedData != null) {
            //rebuild the tree from map data
            rebuildTree();

            System.out.println("Successfully built Huffman tree.");

            //decode bits into text
            decompress(compressedData, outputPath);
        }
    }

    private byte[] readHeaderAndData(String inputPath) {
        try (FileInputStream fis = new FileInputStream(inputPath);
             DataInputStream dis = new DataInputStream(fis)) {

            //number of entries
            int size = dis.readInt();

            //read the key and the item's frequency
            for (int i = 0; i < size; i++) {
                String key = dis.readUTF();//read the key
                int freq = dis.readInt();//read the frequency
                map.put(key, freq);
                mapKeys.add(key);
            }

            //read the total number of bits to decode
            this.validBits = dis.readInt();

            return dis.readAllBytes();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void rebuildTree() {
        //build the Huffman tree nodes
        ArrayList<Node> nodes = new ArrayList<>();
        for (String key : mapKeys) {
            nodes.add(new Node(key, map.get(key)));
        }

        //repeat the greedy logic, combine two smallest until only root remains
        while (nodes.size() > 1) {
            nodes.sort((a, b) -> a.frequency - b.frequency);

            Node left = nodes.remove(0);
            Node right = nodes.remove(0);
            Node parent = new Node(null, left.frequency + right.frequency);
            parent.left = left;
            parent.right = right;

            nodes.add(parent);
        }
        this.root = nodes.get(0);
    }

    private void decompress(byte[] data, String outputPath) {
        StringBuilder bitString = new StringBuilder();
        for (byte b : data) {
            //convert byte to 8-bit binary string, handling the padding zeros
            bitString.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }

        //traverse tree to decode the bits
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath))) {
            Node current = root;

            //loop up to validBits to ignore padding at the end
            for (int i = 0; i < validBits; i++) {
                char bit = bitString.charAt(i);

                if (bit == '0' && current.left != null) {
                    current = current.left;
                } else if (bit == '1' && current.right != null) {
                    current = current.right;
                }

                //if we reach a leaf node
                if (current != null && current.value != null) {
                    String decodedValue = current.value;

                    if (decodedValue.equals("\\n")) {
                        bw.newLine();
                    } else {
                        bw.write(decodedValue);
                    }
                    current = root; //reset to the root for the next character
                }
            }
            System.out.println("Successfully decompressed to " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
