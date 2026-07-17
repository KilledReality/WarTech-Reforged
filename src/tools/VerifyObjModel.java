import java.io.FileInputStream;
import net.minecraftforge.client.model.obj.GroupObject;
import net.minecraftforge.client.model.obj.WavefrontObject;

public final class VerifyObjModel {
    public static void main(String[] args) throws Exception {
        FileInputStream input = new FileInputStream(args[0]);
        WavefrontObject model;
        try {
            model = new WavefrontObject("patriot_launcher.obj", input);
        } finally {
            input.close();
        }
        int faces = 0;
        for (GroupObject group : model.groupObjects) {
            faces += group.faces.size();
            System.out.println(group.name + " faces=" + group.faces.size());
        }
        System.out.println("groups=" + model.groupObjects.size() + " faces=" + faces);
    }
}
