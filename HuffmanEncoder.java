import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        HuffmanEncoder encoder = new HuffmanEncoder();
    }
}

class HuffmanEncoder{
    private HashMap<String, Integer> map;
    private ArrayList<String> mapKeys;
    private HashMap<String, String> huffmanCodes;
    Node root;

    public HuffmanEncoder() {
        readInputFile();
    }

    private void readInputFile() {
        String path = "src/greedy_graph.csv";
        ArrayList<String[]> lines = new ArrayList<>();

        //I don't think I've ever used a HashMap before but I am really vibing with it
        mapKeys = new ArrayList<>();
        map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                lines.add(values);

                //handle the characters within the split value arrays
                for (int j = 0; j < values.length; j++) {
                    String value = values[j];

                    //include the comma that was removed by the split
                    if (j < values.length - 1) {
                        if(map.containsKey(",")) {
                            map.put(",", map.get(",") + 1);//increment the instance of that charValue in the hash map
                        }
                        else{
                            mapKeys.add(",");
                            map.put(",", 1);
                        }
                    }

                    while (value.length() > 0) {
                        if (value.startsWith("inf")) {
                            if(map.containsKey("inf")) {
                                map.put("inf", map.get("inf") + 1);//increment the instance of that charValue in the hash map
                            }
                            else{
                                mapKeys.add("inf");
                                map.put("inf", 1);
                            }
                            value = value.substring(3);
                        } else {
                            if(map.containsKey(value.substring(0, 1))) {
                                map.put(value.substring(0, 1), map.get(value.substring(0, 1)) + 1);//increment the instance of that charValue in the hash map
                            }
                            else{
                                mapKeys.add(value.substring(0, 1));
                                map.put(value.substring(0, 1), 1);
                            }

                            value = value.substring(1);
                        }
                    }
                }
                //add a newline character for every line read
                if(map.containsKey("\\n")) {
                    map.put("\\n", map.get("\\n") + 1);//increment the instance of that charValue in the hash map
                }
                else{
                    mapKeys.add("\\n");
                    map.put("\\n", 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        sort();
        printMap();
        encode();
        for(int i = 0; i < mapKeys.size(); i++){
            System.out.println(mapKeys.get(i) + ": " + huffmanCodes.get(mapKeys.get(i)));
        }

        writeToFile(lines);
    }

    public void printMap(){
        for(int i = 0; i < mapKeys.size(); i++){
            System.out.println(mapKeys.get(i) + ": " + map.get(mapKeys.get(i)));
        }
    }

    public void sort() {//using gnome sort because of the constant and low input size
        int index = 0;
        int n = mapKeys.size();

        while (index < n) {
            if (index == 0 || map.get(mapKeys.get(index)) >= map.get(mapKeys.get(index-1))){
                index++;
            } else {
                //swap the keys
                String tempKey = mapKeys.get(index);
                mapKeys.set(index, mapKeys.get(index-1));
                mapKeys.set(index-1, tempKey);

                index--;//step backwards
            }
        }
    }

    public void encode(){//build the Huffman tree
        ArrayList<Node> nodes = new ArrayList<>();
        for(int i = 0; i < mapKeys.size(); i++){
            nodes.add(new Node(mapKeys.get(i), map.get(mapKeys.get(i))));
        }

        while(nodes.size()>1){
            // To be truly greedy, we must ensure we always grab the two smallest frequencies
            // I added a quick sort here to maintain your logic flow
            nodes.sort((a, b) -> a.frequency - b.frequency);

            Node left = nodes.get(0);//get first node from the list
            Node right = nodes.get(1);//get second node

            Node parent = new Node(null, left.frequency + right.frequency);
            parent.left = left;
            parent.right = right;

            nodes.remove(0);//remove the first two nodes and continue
            nodes.remove(0);
            nodes.add(parent);
        }

        root = nodes.get(0);
        huffmanCodes = new HashMap<>();
        generateCodes(root, "");
    }

    private void generateCodes(Node node, String code) {
        if (node == null) {
            return;
        }

        //if we're at a leaf node, store the code to get there
        if (node.value != null) {
            huffmanCodes.put(node.value, code);
        }

        generateCodes(node.left, code + "0");
        generateCodes(node.right, code + "1");
    }

    public void writeToFile(ArrayList<String[]> lines) {
        //to make it easy to decompress the file later, padding will be added to each code if it is not 8 characters

        try (FileOutputStream fos = new FileOutputStream("src/Compressed.bin")) {
            DataOutputStream dos = new DataOutputStream(fos);//Google says this should work
            StringBuilder fullBitString = new StringBuilder();

            //create a header so the decompression script can read the keys for tree reconstruction
            dos.writeInt(map.size());

            for(String key : map.keySet()){
                dos.writeUTF(key);
                dos.writeInt(map.get(key));
            }

            //build the binary string
            for (int i = 0; i < lines.size(); i++) {
                String[] values = lines.get(i);

                for (int j = 0; j < values.length; j++) {
                    String value = values[j];
                    while (value.length() > 0) {
                        if (value.startsWith("inf")) {
                            fullBitString.append(huffmanCodes.get("inf"));
                            value = value.substring(3);
                        } else {
                            fullBitString.append(huffmanCodes.get(value.substring(0, 1)));
                            value = value.substring(1);
                        }
                    }
                    if (j < values.length - 1) {
                        fullBitString.append(huffmanCodes.get(","));
                    }
                }
                fullBitString.append(huffmanCodes.get("\\n"));
            }

            // We need to tell the decompressor how many bits to actually read to ignore padding
            dos.writeInt(fullBitString.length());

            int currentBuffer = 0;
            int bitCount = 0;

            //convert string into actual bits and write bytes
            for (int i = 0; i < fullBitString.length(); i++) {
                char bit = fullBitString.charAt(i);

                currentBuffer <<= 1; //shift the current byte to the left
                if (bit == '1') {
                    currentBuffer |= 1; //bitwise OR to add the last bit
                }

                bitCount++;//move forward

                if (bitCount == 8) {
                    dos.writeByte(currentBuffer);
                    currentBuffer = 0;
                    bitCount = 0;
                }
            }

            //add padding to the front of the byte
            if (bitCount > 0) {
                currentBuffer <<= (8 - bitCount);
                dos.writeByte(currentBuffer);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Node{
    String value;
    int frequency;
    Node left;
    Node right;

    public Node(String value, int frequency){
        this.value = value;
        this.frequency = frequency;
    }
}
