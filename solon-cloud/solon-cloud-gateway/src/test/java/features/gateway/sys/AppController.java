package features.gateway.sys;

import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.UploadedFile;

/**
 * @author noear 2024/10/1 created
 */
@Controller
public class AppController {
    @Mapping("/hello")
    public String hello(){
        return "hello";
    }

    @Mapping("/file")
    public String file(UploadedFile file){
        return file.getName();
    }
}
