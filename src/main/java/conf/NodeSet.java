package conf;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.util.ArrayList;

@Getter
@Setter
public class NodeSet {
    ArrayList<Node> nodes = new ArrayList<>();

    static public NodeSet read_from_file(String conf_path)  {
        File file = new File(conf_path);
        long file_length=file.length();
        byte[] bytes=new byte[(int) file_length];
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            int read_len = fileInputStream.read(bytes);
            assert read_len==file_length;
            fileInputStream.close();
            String jsonString=new String(bytes);
            return JSON.parseObject(jsonString, NodeSet.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        NodeSet nodeSet=read_from_file("conf/nodes.json");
        assert nodeSet != null;
        System.out.println(nodeSet.nodes.get(0));

//        Node node= new Node();
//        node.setPort(22);
//        node.setIp(222);
//        NodeSet nodeSet= new NodeSet();
//        nodeSet.nodes.add(node);
//
//        System.out.println(JSON.toJSONString(nodeSet));
    }

}
